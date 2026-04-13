package com.example.helpdesk.controller;

import com.example.helpdesk.dto.StatisticsModels;
import com.example.helpdesk.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public StatisticsModels.OverviewResponse overview(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId) {
        return statisticsService.getOverview(userId);
    }
}
