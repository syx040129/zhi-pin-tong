package com.example.recruitment.resume.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.resume.dto.ResumeForm;
import com.example.recruitment.resume.dto.ResumeParseVO;
import com.example.recruitment.resume.dto.ResumeVO;
import com.example.recruitment.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历接口
 */
@Tag(name = "简历模块")
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "我的简历列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public Result<List<ResumeVO>> list() {
        return Result.ok(resumeService.myResumes());
    }

    @Operation(summary = "简历详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<ResumeVO> detail(@PathVariable Long id) {
        return Result.ok(resumeService.detail(id));
    }

    @Operation(summary = "新建简历")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "简历管理", action = "新建简历")
    public Result<ResumeVO> create(@Valid @RequestBody ResumeForm form) {
        return Result.ok(resumeService.create(form));
    }

    @Operation(summary = "修改简历")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "简历管理", action = "修改简历")
    public Result<ResumeVO> update(@PathVariable Long id, @Valid @RequestBody ResumeForm form) {
        return Result.ok(resumeService.update(id, form));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "简历管理", action = "删除简历")
    public Result<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "上传简历文件（PDF / Word）")
    @PostMapping("/{id}/file")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "简历管理", action = "上传简历文件")
    public Result<ResumeVO> uploadFile(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file) {
        return Result.ok(resumeService.uploadFile(id, file));
    }

    @Operation(summary = "解析简历（文件→文本→结构化预览，确认后保存）")
    @PostMapping("/{id}/parse")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "简历管理", action = "AI 解析简历")
    public Result<ResumeParseVO> parse(@PathVariable Long id) {
        return Result.ok(resumeService.parse(id));
    }
}
