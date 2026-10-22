package com.english.config;

import com.english.service.RagVectorRecord;
import com.english.service.RagVectorStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Profile("migrate")
public class RedisToQdrantMigrator {

    private static final Logger log = LoggerFactory.getLogger(RedisToQdrantMigrator.class);

    private final RagVectorStore qdrantStore;
    private final StringRedisTemplate redisTemplate;

    @Value("${rag.vector.key-prefix:rag:chunk:}")
    private String redisKeyPrefix;

    public RedisToQdrantMigrator(RagVectorStore qdrantStore, StringRedisTemplate redisTemplate) {
        this.qdrantStore = qdrantStore;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void migrate() {
        log.info("===== Redis -> Qdrant migration start =====");
        try {
            qdrantStore.ensureIndex();

            List<String> redisKeys = scanAllRedisKeys();
            log.info("scanned {} Redis vector keys (prefix={})", redisKeys.size(), redisKeyPrefix);
            int successCount = 0;

            for (String redisKey : redisKeys) {
                try {
                    Map<byte[], byte[]> hash = redisTemplate.execute(
                            (RedisCallback<Map<byte[], byte[]>>) conn -> conn.hGetAll(redisKey.getBytes())
                    );
                    if (hash == null || hash.isEmpty()) continue;

                    float[] embedding = bytesToFloatArray(
                            Objects.requireNonNull(hash.get("embedding".getBytes()))
                    );
                    Long docId = Long.parseLong(new String(hash.get("documentId".getBytes())));
                    Long chunkId = Long.parseLong(new String(hash.get("chunkId".getBytes())));
                    String title = new String(hash.getOrDefault("title".getBytes(), new byte[0]));
                    String source = new String(hash.getOrDefault("source".getBytes(), new byte[0]));
                    String content = new String(hash.getOrDefault("content".getBytes(), new byte[0]));

                    qdrantStore.upsert(new RagVectorRecord(
                            redisKey, docId, chunkId, title, source, content, embedding
                    ));
                    successCount++;
                    if (successCount % 100 == 0) {
                        log.info("migrated {}/{}", successCount, redisKeys.size());
                    }
                } catch (Exception e) {
                    log.error("migrate failed: redisKey={}", redisKey, e);
                }
            }

            log.info("===== migration done: success {}/{} =====", successCount, redisKeys.size());
        } catch (Exception e) {
            log.error("migration fatal error", e);
        }
    }

    private List<String> scanAllRedisKeys() {
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.scan(ScanOptions.scanOptions()
                            .match(redisKeyPrefix + "*")
                            .count(200)
                            .build())
                    .forEachRemaining(k -> keys.add(new String(k)));
            return null;
        });
        return keys;
    }

    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vec = new float[bytes.length / 4];
        for (int i = 0; i < vec.length; i++) vec[i] = bb.getFloat();
        return vec;
    }
}
