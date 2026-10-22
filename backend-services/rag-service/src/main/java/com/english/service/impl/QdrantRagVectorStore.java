package com.english.service.impl;

import com.english.dto.RagSearchItem;
import com.english.service.RagVectorRecord;
import com.english.service.RagVectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "rag.vector.store", havingValue = "qdrant", matchIfMissing = false)
public class QdrantRagVectorStore implements RagVectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantRagVectorStore.class);

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String baseUrl;
    private final String collectionName;
    private final int dimension;
    private final int hnswM;
    private final int hnswEfConstruction;
    private final int hnswEfSearch;
    private final boolean normalizeVectors;

    public QdrantRagVectorStore(
            @Value("${rag.vector.qdrant.host:localhost}") String host,
            @Value("${rag.vector.qdrant.http-port:6333}") int httpPort,
            @Value("${rag.vector.qdrant.collection-name:rag_chunks}") String collectionName,
            @Value("${rag.vector.dimension:1536}") int dimension,
            @Value("${rag.vector.qdrant.hnsw.m:16}") int hnswM,
            @Value("${rag.vector.qdrant.hnsw.ef-construction:256}") int hnswEfConstruction,
            @Value("${rag.vector.qdrant.hnsw.ef-search:128}") int hnswEfSearch,
            @Value("${rag.vector.qdrant.normalize-vectors:false}") boolean normalizeVectors) {
        this.baseUrl = "http://" + host + ":" + httpPort;
        this.collectionName = collectionName;
        this.dimension = dimension;
        this.hnswM = hnswM;
        this.hnswEfConstruction = hnswEfConstruction;
        this.hnswEfSearch = hnswEfSearch;
        this.normalizeVectors = normalizeVectors;
        log.info("[Qdrant] connected via REST: {}", baseUrl);
    }

    @Override
    public void ensureIndex() {
        try {
            String url = baseUrl + "/collections/" + collectionName;
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("[Qdrant] collection {} already exists, skip creation", collectionName);
                return;
            }
        } catch (Exception ignored) {}

        try {
            Map<String, Object> vectorsConfig = new HashMap<>();
            vectorsConfig.put("size", dimension);
            vectorsConfig.put("distance", "Cosine");

            Map<String, Object> params = new HashMap<>();
            Map<String, Object> hnsw = new HashMap<>();
            hnsw.put("m", hnswM);
            hnsw.put("ef_construction", hnswEfConstruction);
            hnsw.put("on_disk", true);
            params.put("hnsw_config", hnsw);

            Map<String, Object> body = new HashMap<>();
            body.put("vectors", vectorsConfig);
            body.put("params", params);

            post("/collections/" + collectionName, body);
            log.info("[Qdrant] collection {} created (dim={}, HNSW M={})",
                    collectionName, dimension, hnswM);
        } catch (Exception e) {
            log.error("[Qdrant] ensureIndex failed", e);
        }
    }

    @Override
    public void upsert(RagVectorRecord record) {
        try {
            float[] vec = normalizeVectors ? normalize(record.embedding()) : record.embedding();

            Map<String, Object> point = new HashMap<>();
            point.put("id", record.chunkId());
            point.put("vector", floatArrayToList(vec));

            Map<String, Object> payload = new HashMap<>();
            payload.put("document_id", record.documentId());
            payload.put("title", nullSafe(record.title()));
            payload.put("source", nullSafe(record.source()));
            payload.put("content", nullSafe(record.content()));
            point.put("payload", payload);

            Map<String, Object> body = new HashMap<>();
            body.put("collection_name", collectionName);
            body.put("points", List.of(point));

            post("/collections/" + collectionName + "/points", body);
            log.debug("[Qdrant] upsert success: chunkId={}", record.chunkId());
        } catch (Exception e) {
            log.error("[Qdrant] upsert failed: chunkId={}", record.chunkId(), e);
        }
    }

    @Override
    public List<RagSearchItem> search(float[] queryVector, int topK) {
        try {
            float[] vec = normalizeVectors ? normalize(queryVector) : queryVector;

            Map<String, Object> hnsw = new HashMap<>();
            hnsw.put("ef", hnswEfSearch);

            Map<String, Object> params = new HashMap<>();
            params.put("hnsw", hnsw);

            Map<String, Object> body = new HashMap<>();
            body.put("collection_name", collectionName);
            body.put("vector", floatArrayToList(vec));
            body.put("limit", topK);
            body.put("params", params);
            body.put("with_payload", true);

            JsonNode root = postForJson("/collections/" + collectionName + "/points/search", body);
            List<RagSearchItem> items = new ArrayList<>();
            JsonNode results = root.get("result");

            if (results != null && results.isArray()) {
                for (JsonNode hit : results) {
                    float similarity = (float) hit.get("score").asDouble();
                    int score = (int) Math.round(similarity * 1000);

                    JsonNode p = hit.get("payload");
                    RagSearchItem item = new RagSearchItem();
                    item.setDocumentId(String.valueOf(p.get("document_id").asLong()));
                    item.setTitle(p.has("title") ? p.get("title").asText() : "");
                    item.setSource(p.has("source") ? p.get("source").asText() : "");
                    item.setSnippet(p.has("content") ? p.get("content").asText() : "");
                    item.setScore(Math.max(0, Math.min(1000, score)));
                    items.add(item);
                }
            }
            return items;
        } catch (Exception e) {
            log.error("[Qdrant] search failed", e);
            return List.of();
        }
    }

    @Override
    public void delete(String vectorId) {
        try {
            Long chunkId = parseChunkId(vectorId);
            if (chunkId == null) {
                log.warn("[Qdrant] cannot parse chunkId from vectorId: {}", vectorId);
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("collection_name", collectionName);
            body.put("points", List.of(chunkId));
            post("/collections/" + collectionName + "/points/delete", body);
            log.debug("[Qdrant] delete success: chunkId={}", chunkId);
        } catch (Exception e) {
            log.error("[Qdrant] delete failed: vectorId={}", vectorId, e);
        }
    }

    private ResponseEntity<String> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rest.postForEntity(baseUrl + path, new HttpEntity<>(json, headers), String.class);
    }

    private JsonNode postForJson(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(
                baseUrl + path, new HttpEntity<>(json, headers), String.class);
        return mapper.readTree(resp.getBody());
    }

    private Long parseChunkId(String vectorId) {
        if (vectorId == null) return null;
        try {
            return Long.parseLong(vectorId);
        } catch (NumberFormatException ignored) {}
        String[] parts = vectorId.split("-chunk-");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private float[] normalize(float[] vec) {
        float norm = 0f;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm == 0f) return vec;
        float[] normalized = new float[vec.length];
        for (int i = 0; i < vec.length; i++) normalized[i] = vec[i] / norm;
        return normalized;
    }

    private List<Double> floatArrayToList(float[] vec) {
        List<Double> list = new ArrayList<>(vec.length);
        for (float v : vec) list.add((double) v);
        return list;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
