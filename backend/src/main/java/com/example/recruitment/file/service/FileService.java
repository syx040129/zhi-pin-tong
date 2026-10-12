package com.example.recruitment.file.service;

import com.example.recruitment.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务：保存到本地 upload 目录并通过 /api/files/** 对外提供访问
 */
@Slf4j
@Service
public class FileService {

    private final Path baseDir;

    private static final Map<String, Set<String>> ALLOWED_EXT = Map.of(
            "avatar", Set.of("jpg", "jpeg", "png", "gif", "webp"),
            "logo", Set.of("jpg", "jpeg", "png", "gif", "webp"),
            "image", Set.of("jpg", "jpeg", "png", "gif", "webp"),
            "resume", Set.of("pdf", "doc", "docx", "txt")
    );

    public FileService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 保存上传文件，返回可访问的相对 URL
     */
    public String upload(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        Set<String> allowed = ALLOWED_EXT.get(category);
        if (allowed == null || !allowed.contains(ext)) {
            throw BusinessException.badRequest("不支持的文件类型: " + ext);
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetDir = baseDir.resolve(category).resolve(month);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(fileName).toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw BusinessException.badRequest("文件保存失败");
        }
        return "/api/files/" + category + "/" + month + "/" + fileName;
    }

    /**
     * 根据文件访问 URL 解析本地文件路径
     */
    public java.nio.file.Path resolveByUrl(String url) {
        if (url == null || !url.startsWith("/api/files/")) {
            return null;
        }
        String relative = url.substring("/api/files/".length()).replace('/', java.io.File.separatorChar);
        Path resolved = baseDir.resolve(relative).normalize();
        return resolved.startsWith(baseDir) ? resolved : null;
    }
}
