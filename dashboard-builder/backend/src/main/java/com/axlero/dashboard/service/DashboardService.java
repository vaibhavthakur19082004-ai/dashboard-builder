package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.DashboardDtos.*;
import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.dto.MetricQuery;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.exception.InvalidQueryException;
import com.axlero.dashboard.exception.ResourceNotFoundException;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All lookups are scoped to the caller's workspace => tenant isolation (other tenants get 404). */
@Service
@Transactional
public class DashboardService {
    private final DashboardRepository dashboards;
    private final WidgetConfigRepository widgets;
    private final MetricService metricService;
    private final CachedMetricService cached;
    private final ObjectMapper mapper;

    public DashboardService(DashboardRepository dashboards, WidgetConfigRepository widgets,
                            MetricService metricService, CachedMetricService cached, ObjectMapper mapper) {
        this.dashboards = dashboards;
        this.widgets = widgets;
        this.metricService = metricService;
        this.cached = cached;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DashboardSummary> list(User u) {
        return dashboards.findByWorkspaceIdOrderByIdDesc(u.getWorkspace().getId()).stream()
                .map(d -> new DashboardSummary(d.getId(), d.getName())).toList();
    }

    @Transactional(readOnly = true)
    public DashboardDto get(Long id, User u) {
        Dashboard d = owned(id, u);
        return new DashboardDto(d.getId(), d.getName(), d.getWidgets().stream().map(this::toDto).toList());
    }

    public DashboardSummary create(String name, User u) {
        Dashboard d = new Dashboard();
        d.setName(name);
        d.setWorkspace(u.getWorkspace());
        dashboards.save(d);
        return new DashboardSummary(d.getId(), d.getName());
    }

    public void delete(Long id, User u) {
        dashboards.delete(owned(id, u));
    }

    public WidgetDto addWidget(Long dashboardId, WidgetRequest r, User u) {
        Dashboard d = owned(dashboardId, u);
        metricService.validate(r.query());
        WidgetConfig w = new WidgetConfig();
        w.setDashboard(d);
        w.setPosition(d.getWidgets().size());
        apply(w, r);
        d.getWidgets().add(w);
        return toDto(widgets.save(w));
    }

    public WidgetDto updateWidget(Long widgetId, WidgetRequest r, User u) {
        WidgetConfig w = ownedWidget(widgetId, u);
        metricService.validate(r.query());
        apply(w, r);
        return toDto(w);
    }

    public void deleteWidget(Long widgetId, User u) {
        WidgetConfig w = ownedWidget(widgetId, u);
        w.getDashboard().getWidgets().remove(w);
        widgets.delete(w);
    }

    public void reorder(Long dashboardId, List<Long> ids, User u) {
        Dashboard d = owned(dashboardId, u);
        Map<Long, WidgetConfig> byId = new HashMap<>();
        d.getWidgets().forEach(w -> byId.put(w.getId(), w));
        for (int i = 0; i < ids.size(); i++) {
            WidgetConfig w = byId.get(ids.get(i));
            if (w == null) throw new InvalidQueryException("Widget " + ids.get(i) + " is not part of this dashboard");
            w.setPosition(i);
        }
    }

    /** Cached: repeated calls for the same configuration never touch the database. */
    @Transactional(readOnly = true)
    public List<DataPoint> widgetData(Long widgetId, User u) {
        return cached.run(read(ownedWidget(widgetId, u).getQueryJson()));
    }

    private void apply(WidgetConfig w, WidgetRequest r) {
        w.setTitle(r.title());
        w.setChartType(r.chartType());
        try {
            w.setQueryJson(mapper.writeValueAsString(r.query()));
        } catch (JsonProcessingException e) {
            throw new InvalidQueryException("Query could not be serialised");
        }
    }

    private WidgetDto toDto(WidgetConfig w) {
        return new WidgetDto(w.getId(), w.getTitle(), w.getChartType(), read(w.getQueryJson()), w.getPosition());
    }

    private MetricQuery read(String json) {
        try {
            return mapper.readValue(json, MetricQuery.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored widget query is corrupt", e);
        }
    }

    private Dashboard owned(Long id, User u) {
        return dashboards.findByIdAndWorkspaceId(id, u.getWorkspace().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard not found"));
    }

    private WidgetConfig ownedWidget(Long id, User u) {
        return widgets.findByIdAndDashboardWorkspaceId(id, u.getWorkspace().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Widget not found"));
    }
}
