package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.dto.MetricQuery;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caching facade (separate bean so Spring's proxy-based @Cacheable is not bypassed by
 * self-invocation). The MetricQuery record is the cache key, so editing a widget's
 * configuration automatically produces a fresh cache entry.
 */
@Service
public class CachedMetricService {
    private final MetricService metrics;

    public CachedMetricService(MetricService metrics) {
        this.metrics = metrics;
    }

    @Cacheable(cacheNames = "metrics", key = "#query")
    public List<DataPoint> run(MetricQuery query) {
        return metrics.execute(query);
    }

    @CacheEvict(cacheNames = "metrics", allEntries = true)
    public void evictAll() { }
}
