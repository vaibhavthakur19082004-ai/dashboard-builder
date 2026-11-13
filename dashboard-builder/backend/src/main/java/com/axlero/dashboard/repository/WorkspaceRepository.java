package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> { }
