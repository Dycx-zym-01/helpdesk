package com.example.helpdesk.service;

import com.example.helpdesk.domain.Ticket;
import com.example.helpdesk.domain.TicketComment;
import com.example.helpdesk.domain.TicketStatus;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.TicketModels;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class CommentEmailService {

    private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.mail.subject-prefix:[Helpdesk]}")
    private String subjectPrefix;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public CommentEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public TicketModels.EmailDispatchResponse sendCommentEmail(Ticket ticket,
                                                               TicketComment comment,
                                                               User author,
                                                               boolean requested) {
        if (!requested) {
            return new TicketModels.EmailDispatchResponse(false, false, 0, null);
        }
        if (!canSendCommentEmail(ticket)) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    0,
                    "\u5de5\u5355\u9700\u5148\u63a5\u5355\u6216\u5206\u914d\u540e\uff0c\u624d\u80fd\u53d1\u9001\u90ae\u4ef6\u901a\u77e5"
            );
        }

        Set<String> recipientEmails = resolveCommentRecipientEmails(ticket, author);
        return dispatchEmail(
                recipientEmails,
                ticket.getTicketNo() + " \u6709\u65b0\u5907\u6ce8",
                buildCommentBody(ticket, comment, author)
        );
    }

    public TicketModels.EmailDispatchResponse sendResolvedEmail(Ticket ticket, User resolver, String resolutionComment) {
        if (ticket == null || ticket.getCreator() == null || !StringUtils.hasText(ticket.getCreator().getEmail())) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    0,
                    "\u6ca1\u6709\u627e\u5230\u63d0\u4ea4\u4eba\u7684\u90ae\u7bb1\uff0c\u65e0\u6cd5\u53d1\u9001\u89e3\u51b3\u901a\u77e5"
            );
        }

        Set<String> recipientEmails = new LinkedHashSet<>();
        recipientEmails.add(ticket.getCreator().getEmail().trim());
        return dispatchEmail(
                recipientEmails,
                ticket.getTicketNo() + " \u5df2\u89e3\u51b3",
                buildResolvedBody(ticket, resolver, resolutionComment)
        );
    }

    private boolean canSendCommentEmail(Ticket ticket) {
        return ticket != null
                && ticket.getAssignee() != null
                && ticket.getStatus() != null
                && ticket.getStatus() != TicketStatus.DRAFT
                && ticket.getStatus().hasStartedProcessing(ticket.getAssignee() != null);
    }

    private Set<String> resolveCommentRecipientEmails(Ticket ticket, User author) {
        Set<String> emails = new LinkedHashSet<>();
        addUserEmail(emails, ticket.getCreator(), author);
        addUserEmail(emails, ticket.getAssignee(), author);
        return emails;
    }

    private void addUserEmail(Set<String> emails, User user, User author) {
        if (user == null || author == null) {
            return;
        }
        if (user.getId().equals(author.getId())) {
            return;
        }
        if (StringUtils.hasText(user.getEmail())) {
            emails.add(user.getEmail().trim());
        }
    }

    private TicketModels.EmailDispatchResponse dispatchEmail(Set<String> recipientEmails, String subjectSuffix, String body) {
        if (recipientEmails.isEmpty()) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    0,
                    "\u6ca1\u6709\u627e\u5230\u53ef\u63a5\u6536\u90ae\u4ef6\u7684\u90ae\u7bb1"
            );
        }
        if (!mailEnabled) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    recipientEmails.size(),
                    "\u90ae\u4ef6\u529f\u80fd\u672a\u542f\u7528\uff0c\u8bf7\u5148\u914d\u7f6e SMTP"
            );
        }
        if (!StringUtils.hasText(mailHost)) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    recipientEmails.size(),
                    "\u672a\u914d\u7f6e\u90ae\u4ef6\u670d\u52a1\u5668\u5730\u5740"
            );
        }
        if (!StringUtils.hasText(fromAddress)) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    recipientEmails.size(),
                    "\u672a\u914d\u7f6e\u53d1\u4ef6\u4eba\u90ae\u7bb1"
            );
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    recipientEmails.size(),
                    "\u90ae\u4ef6\u670d\u52a1\u672a\u521d\u59cb\u5316\uff0c\u8bf7\u68c0\u67e5\u90ae\u4ef6\u914d\u7f6e"
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmails.toArray(String[]::new));
        message.setSubject(subjectPrefix + " " + subjectSuffix);
        message.setText(body);

        try {
            mailSender.send(message);
            return new TicketModels.EmailDispatchResponse(
                    true,
                    true,
                    recipientEmails.size(),
                    "\u5df2\u5411 " + recipientEmails.size() + " \u4e2a\u6536\u4ef6\u4eba\u53d1\u9001\u90ae\u4ef6\u901a\u77e5"
            );
        } catch (MailException ex) {
            return new TicketModels.EmailDispatchResponse(
                    true,
                    false,
                    recipientEmails.size(),
                    "\u90ae\u4ef6\u53d1\u9001\u5931\u8d25\uff1a" + simplifyMessage(ex.getMessage())
            );
        }
    }

    private String buildCommentBody(Ticket ticket, TicketComment comment, User author) {
        StringBuilder body = new StringBuilder();
        body.append("\u5de5\u5355\u7f16\u53f7\uff1a").append(ticket.getTicketNo()).append('\n');
        body.append("\u5de5\u5355\u6807\u9898\uff1a").append(ticket.getTitle()).append('\n');
        body.append("\u5907\u6ce8\u4eba\uff1a").append(author.getName()).append('\n');
        body.append("\u5907\u6ce8\u65f6\u95f4\uff1a").append(EMAIL_TIME_FORMATTER.format(comment.getCreatedAt())).append('\n');
        body.append('\n');

        if (StringUtils.hasText(comment.getContent())) {
            body.append("\u5907\u6ce8\u5185\u5bb9\uff1a").append('\n');
            body.append(comment.getContent()).append('\n').append('\n');
        }

        if (!comment.getAttachments().isEmpty()) {
            body.append("\u9644\u4ef6\uff1a").append('\n');
            comment.getAttachments().forEach(item -> body.append("- ").append(item.getOriginalName()).append('\n'));
        }
        return body.toString();
    }

    private String buildResolvedBody(Ticket ticket, User resolver, String resolutionComment) {
        StringBuilder body = new StringBuilder();
        body.append("\u5de5\u5355\u7f16\u53f7\uff1a").append(ticket.getTicketNo()).append('\n');
        body.append("\u5de5\u5355\u6807\u9898\uff1a").append(ticket.getTitle()).append('\n');
        body.append("\u5904\u7406\u72b6\u6001\uff1a\u5df2\u89e3\u51b3").append('\n');
        body.append("\u5904\u7406\u4eba\uff1a").append(resolver == null ? "-" : resolver.getName()).append('\n');
        body.append("\u89e3\u51b3\u65f6\u95f4\uff1a")
                .append(ticket.getResolvedAt() == null ? "-" : EMAIL_TIME_FORMATTER.format(ticket.getResolvedAt()))
                .append('\n')
                .append('\n');

        if (StringUtils.hasText(resolutionComment)) {
            body.append("\u5904\u7406\u8bf4\u660e\uff1a").append('\n');
            body.append(resolutionComment.trim()).append('\n').append('\n');
        }

        body.append("\u8be5\u95ee\u9898\u5df2\u5904\u7406\u5b8c\u6210\uff0c\u8bf7\u767b\u5f55\u7cfb\u7edf\u67e5\u770b\u6700\u65b0\u8fdb\u5c55\u3002");
        return body.toString();
    }

    private String simplifyMessage(String message) {
        return StringUtils.hasText(message) ? message : "\u672a\u77e5\u9519\u8bef";
    }
}
