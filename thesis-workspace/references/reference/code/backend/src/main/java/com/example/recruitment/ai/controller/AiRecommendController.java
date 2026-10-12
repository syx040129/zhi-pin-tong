package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.RecommendItemVO;
import com.example.recruitment.ai.service.RecommendationService;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 智能岗位推荐接口
 */
@Tag(name = "AI 岗位推荐")
@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class AiRecommendController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取智能推荐岗位（含 6 小时缓存，首次自动计算）")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<List<RecommendItemVO>> recommendations() {
        return Result.ok(recommendationService.recommendations(
                SecurityUtils.currentUserId(), false));
    }

    @Operation(summary = "重新计算智能推荐")
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('USER')")
    public Result<List<RecommendItemVO>> refresh() {
        return Result.ok(recommendationService.refresh(SecurityUtils.currentUserId()));
    }
}
