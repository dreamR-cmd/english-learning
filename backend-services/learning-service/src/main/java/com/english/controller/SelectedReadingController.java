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
    private static final String USER_ID_HEADER = "X-User-Id";

    private final SelectedReadingService selectedReadingService;

    public SelectedReadingController(SelectedReadingService selectedReadingService) {
        this.selectedReadingService = selectedReadingService;
    }

    @Operation(summary = "查询精选读物", description = "返回精选读物列表，并标记当前登录用户是否已收藏。")
    @GetMapping
    public ApiResult<List<SelectedReadingItem>> getSelectedReadings(@RequestHeader(USER_ID_HEADER) Long userId) {
        return ApiResult.success(selectedReadingService.getSelectedReadings(userId));
    }

    @Operation(summary = "收藏精选读物", description = "收藏一篇精选读物。用户身份来自 Gateway 注入的 X-User-Id。")
    @PostMapping("/favorites")
    public ApiResult<UserSelectedReadingFavorite> addFavorite(@RequestHeader(USER_ID_HEADER) Long userId,
                                                             @RequestBody Map<String, Object> body) {
        try {
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
            @RequestHeader(USER_ID_HEADER) Long userId) {
        try {
            selectedReadingService.removeFavorite(userId, selectedReadingId);
            return ApiResult.success("已取消收藏", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询精选读物收藏", description = "查询指定用户收藏的精选读物记录。")
    @GetMapping("/favorites")
    public ApiResult<List<UserSelectedReadingFavorite>> getFavorites(@RequestHeader(USER_ID_HEADER) Long userId) {
        return ApiResult.success(selectedReadingService.getFavorites(userId));
    }
}
