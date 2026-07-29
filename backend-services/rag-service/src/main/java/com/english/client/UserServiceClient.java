package com.english.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/user/word-progress/review")
    Object getReviewWords(@RequestHeader("X-User-Id") Long userId,
                          @RequestHeader("X-Internal-Gateway-Secret") String internalSecret);

    @GetMapping("/api/user/wrong-records")
    Object getWrongRecords(@RequestHeader("X-User-Id") Long userId,
                           @RequestHeader("X-Internal-Gateway-Secret") String internalSecret);

    @PutMapping("/api/user/profile")
    Object updateProfile(@RequestHeader("X-User-Id") Long userId,
                         @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                         @RequestBody Map<String, Object> body);

    @PostMapping("/api/user/wrong-records")
    Object submitWrongRecord(@RequestHeader("X-User-Id") Long userId,
                             @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                             @RequestBody Map<String, Object> body);

    @DeleteMapping("/api/user/wrong-records/{wrongRecordId}")
    Object removeWrongRecord(@RequestHeader("X-User-Id") Long userId,
                             @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                             @PathVariable("wrongRecordId") Long wrongRecordId);

    @PostMapping("/api/user/word-progress/known")
    Object markWordKnown(@RequestHeader("X-User-Id") Long userId,
                         @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                         @RequestBody Map<String, Object> body);

    @PostMapping("/api/user/word-progress/reset")
    Object resetWordProgress(@RequestHeader("X-User-Id") Long userId,
                             @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                             @RequestBody Map<String, Object> body);

    @PostMapping("/api/user/favorites")
    Object addReadingFavorite(@RequestHeader("X-User-Id") Long userId,
                              @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                              @RequestBody Map<String, Object> body);

    @DeleteMapping("/api/user/favorites/{readingId}")
    Object removeReadingFavorite(@RequestHeader("X-User-Id") Long userId,
                                 @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                                 @PathVariable("readingId") Long readingId);
}
