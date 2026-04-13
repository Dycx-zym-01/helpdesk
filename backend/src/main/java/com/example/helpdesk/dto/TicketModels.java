package com.example.helpdesk.dto;

import com.example.helpdesk.domain.Priority;
import com.example.helpdesk.domain.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketModels {

    private TicketModels() {
    }

    public static class CreateTicketRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Priority is required")
        private Priority priority;

        @NotBlank(message = "Description is required")
        private String description;

        private List<MultipartFile> files = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<MultipartFile> getFiles() {
            return files;
        }

        public void setFiles(List<MultipartFile> files) {
            this.files = files;
        }
    }

    public record AssignTicketRequest(
            @NotNull(message = "Assignee is required") Long assigneeId
    ) {
    }

    public static class CommentRequest {
        private String content;

        private boolean sendEmail;

        private List<MultipartFile> files = new ArrayList<>();

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isSendEmail() {
            return sendEmail;
        }

        public void setSendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
        }

        public List<MultipartFile> getFiles() {
            return files;
        }

        public void setFiles(List<MultipartFile> files) {
            this.files = files;
        }
    }

    public record ResolveTicketRequest(
            String comment
    ) {
    }

    public record AttachmentResponse(
            Long id,
            String originalName,
            String url,
            long size,
            String contentType,
            String uploadedBy,
            LocalDateTime uploadedAt
    ) {
    }

    public record TicketCommentResponse(
            Long id,
            String content,
            UserModels.UserResponse author,
            LocalDateTime createdAt,
            List<AttachmentResponse> attachments
    ) {
    }

    public record TicketSummaryResponse(
            Long id,
            String ticketNo,
            String title,
            String category,
            Priority priority,
            String priorityLabel,
            TicketStatus status,
            String statusLabel,
            UserModels.UserResponse creator,
            UserModels.UserResponse assignee,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record TicketDetailResponse(
            Long id,
            String ticketNo,
            String title,
            String description,
            String category,
            Priority priority,
            String priorityLabel,
            TicketStatus status,
            String statusLabel,
            UserModels.UserResponse creator,
            UserModels.UserResponse assignee,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime resolvedAt,
            LocalDateTime closedAt,
            List<AttachmentResponse> attachments,
            List<TicketCommentResponse> comments
    ) {
    }

    public record EmailDispatchResponse(
            boolean requested,
            boolean sent,
            int recipientCount,
            String message
    ) {
    }

    public record CommentActionResponse(
            TicketDetailResponse ticket,
            EmailDispatchResponse emailDispatch
    ) {
    }

    public record PageResult<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {
    }
}
