package com.axlero.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class Dashboard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Workspace workspace;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WidgetConfig> widgets = new ArrayList<>();
}
