package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * User-supplied widget configuration. Every identifier in here is checked against a
 * whitelist in MetricService; values are always bound as parameters.
 */
public record MetricQuery(
        String dimension,          // GROUP BY field (null => single KPI value)
        @NotNull Aggregation aggregation,
        String measure,            // field to aggregate (not needed for COUNT)
        List<Filter> filters) {

    public enum Aggregation { SUM, COUNT, AVG, MIN, MAX }

    public record Filter(String field, String op, String value) { }
}
