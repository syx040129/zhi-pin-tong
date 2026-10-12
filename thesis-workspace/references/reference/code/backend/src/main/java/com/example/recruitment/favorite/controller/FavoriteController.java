package com.example.recruitment.favorite.controller;

import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.favorite.dto.FavoriteVO;
import com.example.recruitment.favorite.service.FavoriteService;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 岗位收藏接口
 */
@Tag(name = "收藏模块")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏岗位")
    @PostMapping("/{jobId}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "岗位收藏", action = "收藏岗位")
    public Result<Void> add(@PathVariable Long jobId) {
        favoriteService.add(jobId, SecurityUtils.currentUserId());
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "岗位收藏", action = "取消收藏")
    public Result<Void> remove(@PathVariable Long jobId) {
        favoriteService.remove(jobId, SecurityUtils.currentUserId());
        return Result.ok();
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<FavoriteVO>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.ok(favoriteService.page(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(summary = "是否已收藏某岗位")
    @GetMapping("/status/{jobId}")
    @PreAuthorize("hasRole('USER')")
    public Result<Map<String, Boolean>> status(@PathVariable Long jobId) {
        return Result.ok(Map.of("favorite",
                favoriteService.isFavorite(jobId, SecurityUtils.currentUserId())));
    }
}
