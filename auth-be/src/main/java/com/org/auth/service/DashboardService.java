package com.org.auth.service;

import com.org.auth.dto.DashboardStatsDto;

/**
 * Aggregates read-only statistics for the admin dashboard.
 */
public interface DashboardService {

    /**
     * Collects and returns a snapshot of all dashboard statistics.
     *
     * @return aggregate statistics DTO
     */
    DashboardStatsDto getStats();
}
