package com.example.recruitment.file.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import com.example.recruitment.file.service.ResumeFileAccessService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 通用文件上传接口
 */
@Tag(name = "文件模块")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final ResumeFileAccessService resumeFileAccessService;

    @GetMapping("/resume/{month}/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadResume(@PathVariable String month,
                                                    @PathVariable String filename) {
        Resource resource = new FileSystemResource(resumeFileAccessService.requireFile(
                "/api/files/resume/" + month + "/" + filename));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @Operation(summary = "上传文件", description = "category: avatar/logo/image/resume")
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam("category") String category) {
        String url = fileService.upload(file, category);
        return Result.ok(Map.of("url", url));
    }
}
