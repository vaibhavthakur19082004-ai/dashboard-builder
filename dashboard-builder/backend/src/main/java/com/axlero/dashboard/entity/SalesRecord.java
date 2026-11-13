package com.axlero.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Demo dataset that widgets aggregate over. */
@Entity
@Getter @Setter @NoArgsConstructor
public class SalesRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String region;
    private String product;
    private String category;
    private String channel;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    private Integer quantity;
    private LocalDate orderDate;
}
