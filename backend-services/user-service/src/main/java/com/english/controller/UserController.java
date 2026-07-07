package com.english.controller;

import com.english.dto.ApiResult;
import com.english.entity.User;
import com.english.entity.UserFavorite;
import com.english.entity.UserWordProgress;
import com.english.entity.WrongRecord;
import com.english.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户中心接口", description = "用户资料、错题本、阅读收藏和单词掌握进度接口")
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "更新用户资料", description = "更新用户昵称和每日单词目标。请求体字段：userId 用户 ID，nickname 昵称，dailyWordTarget 每日单词目标。")
    @PutMapping("/profile")
    public ApiResult<User> updateProfile(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String nickname = (String) body.get("nickname");
            Integer dailyWordTarget = body.get("dailyWordTarget") == null
                    ? null
                    : Integer.valueOf(body.get("dailyWordTarget").toString());
            User user = userService.updateProfile(userId, nickname, dailyWordTarget);
            user.setPassword(null);
            return ApiResult.success("更新成功", user);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "提交错题记录", description = "保存用户在单词、阅读或听力练习中产生的错题记录。")
    @PostMapping("/wrong-records")
    public ApiResult<WrongRecord> submitWrongRecord(@RequestBody WrongRecord record) {
        try {
            WrongRecord saved = userService.saveWrongRecord(record);
            return ApiResult.success("已记录错题", saved);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询错题记录", description = "查询指定用户的错题本列表。")
    @GetMapping("/wrong-records")
    public ApiResult<List<WrongRecord>> getWrongRecords(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        return ApiResult.success(userService.getWrongRecords(userId));
    }

    @Operation(summary = "删除错题记录", description = "从指定用户的错题本中删除一条错题记录。")
    @DeleteMapping("/wrong-records/{wrongRecordId}")
    public ApiResult<Void> removeWrongRecord(
            @Parameter(description = "错题记录 ID", required = true)
            @PathVariable Long wrongRecordId,
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        try {
            userService.removeWrongRecord(userId, wrongRecordId);
            return ApiResult.success("已移出错题本", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "收藏阅读文章", description = "收藏阅读理解文章。请求体字段：userId 用户 ID，readingId 阅读文章 ID。")
    @PostMapping("/favorites")
    public ApiResult<UserFavorite> addFavorite(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long readingId = Long.valueOf(body.get("readingId").toString());
            UserFavorite fav = userService.addFavorite(userId, readingId);
            if (fav == null) {
                return ApiResult.success("已收藏", null);
            }
            return ApiResult.success("收藏成功", fav);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "取消收藏阅读文章", description = "根据用户 ID 和阅读文章 ID 取消收藏。")
    @DeleteMapping("/favorites/{readingId}")
    public ApiResult<Void> removeFavorite(
            @Parameter(description = "阅读文章 ID", required = true)
            @PathVariable Long readingId,
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        try {
            userService.removeFavorite(userId, readingId);
            return ApiResult.success("已取消收藏", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询阅读收藏", description = "查询指定用户收藏的阅读理解文章记录。")
    @GetMapping("/favorites")
    public ApiResult<List<UserFavorite>> getFavorites(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        return ApiResult.success(userService.getFavorites(userId));
    }

    @Operation(summary = "检查阅读是否已收藏", description = "判断指定用户是否已经收藏某篇阅读文章。")
    @GetMapping("/favorites/check")
    public ApiResult<Boolean> checkFavorite(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "阅读文章 ID", required = true)
            @RequestParam Long readingId) {
        return ApiResult.success(userService.isFavorite(userId, readingId));
    }

    @Operation(summary = "标记单词认识", description = "增加用户对某个单词的认识次数。请求体字段：userId 用户 ID，wordId 单词 ID。")
    @PostMapping("/word-progress/known")
    public ApiResult<UserWordProgress> markWordKnown(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long wordId = Long.valueOf(body.get("wordId").toString());
            UserWordProgress progress = userService.markWordKnown(userId, wordId);
            return ApiResult.success("已记录认识次数", progress);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "重置单词掌握进度", description = "重置用户对某个单词的掌握次数。请求体字段：userId 用户 ID，wordId 单词 ID。")
    @PostMapping("/word-progress/reset")
    public ApiResult<UserWordProgress> resetWordProgress(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long wordId = Long.valueOf(body.get("wordId").toString());
            UserWordProgress progress = userService.resetWordProgress(userId, wordId);
            return ApiResult.success("已重置单词掌握次数", progress);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询复习单词", description = "查询指定用户需要复习的单词进度记录。")
    @GetMapping("/word-progress/review")
    public ApiResult<List<UserWordProgress>> getReviewWords(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId) {
        return ApiResult.success(userService.getReviewWords(userId));
    }
}
