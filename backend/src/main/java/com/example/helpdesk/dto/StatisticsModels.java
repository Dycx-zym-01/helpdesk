package com.example.helpdesk.dto;

import java.util.List;

public final class StatisticsModels {

    private StatisticsModels() {
    }

    public record NamedValue(
            String name,
            long value
    ) {
    }

    public record DeveloperMetric(
            Long userId,
            String userName,
            long resolvedCount,
            long processingCount,
            double handledHours,
            double averageHours
    ) {
    }

    public record TrendPoint(
            String label,
            long createdCount,
            long resolvedCount
    ) {
    }

    public record OverviewResponse(
            long totalTickets,
            long resolvedTickets,
            long unresolvedTickets,
            double resolvedRate,
            double averageResolutionHours,
            List<NamedValue> categoryDistribution,
            List<NamedValue> statusDistribution,
            List<DeveloperMetric> developerMetrics,
            List<TrendPoint> trendPoints
    ) {
    }
}
