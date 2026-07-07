package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.SelectedReadingItem;
import com.english.entity.UserSelectedReadingFavorite;
import com.english.service.SelectedReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "精选读物接口", description = "精选读物列表和精选读物收藏接口")
@RestController
@RequestMapping("/api/selected-readings")
public class SelectedReadingController {
    private final SelectedReadingService selectedReadingService;

    public SelectedReadingController(SelectedReadingService selectedReadingService) {
        this.selectedReadingService = selectedReadingService;
    }

    @Operation(summary = "查询精选读物", description = "返回精选读物列表；传入用户 ID 时会标记每篇读物是否已收藏。")
    @GetMapping
    public ApiResult<List<SelectedReadingItem>> getSelectedReadings(
            @Parameter(description = "用户 ID；不传则不返回收藏状态")
            @RequestParam(required = false) Long userId) {
        return ApiResult.success(selectedReadingService.getSelectedReadings(userId));
    }

    @Operation(summary = "收藏精选读物", description = "收藏一篇精选读物。请求体字段：userId 用户 ID，selectedReadingId 精选读物 ID。")
    @PostMapping("/favorites")
    public ApiResult<UserSelectedReadingFavorite> addFavorite(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long selectedReadingId = Long.valueOf(body.get("selectedReadingId").toString());
            UserSelectedReadingFavorite favorite = selectedReadingService.addFavorite(userId, selectedReadingId);
            return ApiResult.success(favorite == null ? "已收藏" : "收藏成功", favorite);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "取消收藏精选读物", description = "根据用户 ID 和精选读物 ID 取消收藏。")
    @DeleteMapping("/favorites/{selectedReadingId}")
    public ApiResult<Void> removeFavorite(
            @Parameter(description = "精选读物 ID", required = true)
            @PathVariable Long selectedReadingId,
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        try {
            selectedReadingService.removeFavorite(userId, selectedReadingId);
            return ApiResult.success("已取消收藏", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询精选读物收藏", description = "查询指定用户收藏的精选读物记录。")
    @GetMapping("/favorites")
    public ApiResult<List<UserSelectedReadingFavorite>> getFavorites(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        return ApiResult.success(selectedReadingService.getFavorites(userId));
    }
}
