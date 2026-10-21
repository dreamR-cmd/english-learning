package com.english.service.impl;

import com.english.dto.RagSearchItem;
import com.english.service.RagVectorRecord;
import com.english.service.RagVectorStore;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class RedisStackRagVectorStore implements RagVectorStore {
    private static final ByteArrayCodec BYTE_ARRAY_CODEC = ByteArrayCodec.INSTANCE;
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final StringRedisTemplate redisTemplate;
    private final String indexName;
    private final String keyPrefix;
    private final int dimension;

    public RedisStackRagVectorStore(
            StringRedisTemplate redisTemplate,
            @Value("${rag.vector.index-name:rag_chunks_idx}") String indexName,
            @Value("${rag.vector.key-prefix:rag:chunk:}") String keyPrefix,
            @Value("${rag.vector.dimension:1536}") int dimension) {
        this.redisTemplate = redisTemplate;
        this.indexName = indexName;
        this.keyPrefix = keyPrefix;
        this.dimension = dimension;
    }

    @Override
    public void ensureIndex() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try {
                ftInfo(connection);
            } catch (RuntimeException ignored) {
                createIndex(connection);
            }
            return null;
        });
    }

    @Override
    public void upsert(RagVectorRecord record) {
        validateDimension(record.embedding());
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            String key = keyPrefix + record.vectorId();
            Map<byte[], byte[]> values = new HashMap<>();
            values.put(bytes("documentId"), bytes(String.valueOf(record.documentId())));
            values.put(bytes("chunkId"), bytes(String.valueOf(record.chunkId())));
            values.put(bytes("title"), bytes(record.title()));
            values.put(bytes("source"), bytes(record.source() == null ? "" : record.source()));
            values.put(bytes("content"), bytes(record.content()));
            values.put(bytes("embedding"), toBytes(record.embedding()));
            connection.hashCommands().hMSet(bytes(key), values);
            return null;
        });
    }

    @Override
    public List<RagSearchItem> search(float[] queryVector, int topK) {
        validateDimension(queryVector);
        return redisTemplate.execute((RedisCallback<List<RagSearchItem>>) connection -> {
            List<Object> raw = dispatch(connection,
                    RediSearchCommand.FT_SEARCH,
                    new NestedMultiOutput<>(BYTE_ARRAY_CODEC),
                    args(
                            bytes(indexName),
                            bytes("*=>[KNN " + topK + " @embedding $vector AS score]"),
                            bytes("PARAMS"), bytes("2"), bytes("vector"), toBytes(queryVector),
                            bytes("RETURN"), bytes("5"),
                            bytes("documentId"), bytes("title"), bytes("source"), bytes("content"), bytes("score"),
                            bytes("SORTBY"), bytes("score"), bytes("ASC"),
                            bytes("DIALECT"), bytes("2")));
            return parseSearchResult(raw);
        });
    }

    @Override
    public void delete(String vectorId) {
        redisTemplate.delete(keyPrefix + vectorId);
    }

    private void createIndex(RedisConnection connection) {
        dispatch(connection,
                RediSearchCommand.FT_CREATE,
                new StatusOutput<>(BYTE_ARRAY_CODEC),
                args(
                bytes(indexName),
                bytes("ON"), bytes("HASH"),
                bytes("PREFIX"), bytes("1"), bytes(keyPrefix),
                bytes("SCHEMA"),
                bytes("documentId"), bytes("TAG"),
                bytes("chunkId"), bytes("TAG"),
                bytes("title"), bytes("TEXT"),
                bytes("source"), bytes("TEXT"),
                bytes("content"), bytes("TEXT"),
                bytes("embedding"), bytes("VECTOR"), bytes("HNSW"), bytes("6"),
                bytes("TYPE"), bytes("FLOAT32"),
                bytes("DIM"), bytes(String.valueOf(dimension)),
                bytes("DISTANCE_METRIC"), bytes("COSINE")));
    }

    private List<Object> ftInfo(RedisConnection connection) {
        return dispatch(connection,
                RediSearchCommand.FT_INFO,
                new NestedMultiOutput<>(BYTE_ARRAY_CODEC),
                args(bytes(indexName)));
    }

    @SuppressWarnings("unchecked")
    private <T> T dispatch(RedisConnection connection,
                           ProtocolKeyword command,
                           CommandOutput<byte[], byte[], T> output,
                           CommandArgs<byte[], byte[]> args) {
        Object nativeConnection = connection.getNativeConnection();
        if (nativeConnection instanceof StatefulRedisConnection<?, ?> statefulConnection) {
            return ((StatefulRedisConnection<byte[], byte[]>) statefulConnection)
                    .sync()
                    .dispatch(command, output, args);
        }
        if (nativeConnection instanceof StatefulRedisClusterConnection<?, ?> clusterConnection) {
            return ((StatefulRedisClusterConnection<byte[], byte[]>) clusterConnection)
                    .sync()
                    .dispatch(command, output, args);
        }
        if (nativeConnection instanceof RedisCommands<?, ?> redisCommands) {
            return ((RedisCommands<byte[], byte[]>) redisCommands).dispatch(command, output, args);
        }
        if (nativeConnection instanceof RedisAsyncCommands<?, ?> redisAsyncCommands) {
            RedisFuture<T> future = ((RedisAsyncCommands<byte[], byte[]>) redisAsyncCommands)
                    .dispatch(command, output, args);
            return await(future);
        }
        throw new IllegalStateException("Unsupported Redis native connection: " + nativeConnection.getClass().getName());
    }

    private <T> T await(RedisFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Redis command interrupted", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("Redis command failed", error.getCause());
        }
    }

    private CommandArgs<byte[], byte[]> args(byte[]... values) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(BYTE_ARRAY_CODEC);
        for (byte[] value : values) {
            args.add(value);
        }
        return args;
    }

    private List<RagSearchItem> parseSearchResult(Object raw) {
        List<?> result = asList(raw);
        if (result.size() < 2) {
            return List.of();
        }

        List<RagSearchItem> items = new ArrayList<>();
        for (int i = 1; i + 1 < result.size(); i += 2) {
            Map<String, String> fields = parseFields(result.get(i + 1));
            String documentId = fields.get("documentId");
            String title = fields.getOrDefault("title", "Untitled Document");
            String source = fields.get("source");
            String content = fields.getOrDefault("content", "");
            int score = toDisplayScore(fields.get("score"));
            items.add(new RagSearchItem(documentId, title, source, buildSnippet(content), score));
        }
        return items;
    }

    private Map<String, String> parseFields(Object rawFields) {
        List<?> values = asList(rawFields);
        Map<String, String> fields = new HashMap<>();
        for (int i = 0; i + 1 < values.size(); i += 2) {
            fields.put(asString(values.get(i)), asString(values.get(i + 1)));
        }
        return fields;
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Object[] array) {
            return Arrays.asList(array);
        }
        return List.of();
    }

    private String asString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[] bytes) {
            return repairMojibake(new String(bytes, StandardCharsets.UTF_8));
        }
        return repairMojibake(String.valueOf(value));
    }

    private int toDisplayScore(String rawDistance) {
        try {
            double distance = Double.parseDouble(rawDistance);
            return Math.max(0, (int) Math.round((1.0d - distance) * 1000));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private String buildSnippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private void validateDimension(float[] vector) {
        if (vector == null || vector.length != dimension) {
            throw new IllegalArgumentException("RAG 向量维度不匹配，期望 " + dimension + "，实际 "
                    + (vector == null ? 0 : vector.length));
        }
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private String repairMojibake(String value) {
        if (value == null || value.isBlank() || !looksLikeMojibake(value)) {
            return value;
        }
        String repairedFromLatin1 = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        String repairedFromWindows1252 = new String(value.getBytes(WINDOWS_1252), StandardCharsets.UTF_8);

        String best = value;
        if (textQuality(repairedFromLatin1) > textQuality(best)) {
            best = repairedFromLatin1;
        }
        if (textQuality(repairedFromWindows1252) > textQuality(best)) {
            best = repairedFromWindows1252;
        }
        return best;
    }

    private boolean looksLikeMojibake(String value) {
        return value.contains("Ã")
                || value.contains("Â")
                || value.contains("â")
                || value.contains("ä")
                || value.contains("å")
                || value.contains("æ")
                || value.contains("è")
                || value.contains("é")
                || value.contains("É")
                || value.contains("Ë")
                || value.contains("ï")
                || value.chars().anyMatch(ch -> ch >= 0x80 && ch <= 0x9F);
    }

    private int textQuality(String value) {
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                score += 3;
            } else if (ch >= 0x80 && ch <= 0x9F) {
                score -= 10;
            } else if ((ch >= 0x00C0 && ch <= 0x00FF) || ch == '\u0152' || ch == '\u017D' || ch == '\u201A'
                    || ch == '\u201E' || ch == '\u2026' || ch == '\u2030' || ch == '\u2039' || ch == '\u2122') {
                score -= 2;
            }
        }
        return score;
    }

    private enum RediSearchCommand implements ProtocolKeyword {
        FT_INFO("FT.INFO"),
        FT_CREATE("FT.CREATE"),
        FT_SEARCH("FT.SEARCH");

        private final byte[] bytes;

        RediSearchCommand(String command) {
            this.bytes = command.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }
    }
}
