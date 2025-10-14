package com.globalsearch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager
     *
     * Cache Names:
     * - searchResults: Cache search results for 5 minutes
     * - users: Cache user data for 10 minutes
     * - tenants: Cache tenant info for 15 minutes
     * - reports: Cache report data for 10 minutes
     * - dashboards: Cache dashboard data for 10 minutes
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "searchResults",
                "users",
                "tenants",
                "reports",
                "dashboards",
                "systemStats"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());

        return cacheManager;
    }

    /**
     * Cache configuration for search results
     * Short TTL since search results can change frequently
     */
    @Bean
    public Caffeine<Object, Object> searchResultsCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }

    /**
     * Cache configuration for user data
     * Medium TTL since user data changes less frequently
     */
    @Bean
    public Caffeine<Object, Object> usersCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats();
    }

    /**
     * Cache configuration for tenant information
     * Longer TTL since tenant data is relatively static
     */
    @Bean
    public Caffeine<Object, Object> tenantsCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats();
    }

    /**
     * Cache configuration for system statistics
     * Very short TTL for near real-time stats
     */
    @Bean
    public Caffeine<Object, Object> systemStatsCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(50)
                .recordStats();
    }
}
