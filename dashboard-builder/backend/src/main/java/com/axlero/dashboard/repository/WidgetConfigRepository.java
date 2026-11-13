package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.WidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WidgetConfigRepository extends JpaRepository<WidgetConfig, Long> {
    /** Tenant-scoped lookup: a widget is only reachable through its dashboard's workspace. */
    Optional<WidgetConfig> findByIdAndDashboardWorkspaceId(Long id, Long workspaceId);
}
