package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> { }
