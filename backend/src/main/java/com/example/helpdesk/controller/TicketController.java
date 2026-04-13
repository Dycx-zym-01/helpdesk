package com.example.helpdesk.controller;

import com.example.helpdesk.domain.Priority;
import com.example.helpdesk.domain.TicketStatus;
import com.example.helpdesk.dto.TicketModels;
import com.example.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public TicketModels.PageResult<TicketModels.TicketSummaryResponse> listTickets(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ticketService.listTickets(userId, status, category, priority, keyword, assigneeId, mine, startDate, endDate, page, size);
    }

    @GetMapping("/{id}")
    public TicketModels.TicketDetailResponse getTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id) {
        return ticketService.getTicketDetail(userId, id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketModels.TicketDetailResponse createTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @Valid @ModelAttribute TicketModels.CreateTicketRequest request) {
        return ticketService.createTicket(userId, request);
    }

    @PostMapping(value = "/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketModels.TicketDetailResponse saveDraft(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @ModelAttribute TicketModels.CreateTicketRequest request) {
        return ticketService.saveDraft(userId, request);
    }

    @PostMapping(value = "/{id}/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketModels.TicketDetailResponse updateDraft(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @ModelAttribute TicketModels.CreateTicketRequest request) {
        return ticketService.updateDraft(userId, id, request);
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketModels.TicketDetailResponse submitDraft(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @Valid @ModelAttribute TicketModels.CreateTicketRequest request) {
        return ticketService.submitDraft(userId, id, request);
    }

    @PostMapping("/{id}/assign")
    public TicketModels.TicketDetailResponse assignTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TicketModels.AssignTicketRequest request) {
        return ticketService.assignTicket(userId, id, request.assigneeId());
    }

    @PostMapping("/{id}/claim")
    public TicketModels.TicketDetailResponse claimTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id) {
        return ticketService.claimTicket(userId, id);
    }

    @PostMapping("/{id}/start")
    public TicketModels.TicketDetailResponse startTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id) {
        return ticketService.startProcessing(userId, id);
    }

    @PostMapping(value = "/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketModels.CommentActionResponse addComment(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @ModelAttribute TicketModels.CommentRequest request) {
        return ticketService.addComment(userId, id, request);
    }

    @PostMapping("/{id}/resolve")
    public TicketModels.TicketDetailResponse resolveTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody(required = false) TicketModels.ResolveTicketRequest request) {
        return ticketService.resolveTicket(userId, id, request == null ? null : request.comment());
    }

    @PostMapping("/{id}/close")
    public TicketModels.TicketDetailResponse closeTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id) {
        return ticketService.closeTicket(userId, id);
    }

    @DeleteMapping("/{id}")
    public void deleteTicket(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id) {
        ticketService.deleteDraft(userId, id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTickets(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ticketService.exportTickets(userId, status, category, priority, keyword, assigneeId, mine, startDate, endDate);
    }
}
