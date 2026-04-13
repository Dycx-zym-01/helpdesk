package com.example.helpdesk.service;

import com.example.helpdesk.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {

    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(uploadDir));
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化上传目录", e);
        }
    }

    public StoredFile store(Long ticketId, int sequence, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (ticketId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "问题ID不能为空");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename());
        String dateFolder = LocalDate.now().format(DATE_FOLDER_FORMATTER);
        String storedName = buildStoredName(ticketId, sequence, originalName);
        Path targetDirectory = Path.of(uploadDir).resolve(dateFolder);
        Path target = targetDirectory.resolve(storedName);
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(
                    originalName,
                    dateFolder + "/" + storedName,
                    "/uploads/" + dateFolder + "/" + storedName,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
    }

    public void deleteByUrl(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith("/uploads/")) {
            return;
        }
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path target = root.resolve(url.substring("/uploads/".length())).normalize();
            if (!target.startsWith(root)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException | InvalidPathException ignored) {
            // Ignore cleanup failures so draft deletion is not blocked by stale files.
        }
    }

    private String buildStoredName(Long ticketId, int sequence, String originalName) {
        String normalizedFileName = normalizeFileName(originalName);
        return ticketId + "-" + sequence + "-" + normalizedFileName;
    }

    private String normalizeFileName(String originalName) {
        String extension = StringUtils.getFilenameExtension(originalName);
        String baseName = StringUtils.stripFilenameExtension(originalName);
        String normalizedBaseName = normalizeBaseName(baseName);
        if (!StringUtils.hasText(normalizedBaseName)) {
            normalizedBaseName = "file";
        }
        if (!StringUtils.hasText(extension)) {
            return normalizedBaseName;
        }
        String normalizedExtension = extension.trim().replaceAll("[^\\p{L}\\p{N}]", "");
        return StringUtils.hasText(normalizedExtension)
                ? normalizedBaseName + "." + normalizedExtension
                : normalizedBaseName;
    }

    private String normalizeBaseName(String baseName) {
        if (!StringUtils.hasText(baseName)) {
            return "";
        }
        String normalized = baseName.trim().replaceAll("[^\\p{L}\\p{N}._-]", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.replaceAll("^_+|_+$", "");
    }

    public record StoredFile(
            String originalName,
            String storedName,
            String url,
            long size,
            String contentType
    ) {
    }
}
