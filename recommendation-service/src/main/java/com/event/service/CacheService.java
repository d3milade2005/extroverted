package com.event.service;

import com.event.dto.EventRecommendationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${recommendation.cache.user-recommendations}")
    private int userRecommendationsTTL;

    @Value("${recommendation.cache.trending-events}")
    private int trendingEventsTTL;

    @Value("${recommendation.cache.similar-events}")
    private int similarEventsTTL;

    private static final String USER_RECOMMENDATIONS_PREFIX = "recommendations:user:";
    private static final String TRENDING_PREFIX = "recommendations:trending";
    private static final String SIMILAR_PREFIX = "recommendations:similar:";
    private static final String CATEGORY_PREFIX = "recommendations:category:";

    public void cacheUserRecommendations(
            UUID userId,
            int page,
            List<EventRecommendationResponse> recommendations
    ) {
        String key = getUserRecommendationsKey(userId, page);
        cache(key, recommendations, userRecommendationsTTL, TimeUnit.MINUTES);
    }

    public List<EventRecommendationResponse> getUserRecommendations(UUID userId, int page) {
        String key = getUserRecommendationsKey(userId, page);
        return getRecommendations(key);
    }

    public void cacheTrendingEvents(List<EventRecommendationResponse> recommendations) {
        cache(TRENDING_PREFIX, recommendations, trendingEventsTTL, TimeUnit.MINUTES);
    }


    public List<EventRecommendationResponse> getTrendingEvents() {
        return getRecommendations(TRENDING_PREFIX);
    }

    public void cacheSimilarEvents(UUID eventId, List<EventRecommendationResponse> recommendations) {
        String key = SIMILAR_PREFIX + eventId;
        cache(key, recommendations, similarEventsTTL, TimeUnit.MINUTES);
    }


    public List<EventRecommendationResponse> getSimilarEvents(UUID eventId) {
        String key = SIMILAR_PREFIX + eventId;
        return getRecommendations(key);
    }

    public void cacheCategoryRecommendations(
            UUID userId,
            String category,
            List<EventRecommendationResponse> recommendations
    ) {
        String key = CATEGORY_PREFIX + userId + ":" + category;
        cache(key, recommendations, userRecommendationsTTL, TimeUnit.MINUTES);
    }


    public List<EventRecommendationResponse> getCategoryRecommendations(UUID userId, String category) {
        String key = CATEGORY_PREFIX + userId + ":" + category;
        return getRecommendations(key);
    }


    public void invalidateUserCache(UUID userId) {
        String pattern = USER_RECOMMENDATIONS_PREFIX + userId + ":*";
        String categoryPattern = CATEGORY_PREFIX + userId + ":*";

        deleteByPattern(pattern);
        deleteByPattern(categoryPattern);

        log.info("Invalidated cache for user {}", userId);
    }


    public void invalidateTrendingCache() {
        redisTemplate.delete(TRENDING_PREFIX);
        log.info("Invalidated trending cache");
    }


    public void invalidateSimilarEventsCache(UUID eventId) {
        String key = SIMILAR_PREFIX + eventId;
        redisTemplate.delete(key);
        log.info("Invalidated similar events cache for event {}", eventId);
    }

    private void cache(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Cached data with key: {} (TTL: {} {})", key, timeout, unit);
        } catch (Exception e) {
            log.error("Error caching data with key {}: {}", key, e.getMessage());
        }
    }

    private List<EventRecommendationResponse> getRecommendations(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for key: {}", key);
                // Convert to proper type
                return objectMapper.convertValue(
                        cached,
                        new TypeReference<List<EventRecommendationResponse>>() {}
                );
            }
            log.debug("Cache miss for key: {}", key);
        } catch (Exception e) {
            log.error("Error retrieving from cache with key {}: {}", key, e.getMessage());
        }
        return null;
    }

    private void deleteByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error deleting keys with pattern {}: {}", pattern, e.getMessage());
        }
    }


    private String getUserRecommendationsKey(UUID userId, int page) {
        return USER_RECOMMENDATIONS_PREFIX + userId + ":page:" + page;
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking key existence: {}", e.getMessage());
            return false;
        }
    }

    public long getTTL(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL for key {}: {}", key, e.getMessage());
            return -1;
        }
    }
}
