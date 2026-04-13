package com.example.helpdesk.service;

import com.example.helpdesk.domain.Priority;
import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.Ticket;
import com.example.helpdesk.domain.TicketAttachment;
import com.example.helpdesk.domain.TicketComment;
import com.example.helpdesk.domain.TicketCommentAttachment;
import com.example.helpdesk.domain.TicketProcessingRecord;
import com.example.helpdesk.domain.TicketStatus;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.TicketModels;
import com.example.helpdesk.dto.UserModels;
import com.example.helpdesk.exception.BusinessException;
import com.example.helpdesk.repository.TicketCommentRepository;
import com.example.helpdesk.repository.TicketProcessingRecordRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketProcessingRecordRepository ticketProcessingRecordRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final CommentEmailService commentEmailService;
    private final CatalogService catalogService;

    public TicketService(TicketRepository ticketRepository,
                         TicketCommentRepository ticketCommentRepository,
                         TicketProcessingRecordRepository ticketProcessingRecordRepository,
                         UserRepository userRepository,
                         CurrentUserService currentUserService,
                         NotificationService notificationService,
                         FileStorageService fileStorageService,
                         CommentEmailService commentEmailService,
                         CatalogService catalogService) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketProcessingRecordRepository = ticketProcessingRecordRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.commentEmailService = commentEmailService;
        this.catalogService = catalogService;
    }

    @Transactional(readOnly = true)
    public TicketModels.PageResult<TicketModels.TicketSummaryResponse> listTickets(Long currentUserId,
                                                                                   TicketStatus status,
                                                                                   String category,
                                                                                   Priority priority,
                                                                                   String keyword,
                                                                                   Long assigneeId,
                                                                                   Boolean mine,
                                                                                   LocalDate startDate,
                                                                                   LocalDate endDate,
                                                                                   int page,
                                                                                   int size) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Specification<Ticket> specification = buildSpecification(
                currentUser,
                status,
                category,
                priority,
                keyword,
                assigneeId,
                mine,
                startDate,
                endDate
        );
        Page<Ticket> result = ticketRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new TicketModels.PageResult<>(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public TicketModels.TicketDetailResponse getTicketDetail(Long currentUserId, Long ticketId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketOrThrow(ticketId);
        ensureVisible(currentUser, ticket);
        return toDetail(ticket);
    }

    @Transactional
    public TicketModels.TicketDetailResponse createTicket(Long currentUserId, TicketModels.CreateTicketRequest request) {
        User creator = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setCreator(creator);
        applySubmittedValues(ticket, request);
        ticket.setStatus(TicketStatus.PENDING);

        Ticket saved = ticketRepository.save(ticket);
        appendAttachments(saved, creator, request.getFiles());
        saved = ticketRepository.save(saved);
        notificationService.notifyTicketCreated(saved);
        return toDetail(saved);
    }

    @Transactional
    public TicketModels.TicketDetailResponse saveDraft(Long currentUserId, TicketModels.CreateTicketRequest request) {
        User creator = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setCreator(creator);
        ticket.setStatus(TicketStatus.DRAFT);
        applyDraftValues(ticket, request);

        Ticket saved = ticketRepository.save(ticket);
        appendAttachments(saved, creator, request.getFiles());
        return toDetail(ticketRepository.save(saved));
    }

    @Transactional
    public TicketModels.TicketDetailResponse updateDraft(Long currentUserId,
                                                         Long ticketId,
                                                         TicketModels.CreateTicketRequest request) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        ensureDraftEditable(currentUser, ticket);
        applyDraftValues(ticket, request);
        appendAttachments(ticket, currentUser, request.getFiles());
        return toDetail(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketModels.TicketDetailResponse submitDraft(Long currentUserId,
                                                         Long ticketId,
                                                         TicketModels.CreateTicketRequest request) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        ensureDraftEditable(currentUser, ticket);
        applySubmittedValues(ticket, request);
        appendAttachments(ticket, currentUser, request.getFiles());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        Ticket saved = ticketRepository.save(ticket);
        notificationService.notifyTicketCreated(saved);
        return toDetail(saved);
    }

    @Transactional
    public TicketModels.TicketDetailResponse assignTicket(Long currentUserId, Long ticketId, Long assigneeId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(currentUser, Role.ADMIN);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        if (displayStatusOf(ticket) == TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "草稿需先提交后才能分配");
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "处理人不存在"));
        if (assignee.getRole() != Role.DEVELOPER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只能分配给开发人员");
        }
        if (ticket.getStatus().isResolvedLike()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已解决的问题不能再次分配");
        }

        LocalDateTime now = LocalDateTime.now();
        User previousAssignee = ticket.getAssignee();
        ticket.setAssignee(assignee);
        ticket.setStatus(TicketStatus.PROCESSING);
        syncActiveProcessingRecord(ticket, previousAssignee, assignee, now);
        Ticket saved = ticketRepository.save(ticket);
        notificationService.notifyAssignment(saved);
        return toDetail(saved);
    }

    @Transactional
    public TicketModels.TicketDetailResponse claimTicket(Long currentUserId, Long ticketId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(currentUser, Role.DEVELOPER);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        if (displayStatusOf(ticket) == TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "草稿不能接单");
        }
        if (ticket.getStatus().isResolvedLike()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已解决的问题不能再接单");
        }
        if (ticket.getAssignee() != null && !ticket.getAssignee().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该问题已被其他开发人员接收");
        }

        LocalDateTime now = LocalDateTime.now();
        User previousAssignee = ticket.getAssignee();
        ticket.setAssignee(currentUser);
        ticket.setStatus(TicketStatus.PROCESSING);
        syncActiveProcessingRecord(ticket, previousAssignee, currentUser, now);
        Ticket saved = ticketRepository.save(ticket);
        notificationService.notifyClaimed(saved);
        return toDetail(saved);
    }

    @Transactional
    public TicketModels.TicketDetailResponse startProcessing(Long currentUserId, Long ticketId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(currentUser, Role.DEVELOPER, Role.ADMIN);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        if (displayStatusOf(ticket) == TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "草稿需先提交后才能开始处理");
        }
        if (ticket.getStatus().isResolvedLike()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已解决的问题不能重复开始处理");
        }

        if (ticket.getAssignee() == null) {
            if (currentUser.getRole() != Role.DEVELOPER) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请先分配开发人员");
            }
            ticket.setAssignee(currentUser);
        }
        ensureAssigneeOrAdmin(currentUser, ticket);
        LocalDateTime now = LocalDateTime.now();
        User previousAssignee = ticket.getAssignee();
        ticket.setStatus(TicketStatus.PROCESSING);
        syncActiveProcessingRecord(ticket, previousAssignee, ticket.getAssignee(), now);
        Ticket saved = ticketRepository.save(ticket);
        notificationService.notifyProcessing(saved);
        return toDetail(saved);
    }

    @Transactional
    public TicketModels.CommentActionResponse addComment(Long currentUserId,
                                                         Long ticketId,
                                                         TicketModels.CommentRequest request) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        ensureVisible(currentUser, ticket);
        if (displayStatusOf(ticket) == TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "草稿不能添加处理备注");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        List<MultipartFile> files = Optional.ofNullable(request.getFiles()).orElse(List.of());
        boolean hasFiles = files.stream().anyMatch(file -> file != null && !file.isEmpty());
        if (!StringUtils.hasText(content) && !hasFiles) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "备注内容或附件不能为空");
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(currentUser);
        comment.setContent(content);

        int nextAttachmentSequence = countTicketAttachments(ticket) + 1;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            FileStorageService.StoredFile storedFile = fileStorageService.store(ticket.getId(), nextAttachmentSequence++, file);
            TicketCommentAttachment attachment = new TicketCommentAttachment();
            attachment.setComment(comment);
            attachment.setUploadedBy(currentUser);
            attachment.setOriginalName(storedFile.originalName());
            attachment.setStoredName(storedFile.storedName());
            attachment.setUrl(storedFile.url());
            attachment.setSize(storedFile.size());
            attachment.setContentType(storedFile.contentType());
            comment.getAttachments().add(attachment);
        }

        ticketCommentRepository.save(comment);
        ticket.getComments().add(comment);
        ticket.setUpdatedAt(LocalDateTime.now());
        notificationService.notifyComment(ticket, currentUser);
        TicketModels.EmailDispatchResponse emailDispatch = commentEmailService.sendCommentEmail(
                ticket,
                comment,
                currentUser,
                request.isSendEmail()
        );
        return new TicketModels.CommentActionResponse(toDetail(ticket), emailDispatch);
    }

    @Transactional
    public TicketModels.TicketDetailResponse resolveTicket(Long currentUserId, Long ticketId, String comment) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        if (displayStatusOf(ticket) == TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "草稿不能直接标记为已解决");
        }
        ensureAssigneeOrAdmin(currentUser, ticket);
        if (displayStatusOf(ticket) != TicketStatus.PROCESSING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "问题需先进入处理中，才能标记为已解决");
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(now);
        completeActiveProcessingRecord(ticket, now);
        ticketRepository.save(ticket);

        if (StringUtils.hasText(comment)) {
            TicketComment ticketComment = new TicketComment();
            ticketComment.setTicket(ticket);
            ticketComment.setAuthor(currentUser);
            ticketComment.setContent(comment.trim());
            ticketCommentRepository.save(ticketComment);
            ticket.getComments().add(ticketComment);
        }

        notificationService.notifyResolved(ticket);
        commentEmailService.sendResolvedEmail(ticket, currentUser, comment);
        return toDetail(ticket);
    }

    @Transactional
    public TicketModels.TicketDetailResponse closeTicket(Long currentUserId, Long ticketId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        boolean canClose = currentUser.getRole() == Role.ADMIN || ticket.getCreator().getId().equals(currentUser.getId());
        if (!canClose) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有提交人或管理员可以关闭该问题");
        }
        if (displayStatusOf(ticket) != TicketStatus.RESOLVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只有已解决的问题才能关闭");
        }

        if (ticket.getClosedAt() == null) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticket.setStatus(TicketStatus.RESOLVED);
        return toDetail(ticketRepository.save(ticket));
    }

    @Transactional
    public void deleteDraft(Long currentUserId, Long ticketId) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Ticket ticket = getTicketForUpdateOrThrow(ticketId);
        ensureDraftDeletable(currentUser, ticket);
        deleteStoredFiles(ticket);
        ticketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportTickets(Long currentUserId,
                                                TicketStatus status,
                                                String category,
                                                Priority priority,
                                                String keyword,
                                                Long assigneeId,
                                                Boolean mine,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        User currentUser = currentUserService.requireCurrentUser(currentUserId);
        Specification<Ticket> specification = buildSpecification(
                currentUser,
                status,
                category,
                priority,
                keyword,
                assigneeId,
                mine,
                startDate,
                endDate
        );
        List<Ticket> tickets = ticketRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("反馈编号,标题,类别,优先级,状态,提交人,处理人,创建时间,更新时间\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        tickets.forEach(ticket -> csv.append(csvValue(ticket.getTicketNo())).append(',')
                .append(csvValue(ticket.getTitle())).append(',')
                .append(csvValue(ticket.getCategory())).append(',')
                .append(csvValue(ticket.getPriority().getLabel())).append(',')
                .append(csvValue(displayStatusOf(ticket).getLabel())).append(',')
                .append(csvValue(ticket.getCreator().getName())).append(',')
                .append(csvValue(ticket.getAssignee() == null ? "" : ticket.getAssignee().getName())).append(',')
                .append(csvValue(formatter.format(ticket.getCreatedAt()))).append(',')
                .append(csvValue(formatter.format(ticket.getUpdatedAt()))).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=feedback-list.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Specification<Ticket> buildSpecification(User currentUser,
                                                     TicketStatus status,
                                                     String category,
                                                     Priority priority,
                                                     String keyword,
                                                     Long assigneeId,
                                                     Boolean mine,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getRole() == Role.EMPLOYEE) {
                predicates.add(cb.equal(root.get("creator").get("id"), currentUser.getId()));
            } else if (currentUser.getRole() == Role.DEVELOPER) {
                predicates.add(cb.notEqual(root.get("status"), TicketStatus.DRAFT));
                if (Boolean.TRUE.equals(mine)) {
                    predicates.add(cb.equal(root.get("assignee").get("id"), currentUser.getId()));
                }
            }

            if (status != null) {
                TicketStatus normalizedStatus = status.toDisplayStatus();
                if (normalizedStatus == TicketStatus.PENDING) {
                    predicates.add(cb.or(
                            cb.equal(root.get("status"), TicketStatus.NEW),
                            cb.and(
                                    cb.equal(root.get("status"), TicketStatus.PENDING),
                                    cb.isNull(root.get("assignee"))
                            )
                    ));
                } else if (normalizedStatus == TicketStatus.PROCESSING) {
                    predicates.add(cb.or(
                            cb.equal(root.get("status"), TicketStatus.PROCESSING),
                            cb.and(
                                    cb.equal(root.get("status"), TicketStatus.PENDING),
                                    cb.isNotNull(root.get("assignee"))
                            )
                    ));
                } else if (normalizedStatus == TicketStatus.RESOLVED) {
                    predicates.add(root.get("status").in(TicketStatus.RESOLVED, TicketStatus.CLOSED));
                } else {
                    predicates.add(cb.equal(root.get("status"), normalizedStatus));
                }
            }

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("description"), pattern),
                        cb.like(root.get("ticketNo"), pattern)
                ));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void ensureVisible(User currentUser, Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.DRAFT) {
            boolean canViewDraft = currentUser.getRole() == Role.ADMIN || ticket.getCreator().getId().equals(currentUser.getId());
            if (!canViewDraft) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "没有权限查看该草稿");
            }
            return;
        }

        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.DEVELOPER) {
            return;
        }
        if (!ticket.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "没有权限查看该问题");
        }
    }

    private void ensureAssigneeOrAdmin(User currentUser, Ticket ticket) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.DEVELOPER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前角色不能处理该问题");
        }
        if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有当前处理人可以执行此操作");
        }
    }

    private void ensureDraftEditable(User currentUser, Ticket ticket) {
        ensureVisible(currentUser, ticket);
        if (ticket.getStatus() != TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只有草稿可以继续编辑");
        }
        if (!ticket.getCreator().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有草稿提交人可以编辑草稿");
        }
    }

    private void ensureDraftDeletable(User currentUser, Ticket ticket) {
        ensureVisible(currentUser, ticket);
        if (ticket.getStatus() != TicketStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只有草稿可以删除");
        }
        boolean canDelete = currentUser.getRole() == Role.ADMIN || ticket.getCreator().getId().equals(currentUser.getId());
        if (!canDelete) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有草稿提交人或管理员可以删除草稿");
        }
    }

    private Ticket getTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "问题不存在"));
    }

    private Ticket getTicketForUpdateOrThrow(Long ticketId) {
        return ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "问题不存在"));
    }

    private int countTicketAttachments(Ticket ticket) {
        int ticketAttachmentCount = ticket.getAttachments().size();
        int commentAttachmentCount = ticket.getComments().stream()
                .mapToInt(comment -> comment.getAttachments().size())
                .sum();
        return ticketAttachmentCount + commentAttachmentCount;
    }

    private String generateTicketNo() {
        long nextId = ticketRepository.findTopByOrderByIdDesc()
                .map(ticket -> ticket.getId() + 1)
                .orElse(1L);
        return "GD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", nextId);
    }

    private void applySubmittedValues(Ticket ticket, TicketModels.CreateTicketRequest request) {
        catalogService.validateCategory(request.getCategory());
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setCategory(request.getCategory().trim());
        ticket.setPriority(request.getPriority());
    }

    private void applyDraftValues(Ticket ticket, TicketModels.CreateTicketRequest request) {
        ticket.setTitle(normalizeDraftText(request.getTitle()));
        ticket.setDescription(normalizeDraftText(request.getDescription()));

        if (StringUtils.hasText(request.getCategory())) {
            String category = request.getCategory().trim();
            catalogService.validateCategory(category);
            ticket.setCategory(category);
        } else {
            ticket.setCategory("");
        }

        ticket.setPriority(request.getPriority() == null ? Priority.MEDIUM : request.getPriority());
    }

    private String normalizeDraftText(String value) {
        return value == null ? "" : value.trim();
    }

    private void appendAttachments(Ticket ticket, User uploader, List<MultipartFile> files) {
        List<MultipartFile> safeFiles = Optional.ofNullable(files).orElse(List.of());
        int nextAttachmentSequence = countTicketAttachments(ticket) + 1;
        for (MultipartFile file : safeFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            FileStorageService.StoredFile storedFile = fileStorageService.store(ticket.getId(), nextAttachmentSequence++, file);
            TicketAttachment attachment = new TicketAttachment();
            attachment.setTicket(ticket);
            attachment.setUploadedBy(uploader);
            attachment.setOriginalName(storedFile.originalName());
            attachment.setStoredName(storedFile.storedName());
            attachment.setUrl(storedFile.url());
            attachment.setSize(storedFile.size());
            attachment.setContentType(storedFile.contentType());
            ticket.getAttachments().add(attachment);
        }
    }

    private void syncActiveProcessingRecord(Ticket ticket, User previousAssignee, User assignee, LocalDateTime now) {
        TicketProcessingRecord activeRecord = ticketProcessingRecordRepository
                .findFirstByTicketIdAndEndedAtIsNullOrderByStartedAtDesc(ticket.getId())
                .orElse(null);

        if (activeRecord != null) {
            if (activeRecord.getAssignee().getId().equals(assignee.getId())) {
                return;
            }
            activeRecord.setEndedAt(now);
            activeRecord.setResolved(false);
            ticketProcessingRecordRepository.save(activeRecord);
        } else if (previousAssignee != null) {
            if (Objects.equals(previousAssignee.getId(), assignee.getId())) {
                createProcessingRecord(ticket, assignee, inferProcessingStartedAt(ticket, now), null, false);
                return;
            }
            createProcessingRecord(ticket, previousAssignee, inferProcessingStartedAt(ticket, now), now, false);
        }

        createProcessingRecord(ticket, assignee, now, null, false);
    }

    private void completeActiveProcessingRecord(Ticket ticket, LocalDateTime now) {
        if (ticket.getAssignee() == null) {
            return;
        }

        TicketProcessingRecord activeRecord = ticketProcessingRecordRepository
                .findFirstByTicketIdAndEndedAtIsNullOrderByStartedAtDesc(ticket.getId())
                .orElse(null);

        if (activeRecord == null || !activeRecord.getAssignee().getId().equals(ticket.getAssignee().getId())) {
            createProcessingRecord(ticket, ticket.getAssignee(), inferProcessingStartedAt(ticket, now), now, true);
            return;
        }

        activeRecord.setEndedAt(now);
        activeRecord.setResolved(true);
        ticketProcessingRecordRepository.save(activeRecord);
    }

    private void createProcessingRecord(Ticket ticket,
                                        User assignee,
                                        LocalDateTime startedAt,
                                        LocalDateTime endedAt,
                                        boolean resolved) {
        TicketProcessingRecord record = new TicketProcessingRecord();
        record.setTicket(ticket);
        record.setAssignee(assignee);
        record.setStartedAt(startedAt);
        record.setEndedAt(endedAt);
        record.setResolved(resolved);
        ticketProcessingRecordRepository.save(record);
    }

    private LocalDateTime inferProcessingStartedAt(Ticket ticket, LocalDateTime fallback) {
        if (ticket.getUpdatedAt() != null && !ticket.getUpdatedAt().isAfter(fallback)) {
            return ticket.getUpdatedAt();
        }
        if (ticket.getCreatedAt() != null && !ticket.getCreatedAt().isAfter(fallback)) {
            return ticket.getCreatedAt();
        }
        return fallback;
    }

    private void deleteStoredFiles(Ticket ticket) {
        ticket.getAttachments().forEach(item -> fileStorageService.deleteByUrl(item.getUrl()));
        ticket.getComments().forEach(comment ->
                comment.getAttachments().forEach(item -> fileStorageService.deleteByUrl(item.getUrl()))
        );
    }

    private TicketModels.TicketSummaryResponse toSummary(Ticket ticket) {
        TicketStatus displayStatus = displayStatusOf(ticket);
        return new TicketModels.TicketSummaryResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getPriority().getLabel(),
                displayStatus,
                displayStatus.getLabel(),
                toUserResponse(ticket.getCreator()),
                ticket.getAssignee() == null ? null : toUserResponse(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private TicketModels.TicketDetailResponse toDetail(Ticket ticket) {
        TicketStatus displayStatus = displayStatusOf(ticket);
        return new TicketModels.TicketDetailResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getPriority().getLabel(),
                displayStatus,
                displayStatus.getLabel(),
                toUserResponse(ticket.getCreator()),
                ticket.getAssignee() == null ? null : toUserResponse(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getAttachments().stream()
                        .map(this::toAttachmentResponse)
                        .toList(),
                ticket.getComments().stream()
                        .map(item -> new TicketModels.TicketCommentResponse(
                                item.getId(),
                                item.getContent(),
                                toUserResponse(item.getAuthor()),
                                item.getCreatedAt(),
                                item.getAttachments().stream()
                                        .map(this::toAttachmentResponse)
                                        .toList()
                        ))
                        .toList()
        );
    }

    private TicketModels.AttachmentResponse toAttachmentResponse(TicketAttachment item) {
        return new TicketModels.AttachmentResponse(
                item.getId(),
                item.getOriginalName(),
                item.getUrl(),
                item.getSize(),
                item.getContentType(),
                item.getUploadedBy().getName(),
                item.getUploadedAt()
        );
    }

    private TicketModels.AttachmentResponse toAttachmentResponse(TicketCommentAttachment item) {
        return new TicketModels.AttachmentResponse(
                item.getId(),
                item.getOriginalName(),
                item.getUrl(),
                item.getSize(),
                item.getContentType(),
                item.getUploadedBy().getName(),
                item.getUploadedAt()
        );
    }

    private UserModels.UserResponse toUserResponse(User user) {
        return new UserModels.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getRole().getLabel(),
                user.isEnabled()
        );
    }

    private TicketStatus displayStatusOf(Ticket ticket) {
        return ticket.getStatus().toDisplayStatus(ticket.getAssignee() != null);
    }

    private String csvValue(String value) {
        String normalized = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + normalized + "\"";
    }
}
