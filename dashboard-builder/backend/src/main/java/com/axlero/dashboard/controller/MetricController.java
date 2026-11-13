package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.dto.MetricQuery;
import com.axlero.dashboard.service.CachedMetricService;
import com.axlero.dashboard.service.MetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {
    private final MetricService metrics;
    private final CachedMetricService cached;

    public MetricController(MetricService metrics, CachedMetricService cached) {
        this.metrics = metrics;
        this.cached = cached;
    }

    /** Tells the React builder which fields / aggregations are allowed. */
    @GetMapping("/schema")
    public Map<String, Object> schema() {
        return metrics.schema();
    }

    @PostMapping("/preview")
    public List<DataPoint> preview(@Valid @RequestBody MetricQuery q) {
        return cached.run(q);
    }

    @PostMapping("/cache/evict")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evict() {
        cached.evictAll();
    }
}
