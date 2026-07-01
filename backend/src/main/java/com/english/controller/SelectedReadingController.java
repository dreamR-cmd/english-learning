package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.SelectedReadingItem;
import com.english.entity.UserSelectedReadingFavorite;
import com.english.service.SelectedReadingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/selected-readings")
public class SelectedReadingController {
    private final SelectedReadingService selectedReadingService;

    public SelectedReadingController(SelectedReadingService selectedReadingService) {
        this.selectedReadingService = selectedReadingService;
    }

    @GetMapping
    public ApiResult<List<SelectedReadingItem>> getSelectedReadings(@RequestParam(required = false) Long userId) {
        return ApiResult.success(selectedReadingService.getSelectedReadings(userId));
    }

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

    @DeleteMapping("/favorites/{selectedReadingId}")
    public ApiResult<Void> removeFavorite(@PathVariable Long selectedReadingId, @RequestParam Long userId) {
        try {
            selectedReadingService.removeFavorite(userId, selectedReadingId);
            return ApiResult.success("已取消收藏", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @GetMapping("/favorites")
    public ApiResult<List<UserSelectedReadingFavorite>> getFavorites(@RequestParam Long userId) {
        return ApiResult.success(selectedReadingService.getFavorites(userId));
    }
}
