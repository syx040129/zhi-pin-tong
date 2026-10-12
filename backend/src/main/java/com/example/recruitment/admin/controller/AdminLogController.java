package com.example.recruitment.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.log.entity.SysLog;
import com.example.recruitment.log.mapper.SysLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：系统日志
 */
@Tag(name = "管理端-日志")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final SysLogMapper sysLogMapper;

    @Operation(summary = "日志分页查询")
    @GetMapping
    public Result<PageResult<SysLog>> page(@RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.hasText(type), SysLog::getType, type);
        qw.orderByDesc(SysLog::getId);
        return Result.ok(PageResult.of(sysLogMapper.selectPage(new Page<>(page, size), qw)));
    }
}
