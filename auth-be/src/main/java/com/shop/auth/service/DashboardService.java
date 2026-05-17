package com.shop.auth.service;

import com.shop.auth.dto.DashboardStatsDto;

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
