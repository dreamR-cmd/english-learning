package com.english.controller;

import com.english.dto.ApiResult;
import com.english.entity.User;
import com.english.entity.UserFavorite;
import com.english.entity.WrongRecord;
import com.english.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    public ApiResult<User> updateProfile(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String nickname = (String) body.get("nickname");
            User user = userService.updateProfile(userId, nickname);
            user.setPassword(null);
            return ApiResult.success("更新成功", user);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @PostMapping("/wrong-records")
    public ApiResult<WrongRecord> submitWrongRecord(@RequestBody WrongRecord record) {
        try {
            WrongRecord saved = userService.saveWrongRecord(record);
            return ApiResult.success("已记录错题", saved);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @GetMapping("/wrong-records")
    public ApiResult<List<WrongRecord>> getWrongRecords(@RequestParam Long userId) {
        return ApiResult.success(userService.getWrongRecords(userId));
    }

    @DeleteMapping("/wrong-records/{wrongRecordId}")
    public ApiResult<Void> removeWrongRecord(@PathVariable Long wrongRecordId, @RequestParam Long userId) {
        try {
            userService.removeWrongRecord(userId, wrongRecordId);
            return ApiResult.success("已移出错题本", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

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

    @DeleteMapping("/favorites/{readingId}")
    public ApiResult<Void> removeFavorite(@PathVariable Long readingId, @RequestParam Long userId) {
        try {
            userService.removeFavorite(userId, readingId);
            return ApiResult.success("已取消收藏", null);
        } catch (Exception e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @GetMapping("/favorites")
    public ApiResult<List<UserFavorite>> getFavorites(@RequestParam Long userId) {
        return ApiResult.success(userService.getFavorites(userId));
    }

    @GetMapping("/favorites/check")
    public ApiResult<Boolean> checkFavorite(@RequestParam Long userId, @RequestParam Long readingId) {
        return ApiResult.success(userService.isFavorite(userId, readingId));
    }
}
