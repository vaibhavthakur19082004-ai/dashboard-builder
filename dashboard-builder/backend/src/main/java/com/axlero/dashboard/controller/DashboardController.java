package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.DashboardDtos.*;
import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.exception.InvalidQueryException;
import com.axlero.dashboard.service.DashboardService;
import com.axlero.dashboard.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final String WRITE = "hasAnyRole('ADMIN','EDITOR')";
    private final DashboardService service;
    private final ExportService export;

    public DashboardController(DashboardService service, ExportService export) {
        this.service = service;
        this.export = export;
    }

    @GetMapping("/dashboards")
    public List<DashboardSummary> list(@AuthenticationPrincipal User u) {
        return service.list(u);
    }

    @GetMapping("/dashboards/{id}")
    public DashboardDto get(@PathVariable Long id, @AuthenticationPrincipal User u) {
        return service.get(id, u);
    }

    @PostMapping("/dashboards")
    @PreAuthorize(WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardSummary create(@Valid @RequestBody DashboardRequest r, @AuthenticationPrincipal User u) {
        return service.create(r.name(), u);
    }

    @DeleteMapping("/dashboards/{id}")
    @PreAuthorize(WRITE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        service.delete(id, u);
    }

    @PostMapping("/dashboards/{id}/widgets")
    @PreAuthorize(WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public WidgetDto addWidget(@PathVariable Long id, @Valid @RequestBody WidgetRequest r, @AuthenticationPrincipal User u) {
        return service.addWidget(id, r, u);
    }

    @PutMapping("/dashboards/{id}/widgets/order")
    @PreAuthorize(WRITE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable Long id, @Valid @RequestBody ReorderRequest r, @AuthenticationPrincipal User u) {
        service.reorder(id, r.widgetIds(), u);
    }

    @PutMapping("/widgets/{id}")
    @PreAuthorize(WRITE)
    public WidgetDto updateWidget(@PathVariable Long id, @Valid @RequestBody WidgetRequest r, @AuthenticationPrincipal User u) {
        return service.updateWidget(id, r, u);
    }

    @DeleteMapping("/widgets/{id}")
    @PreAuthorize(WRITE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWidget(@PathVariable Long id, @AuthenticationPrincipal User u) {
        service.deleteWidget(id, u);
    }

    @GetMapping("/widgets/{id}/data")
    public List<DataPoint> widgetData(@PathVariable Long id, @AuthenticationPrincipal User u) {
        return service.widgetData(id, u);
    }

    @GetMapping("/dashboards/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @RequestParam(defaultValue = "csv") String format,
                                         @AuthenticationPrincipal User u) {
        byte[] body;
        MediaType type;
        String ext;
        switch (format.toLowerCase()) {
            case "csv" -> { body = export.csv(id, u); type = MediaType.parseMediaType("text/csv"); ext = "csv"; }
            case "xlsx" -> {
                body = export.xlsx(id, u);
                type = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                ext = "xlsx";
            }
            default -> throw new InvalidQueryException("format must be csv or xlsx");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dashboard-" + id + "." + ext + "\"")
                .contentType(type).body(body);
    }
}
