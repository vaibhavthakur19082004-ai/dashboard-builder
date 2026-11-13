package com.axlero.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class WidgetConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChartType chartType;

    /** JSON-serialised MetricQuery (validated before it is stored). */
    @Column(nullable = false, length = 4000)
    private String queryJson;

    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Dashboard dashboard;
}
