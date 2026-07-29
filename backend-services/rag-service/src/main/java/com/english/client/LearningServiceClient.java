package com.english.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "learning-service")
public interface LearningServiceClient {

    @GetMapping("/api/practice/words/daily")
    Object getDailyWords(@RequestHeader("X-User-Id") Long userId,
                         @RequestHeader("X-Internal-Gateway-Secret") String internalSecret);

    @GetMapping("/api/practice/words/{moduleCode}")
    Object getModuleWords(@RequestHeader("X-User-Id") Long userId,
                          @RequestHeader("X-Internal-Gateway-Secret") String internalSecret,
                          @PathVariable("moduleCode") String moduleCode);
}
