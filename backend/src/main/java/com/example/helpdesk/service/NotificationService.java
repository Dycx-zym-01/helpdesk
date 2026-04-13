package com.example.helpdesk.service;

import com.example.helpdesk.domain.Notification;
import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.Ticket;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.NotificationModels;
import com.example.helpdesk.exception.BusinessException;
import com.example.helpdesk.repository.NotificationRepository;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    public static final String TYPE_TICKET_CREATED = "TICKET_CREATED";
    public static final String TYPE_ASSIGNED_TO_YOU = "ASSIGNED_TO_YOU";
    public static final String TYPE_ASSIGNMENT_CONFIRMED = "ASSIGNMENT_CONFIRMED";
    public static final String TYPE_CLAIMED = "CLAIMED";
    public static final String TYPE_PROCESSING = "PROCESSING";
    public static final String TYPE_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String TYPE_RESOLVED = "RESOLVED";
    public static final String TYPE_CLOSED = "CLOSED";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<NotificationModels.NotificationResponse> listNotifications(Long userId) {
        User user = currentUserService.requireCurrentUser(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        User user = currentUserService.requireCurrentUser(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "通知不存在"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能操作他人的通知");
        }
        notification.setReadFlag(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        User user = currentUserService.requireCurrentUser(userId);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(item -> item.setReadFlag(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void notifyTicketCreated(Ticket ticket) {
        List<User> users = userRepository.findByRoleIn(List.of(Role.DEVELOPER, Role.ADMIN));
        notifyUsers(users, ticket, TYPE_TICKET_CREATED, null);
    }

    @Transactional
    public void notifyAssignment(Ticket ticket) {
        if (ticket.getAssignee() != null) {
            createNotification(ticket.getAssignee(), ticket, TYPE_ASSIGNED_TO_YOU, null);
        }
        createNotification(ticket.getCreator(), ticket, TYPE_ASSIGNMENT_CONFIRMED, safeUserName(ticket.getAssignee()));
    }

    @Transactional
    public void notifyClaimed(Ticket ticket) {
        createNotification(ticket.getCreator(), ticket, TYPE_CLAIMED, safeUserName(ticket.getAssignee()));
    }

    @Transactional
    public void notifyProcessing(Ticket ticket) {
        createNotification(ticket.getCreator(), ticket, TYPE_PROCESSING, null);
    }

    @Transactional
    public void notifyResolved(Ticket ticket) {
        createNotification(ticket.getCreator(), ticket, TYPE_RESOLVED, null);
    }

    @Transactional
    public void notifyComment(Ticket ticket, User author) {
        Set<User> receivers = new LinkedHashSet<>();
        receivers.add(ticket.getCreator());
        if (ticket.getAssignee() != null) {
            receivers.add(ticket.getAssignee());
        }
        receivers.removeIf(user -> user.getId().equals(author.getId()));
        notifyUsers(receivers, ticket, TYPE_COMMENT_ADDED, null);
    }

    @Transactional
    public void notifyClosed(Ticket ticket) {
        if (ticket.getAssignee() != null) {
            createNotification(ticket.getAssignee(), ticket, TYPE_CLOSED, null);
        }
    }

    @Transactional
    public void createNotification(User user, Ticket ticket, String typeKey, String actorName) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTicket(ticket);
        notification.setTypeKey(typeKey);
        notification.setActorName(actorName);
        notification.setTitle(legacyTitle(typeKey));
        notification.setContent(legacyContent(typeKey, ticket, actorName));
        notificationRepository.save(notification);
    }

    private void notifyUsers(Iterable<User> users, Ticket ticket, String typeKey, String actorName) {
        Set<Long> dedup = new LinkedHashSet<>();
        for (User user : users) {
            if (dedup.add(user.getId())) {
                createNotification(user, ticket, typeKey, actorName);
            }
        }
    }

    private NotificationModels.NotificationResponse toResponse(Notification notification) {
        String resolvedTypeKey = resolveTypeKey(notification);
        String resolvedActorName = resolveActorName(notification, resolvedTypeKey);
        return new NotificationModels.NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                resolvedTypeKey,
                resolvedActorName,
                notification.isReadFlag(),
                notification.getTicket() == null ? null : notification.getTicket().getId(),
                notification.getTicket() == null ? null : notification.getTicket().getTicketNo(),
                notification.getCreatedAt()
        );
    }

    private String resolveTypeKey(Notification notification) {
        if (StringUtils.hasText(notification.getTypeKey())) {
            return notification.getTypeKey();
        }
        String title = notification.getTitle();
        if ("新工单待处理".equals(title) || "新问题待处理".equals(title)) {
            return TYPE_TICKET_CREATED;
        }
        if ("工单已分配给你".equals(title) || "问题已分派给你".equals(title)) {
            return TYPE_ASSIGNED_TO_YOU;
        }
        if ("工单已完成分配".equals(title) || "问题已完成分派".equals(title)) {
            return TYPE_ASSIGNMENT_CONFIRMED;
        }
        if ("工单已被接单".equals(title) || "问题已被接收".equals(title)) {
            return TYPE_CLAIMED;
        }
        if ("工单处理中".equals(title) || "问题处理中".equals(title)) {
            return TYPE_PROCESSING;
        }
        if ("工单有新备注".equals(title) || "问题有新备注".equals(title)) {
            return TYPE_COMMENT_ADDED;
        }
        if ("工单已解决".equals(title) || "问题已解决".equals(title)) {
            return TYPE_RESOLVED;
        }
        if ("工单已关闭".equals(title) || "问题已关闭".equals(title)) {
            return TYPE_CLOSED;
        }
        return null;
    }

    private String resolveActorName(Notification notification, String typeKey) {
        if (StringUtils.hasText(notification.getActorName())) {
            return notification.getActorName();
        }
        if (TYPE_ASSIGNMENT_CONFIRMED.equals(typeKey) || TYPE_CLAIMED.equals(typeKey)) {
            return safeUserName(notification.getTicket() == null ? null : notification.getTicket().getAssignee());
        }
        return null;
    }

    private String legacyTitle(String typeKey) {
        return switch (typeKey) {
            case TYPE_TICKET_CREATED -> "新问题待处理";
            case TYPE_ASSIGNED_TO_YOU -> "问题已分派给你";
            case TYPE_ASSIGNMENT_CONFIRMED -> "问题已完成分派";
            case TYPE_CLAIMED -> "问题已被接收";
            case TYPE_PROCESSING -> "问题处理中";
            case TYPE_COMMENT_ADDED -> "问题有新备注";
            case TYPE_RESOLVED -> "问题已解决";
            case TYPE_CLOSED -> "问题已关闭";
            default -> "问题通知";
        };
    }

    private String legacyContent(String typeKey, Ticket ticket, String actorName) {
        String ticketNo = ticket == null ? "-" : ticket.getTicketNo();
        return switch (typeKey) {
            case TYPE_TICKET_CREATED -> "问题 " + ticketNo + " 已提交，请及时处理。";
            case TYPE_ASSIGNED_TO_YOU -> ticketNo + " 已分派到你名下，请开始处理。";
            case TYPE_ASSIGNMENT_CONFIRMED -> ticketNo + " 已分派给：" + safeActorName(actorName) + "，并进入处理中。";
            case TYPE_CLAIMED -> ticketNo + " 已由 " + safeActorName(actorName) + " 接单处理。";
            case TYPE_PROCESSING -> ticketNo + " 已进入处理中状态。";
            case TYPE_COMMENT_ADDED -> ticketNo + " 新增了一条处理备注。";
            case TYPE_RESOLVED -> ticketNo + " 已处理完成，请查看处理结果。";
            case TYPE_CLOSED -> ticketNo + " 已由提交人确认关闭。";
            default -> ticketNo + " 有新的问题通知。";
        };
    }

    private String safeActorName(String actorName) {
        return StringUtils.hasText(actorName) ? actorName : "未分配";
    }

    private String safeUserName(User user) {
        return user == null ? "未分配" : user.getName();
    }
}
