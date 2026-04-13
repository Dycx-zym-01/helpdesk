package com.example.helpdesk.service;

import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.Ticket;
import com.example.helpdesk.domain.TicketProcessingRecord;
import com.example.helpdesk.domain.TicketStatus;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.StatisticsModels;
import com.example.helpdesk.repository.TicketProcessingRecordRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final TicketRepository ticketRepository;
    private final TicketProcessingRecordRepository ticketProcessingRecordRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public StatisticsService(TicketRepository ticketRepository,
                             TicketProcessingRecordRepository ticketProcessingRecordRepository,
                             UserRepository userRepository,
                             CurrentUserService currentUserService) {
        this.ticketRepository = ticketRepository;
        this.ticketProcessingRecordRepository = ticketProcessingRecordRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public StatisticsModels.OverviewResponse getOverview(Long currentUserId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(currentUser, Role.ADMIN);

        List<Ticket> tickets = ticketRepository.findAll().stream()
                .filter(item -> item.getStatus() != TicketStatus.DRAFT)
                .toList();
        Map<Long, List<TicketProcessingRecord>> processingRecordsByTicketId = loadProcessingRecords(tickets);
        long total = tickets.size();
        long resolved = tickets.stream()
                .filter(item -> item.getStatus().isResolvedLike())
                .count();
        long unresolved = total - resolved;
        double resolvedRate = total == 0 ? 0D : resolved * 100.0 / total;
        double averageHours = tickets.stream()
                .filter(item -> item.getResolvedAt() != null)
                .mapToDouble(item -> Duration.between(item.getCreatedAt(), item.getResolvedAt()).toMinutes() / 60.0)
                .average()
                .orElse(0D);

        List<StatisticsModels.NamedValue> categoryDistribution = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCategory, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new StatisticsModels.NamedValue(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(StatisticsModels.NamedValue::value).reversed())
                .toList();

        List<StatisticsModels.NamedValue> statusDistribution = tickets.stream()
                .collect(Collectors.groupingBy(item -> displayStatusOf(item).getLabel(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new StatisticsModels.NamedValue(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(StatisticsModels.NamedValue::value).reversed())
                .toList();

        List<User> developers = userRepository.findByRole(Role.DEVELOPER);
        LocalDateTime now = LocalDateTime.now();
        List<StatisticsModels.DeveloperMetric> developerMetrics = developers.stream()
                .map(user -> buildDeveloperMetric(user, tickets, processingRecordsByTicketId, now))
                .sorted(Comparator.comparingDouble(StatisticsModels.DeveloperMetric::handledHours)
                        .reversed()
                        .thenComparing(Comparator.comparingLong(StatisticsModels.DeveloperMetric::resolvedCount).reversed()))
                .toList();

        LocalDate start = LocalDate.now().minusDays(13);
        List<StatisticsModels.TrendPoint> trendPoints = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = start.plusDays(i);
            long createdCount = tickets.stream()
                    .filter(item -> item.getCreatedAt().toLocalDate().equals(day))
                    .count();
            long resolvedCount = tickets.stream()
                    .filter(item -> item.getResolvedAt() != null && item.getResolvedAt().toLocalDate().equals(day))
                    .count();
            trendPoints.add(new StatisticsModels.TrendPoint(day.toString(), createdCount, resolvedCount));
        }

        return new StatisticsModels.OverviewResponse(
                total,
                resolved,
                unresolved,
                round(resolvedRate),
                round(averageHours),
                categoryDistribution,
                statusDistribution,
                developerMetrics,
                trendPoints
        );
    }

    private StatisticsModels.DeveloperMetric buildDeveloperMetric(User user,
                                                                  List<Ticket> tickets,
                                                                  Map<Long, List<TicketProcessingRecord>> processingRecordsByTicketId,
                                                                  LocalDateTime now) {
        long resolvedCount = tickets.stream()
                .filter(item -> handledAsResolver(user.getId(), item, processingRecordsByTicketId.get(item.getId())))
                .count();
        long processingCount = tickets.stream()
                .filter(item -> displayStatusOf(item) == TicketStatus.PROCESSING)
                .filter(item -> item.getAssignee() != null && Objects.equals(item.getAssignee().getId(), user.getId()))
                .count();

        Map<Long, Double> handledHoursByTicket = new HashMap<>();
        tickets.forEach(item -> {
            double handledHours = calculateHandledHours(user.getId(), item, processingRecordsByTicketId.get(item.getId()), now);
            if (handledHours > 0D) {
                handledHoursByTicket.put(item.getId(), handledHours);
            }
        });

        double averageHours = handledHoursByTicket.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0D);
        double handledHours = handledHoursByTicket.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        return new StatisticsModels.DeveloperMetric(
                user.getId(),
                user.getName(),
                resolvedCount,
                processingCount,
                round(handledHours),
                round(averageHours)
        );
    }

    private Map<Long, List<TicketProcessingRecord>> loadProcessingRecords(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Map.of();
        }
        return ticketProcessingRecordRepository.findByTicketIdIn(
                        tickets.stream().map(Ticket::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(item -> item.getTicket().getId()));
    }

    private boolean handledAsResolver(Long userId, Ticket ticket, List<TicketProcessingRecord> processingRecords) {
        if (processingRecords != null && !processingRecords.isEmpty()) {
            return processingRecords.stream()
                    .anyMatch(item -> item.isResolved() && Objects.equals(item.getAssignee().getId(), userId));
        }
        return ticket.getAssignee() != null
                && Objects.equals(ticket.getAssignee().getId(), userId)
                && ticket.getStatus().isResolvedLike();
    }

    private double calculateHandledHours(Long userId,
                                         Ticket ticket,
                                         List<TicketProcessingRecord> processingRecords,
                                         LocalDateTime now) {
        if (processingRecords != null && !processingRecords.isEmpty()) {
            return processingRecords.stream()
                    .filter(item -> Objects.equals(item.getAssignee().getId(), userId))
                    .mapToDouble(item -> calculateHours(
                            item.getStartedAt(),
                            item.getEndedAt() == null ? now : item.getEndedAt()
                    ))
                    .sum();
        }

        if (ticket.getAssignee() != null && Objects.equals(ticket.getAssignee().getId(), userId)) {
            LocalDateTime endTime = ticket.getResolvedAt() == null ? now : ticket.getResolvedAt();
            return calculateHours(ticket.getCreatedAt(), endTime);
        }
        return 0D;
    }

    private double calculateHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0D;
        }
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private TicketStatus displayStatusOf(Ticket ticket) {
        return ticket.getStatus().toDisplayStatus(ticket.getAssignee() != null);
    }
}
