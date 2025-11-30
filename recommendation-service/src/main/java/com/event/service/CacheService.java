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

    /**
     * Cache user recommendations
     *
     * @param userId User ID
     * @param page Page number
     * @param recommendations List of recommendations
     */
    public void cacheUserRecommendations(
            UUID userId,
            int page,
            List<EventRecommendationResponse> recommendations
    ) {
        String key = getUserRecommendationsKey(userId, page);
        cache(key, recommendations, userRecommendationsTTL, TimeUnit.MINUTES);
    }

    /**
     * Get cached user recommendations
     *
     * @param userId User ID
     * @param page Page number
     * @return Cached recommendations or null if not found
     */
    public List<EventRecommendationResponse> getUserRecommendations(UUID userId, int page) {
        String key = getUserRecommendationsKey(userId, page);
        return getRecommendations(key);
    }

    /**
     * Cache trending events
     *
     * @param recommendations List of trending events
     */
    public void cacheTrendingEvents(List<EventRecommendationResponse> recommendations) {
        cache(TRENDING_PREFIX, recommendations, trendingEventsTTL, TimeUnit.MINUTES);
    }

    /**
     * Get cached trending events
     *
     * @return Cached trending events or null if not found
     */
    public List<EventRecommendationResponse> getTrendingEvents() {
        return getRecommendations(TRENDING_PREFIX);
    }

    /**
     * Cache similar events
     *
     * @param eventId Event ID
     * @param recommendations List of similar events
     */
    public void cacheSimilarEvents(UUID eventId, List<EventRecommendationResponse> recommendations) {
        String key = SIMILAR_PREFIX + eventId;
        cache(key, recommendations, similarEventsTTL, TimeUnit.MINUTES);
    }

    /**
     * Get cached similar events
     *
     * @param eventId Event ID
     * @return Cached similar events or null if not found
     */
    public List<EventRecommendationResponse> getSimilarEvents(UUID eventId) {
        String key = SIMILAR_PREFIX + eventId;
        return getRecommendations(key);
    }

    /**
     * Cache category recommendations
     *
     * @param userId User ID
     * @param category Category name
     * @param recommendations List of recommendations
     */
    public void cacheCategoryRecommendations(
            UUID userId,
            String category,
            List<EventRecommendationResponse> recommendations
    ) {
        String key = CATEGORY_PREFIX + userId + ":" + category;
        cache(key, recommendations, userRecommendationsTTL, TimeUnit.MINUTES);
    }

    /**
     * Get cached category recommendations
     *
     * @param userId User ID
     * @param category Category name
     * @return Cached recommendations or null if not found
     */
    public List<EventRecommendationResponse> getCategoryRecommendations(UUID userId, String category) {
        String key = CATEGORY_PREFIX + userId + ":" + category;
        return getRecommendations(key);
    }

    /**
     * Invalidate all caches for a user
     * Called when user updates preferences or interactions
     *
     * @param userId User ID
     */
    public void invalidateUserCache(UUID userId) {
        String pattern = USER_RECOMMENDATIONS_PREFIX + userId + ":*";
        String categoryPattern = CATEGORY_PREFIX + userId + ":*";

        deleteByPattern(pattern);
        deleteByPattern(categoryPattern);

        log.info("Invalidated cache for user {}", userId);
    }

    /**
     * Invalidate trending cache
     * Called when new event is created or event gets popular
     */
    public void invalidateTrendingCache() {
        redisTemplate.delete(TRENDING_PREFIX);
        log.info("Invalidated trending cache");
    }

    /**
     * Invalidate similar events cache
     *
     * @param eventId Event ID
     */
    public void invalidateSimilarEventsCache(UUID eventId) {
        String key = SIMILAR_PREFIX + eventId;
        redisTemplate.delete(key);
        log.info("Invalidated similar events cache for event {}", eventId);
    }

    /**
     * Generic cache method
     */
    private void cache(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Cached data with key: {} (TTL: {} {})", key, timeout, unit);
        } catch (Exception e) {
            log.error("Error caching data with key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get recommendations from cache
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Delete keys matching pattern
     */
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

    /**
     * Build user recommendations cache key
     */
    private String getUserRecommendationsKey(UUID userId, int page) {
        return USER_RECOMMENDATIONS_PREFIX + userId + ":page:" + page;
    }

    /**
     * Check if cache exists for a key
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking key existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining TTL for a key
     */
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
