package com.axlero.dashboard.dto;

import com.axlero.dashboard.entity.ChartType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() { }

    public record DashboardRequest(@NotBlank @Size(max = 100) String name) { }

    public record WidgetRequest(
            @NotBlank @Size(max = 100) String title,
            @NotNull ChartType chartType,
            @NotNull @Valid MetricQuery query) { }

    public record ReorderRequest(@NotNull List<Long> widgetIds) { }

    public record WidgetDto(Long id, String title, ChartType chartType, MetricQuery query, int position) { }

    public record DashboardSummary(Long id, String name) { }

    public record DashboardDto(Long id, String name, List<WidgetDto> widgets) { }
}
