package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.DashboardDtos.DashboardDto;
import com.axlero.dashboard.dto.DashboardDtos.WidgetDto;
import com.axlero.dashboard.dto.DataPoint;
import com.axlero.dashboard.entity.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportService {
    private final DashboardService dashboards;
    private final CachedMetricService metrics;

    public ExportService(DashboardService dashboards, CachedMetricService metrics) {
        this.dashboards = dashboards;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public byte[] csv(Long dashboardId, User u) {
        DashboardDto d = dashboards.get(dashboardId, u);
        StringBuilder sb = new StringBuilder("Widget,Label,Value\r\n");
        for (WidgetDto w : d.widgets()) {
            for (DataPoint p : metrics.run(w.query())) {
                sb.append(cell(w.title())).append(',').append(cell(p.label())).append(',')
                  .append(p.value().toPlainString()).append("\r\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] xlsx(Long dashboardId, User u) {
        DashboardDto d = dashboards.get(dashboardId, u);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int n = 1;
            for (WidgetDto w : d.widgets()) {
                Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName((n++) + " " + w.title()));
                Row head = sheet.createRow(0);
                head.createCell(0).setCellValue("Label");
                head.createCell(1).setCellValue("Value");
                List<DataPoint> points = metrics.run(w.query());
                for (int i = 0; i < points.size(); i++) {
                    Row r = sheet.createRow(i + 1);
                    r.createCell(0).setCellValue(points.get(i).label());
                    r.createCell(1).setCellValue(points.get(i).value().doubleValue());
                }
                sheet.autoSizeColumn(0);
            }
            if (d.widgets().isEmpty()) wb.createSheet("Empty").createRow(0).createCell(0).setCellValue("No widgets");
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Escapes CSV and neutralises spreadsheet formula injection (=, +, -, @). */
    private String cell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
