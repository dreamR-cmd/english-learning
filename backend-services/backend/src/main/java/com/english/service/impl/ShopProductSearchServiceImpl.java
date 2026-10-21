package com.english.service.impl;

import com.english.entity.ShopProduct;
import com.english.mapper.ShopProductMapper;
import com.english.service.ShopProductSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ShopProductSearchServiceImpl implements ShopProductSearchService {
    private static final Logger log = LoggerFactory.getLogger(ShopProductSearchServiceImpl.class);
    private static final String SEARCH_CACHE_KEY_PREFIX = "shop:product:search:";

    private final ShopProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final boolean enabled;
    private final String elasticsearchUrl;
    private final String indexName;
    private final long cacheTtlSeconds;
    private final int maxResults;

    public ShopProductSearchServiceImpl(ShopProductMapper productMapper,
                                        StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        @Value("${shop.search.enabled:true}") boolean enabled,
                                        @Value("${shop.search.elasticsearch-url:http://localhost:9200}") String elasticsearchUrl,
                                        @Value("${shop.search.index-name:shop_products}") String indexName,
                                        @Value("${shop.search.cache-ttl-seconds:300}") long cacheTtlSeconds,
                                        @Value("${shop.search.max-results:50}") int maxResults) {
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
        this.enabled = enabled;
        this.elasticsearchUrl = trimTrailingSlash(elasticsearchUrl);
        this.indexName = indexName;
        this.cacheTtlSeconds = cacheTtlSeconds <= 0 ? 300 : cacheTtlSeconds;
        this.maxResults = maxResults <= 0 ? 50 : maxResults;
    }

    @PostConstruct
    public void initIndex() {
        if (!enabled) {
            return;
        }
        try {
            rebuildIndex();
        } catch (RuntimeException error) {
            log.warn("Shop product Elasticsearch index rebuild skipped: {}", safeMessage(error));
        }
    }

    //根据输入的关键词搜索商品 ID 列表，优先使用 Elasticsearch，如果失败则返回空列表
    @Override
    public List<Long> searchProductIds(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (!enabled || normalized.isBlank()) {
            return List.of();
        }

        String cacheKey = searchCacheKey(normalized);
        try {
            //尝试从 Redis 缓存中读取搜索结果，如果存在则直接返回缓存的商品 ID 列表
            List<Long> cachedIds = readCachedIds(cacheKey);
            if (cachedIds != null) {
                return cachedIds;
            }
        } catch (RuntimeException error) {
            log.warn("Shop product search cache ignored: {}", safeMessage(error));
        }

        try {
            List<Long> ids = searchEs(normalized);
            try {
                cacheIds(cacheKey, ids);
            } catch (RuntimeException error) {
                log.warn("Failed to cache shop product search result: {}", safeMessage(error));
            }
            return ids;
        } catch (RuntimeException error) {
            log.warn("Shop product Elasticsearch search failed, fallback to MySQL: {}", safeMessage(error));
            return List.of();
        }
    }

    @Override
    public void rebuildIndex() {
        if (!enabled) {
            return;
        }
        createIndexIfNeeded();
        for (ShopProduct product : productMapper.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            indexProduct(product);
        }
        log.info("Shop product Elasticsearch index rebuilt: index={}", indexName);
    }

    @Override
    public void indexProduct(ShopProduct product) {
        if (!enabled || product == null || product.getId() == null) {
            return;
        }
        try {
            restClient.put()
                    .uri(elasticsearchUrl + "/" + indexName + "/_doc/" + product.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toDocument(product))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException error) {
            log.warn("Failed to index shop product {} to Elasticsearch: {}", product.getId(), safeMessage(error));
        }
    }

    private void createIndexIfNeeded() {
        try {
            restClient.head()
                    .uri(elasticsearchUrl + "/" + indexName)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ignored) {
            restClient.put()
                    .uri(elasticsearchUrl + "/" + indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(indexDefinition())
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private Map<String, Object> indexDefinition() {
        return Map.of(
                "settings", Map.of(
                        "analysis", Map.of(
                                "normalizer", Map.of(
                                        "shop_keyword_normalizer", Map.of(
                                                "type", "custom",
                                                "filter", List.of("lowercase", "asciifolding")
                                        )
                                )
                        )
                ),
                "mappings", Map.of(
                        "properties", Map.of(
                                "id", Map.of("type", "long"),
                                "title", textWithKeyword(),
                                "category", textWithKeyword(),
                                "description", Map.of("type", "text"),
                                "tag", textWithKeyword(),
                                "points", Map.of("type", "text"),
                                "searchText", Map.of("type", "text"),
                                "active", Map.of("type", "boolean"),
                                "sortOrder", Map.of("type", "integer"),
                                "updatedAt", Map.of("type", "date")
                        )
                )
        );
    }

    private Map<String, Object> textWithKeyword() {
        return Map.of(
                "type", "text",
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256,
                                "normalizer", "shop_keyword_normalizer"
                        )
                )
        );
    }

    private Map<String, Object> toDocument(ShopProduct product) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", product.getId());
        document.put("title", product.getTitle());
        document.put("category", product.getCategory());
        document.put("description", product.getDescription());
        document.put("tag", product.getTag());
        document.put("points", product.getPoints());
        document.put("searchText", buildSearchText(product));
        document.put("active", Boolean.TRUE.equals(product.getActive()));
        document.put("sortOrder", product.getSortOrder());
        document.put("updatedAt", product.getUpdatedAt() == null ? LocalDateTime.now().toString() : product.getUpdatedAt().toString());
        return document;
    }

    private String buildSearchText(ShopProduct product) {
        return String.join(" ",
                safeText(product.getTitle()),
                safeText(product.getCategory()),
                safeText(product.getDescription()),
                safeText(product.getTag()),
                safeText(product.getPoints()).replace('|', ' ')
        ).trim();
    }

    private List<Long> searchEs(String keyword) {
        String response = restClient.post()
                .uri(elasticsearchUrl + "/" + indexName + "/_search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(searchRequest(keyword))
                .retrieve()
                .body(String.class);
        return parseIds(response);
    }

    private Map<String, Object> searchRequest(String keyword) {
        String wildcard = "*" + wildcardValue(keyword) + "*";
        return Map.of(
                "size", maxResults,
                "_source", List.of("id"),
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of("term", Map.of("active", true))),
                                "minimum_should_match", 1,
                                "should", List.of(
                                        Map.of("multi_match", Map.of(
                                                "query", keyword,
                                                "fields", List.of("title^5", "category^3", "tag^3", "description^2", "points^2", "searchText"),
                                                "operator", "or"
                                        )),
                                        Map.of("match_phrase_prefix", Map.of("title", Map.of("query", keyword, "boost", 5))),
                                        Map.of("match_phrase_prefix", Map.of("searchText", Map.of("query", keyword, "boost", 2))),
                                        Map.of("wildcard", Map.of("title.keyword", Map.of("value", wildcard, "case_insensitive", true, "boost", 4))),
                                        Map.of("wildcard", Map.of("category.keyword", Map.of("value", wildcard, "case_insensitive", true, "boost", 2))),
                                        Map.of("wildcard", Map.of("tag.keyword", Map.of("value", wildcard, "case_insensitive", true, "boost", 2)))
                                )
                        )
                )
        );
    }

    private List<Long> parseIds(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        try {
            JsonNode hits = objectMapper.readTree(response).path("hits").path("hits");
            if (!hits.isArray()) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>();
            for (JsonNode hit : hits) {
                JsonNode sourceId = hit.path("_source").path("id");
                if (sourceId.canConvertToLong()) {
                    ids.add(sourceId.asLong());
                    continue;
                }
                String rawId = hit.path("_id").asText("");
                if (!rawId.isBlank()) {
                    ids.add(Long.valueOf(rawId));
                }
            }
            return ids;
        } catch (JsonProcessingException | NumberFormatException error) {
            log.warn("Failed to parse shop product Elasticsearch response: {}", safeMessage(error));
            return List.of();
        }
    }

    private List<Long> readCachedIds(String cacheKey) {
        String value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.valueOf(part));
            }
        }
        return ids;
    }

    private void cacheIds(String cacheKey, List<Long> ids) {
        String value = ids == null || ids.isEmpty()
                ? ""
                : String.join(",", ids.stream().map(String::valueOf).toList());
        redisTemplate.opsForValue().set(cacheKey, value, cacheTtlSeconds, TimeUnit.SECONDS);
    }

    private String searchCacheKey(String keyword) {
        String hash = DigestUtils.md5DigestAsHex(keyword.getBytes(StandardCharsets.UTF_8));
        return SEARCH_CACHE_KEY_PREFIX + hash;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
    }

    private String wildcardValue(String keyword) {
        return keyword.toLowerCase()
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("?", "\\?");
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
