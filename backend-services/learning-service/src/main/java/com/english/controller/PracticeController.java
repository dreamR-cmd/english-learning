 package com.english.controller;

 import com.english.dto.ApiResult;
 import com.english.dto.WordPracticeItem;
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.service.PracticeService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.Parameter;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.web.bind.annotation.*;

 import java.util.List;

 @Tag(name = "练习接口", description = "单词、阅读、听力练习内容查询接口")
 @RestController
 @RequestMapping("/api/practice")
 public class PracticeController {
     private static final String USER_ID_HEADER = "X-User-Id";

     private final PracticeService practiceService;

     public PracticeController(PracticeService practiceService) {
         this.practiceService = practiceService;
     }

     @Operation(summary = "查询模块单词", description = "根据考试模块编码查询单词练习列表，并附带当前登录用户的掌握次数。")
     @GetMapping("/words/{moduleCode}")
     public ApiResult<List<WordPracticeItem>> getWords(
             @Parameter(description = "模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre", required = true)
             @PathVariable String moduleCode,
             @RequestHeader(USER_ID_HEADER) Long userId) {
         return ApiResult.success(practiceService.getWordsByModule(moduleCode, userId));
     }

     @Operation(summary = "查询每日单词", description = "根据用户每日目标生成或读取当天单词练习列表。")
     @GetMapping("/words/daily")
     public ApiResult<List<WordPracticeItem>> getDailyWords(@RequestHeader(USER_ID_HEADER) Long userId) {
         return ApiResult.success(practiceService.getDailyWords(userId));
     }

     @Operation(summary = "查询模块阅读文章", description = "根据考试模块编码查询阅读理解文章和题目信息。")
     @GetMapping("/readings/{moduleCode}")
     public ApiResult<List<Reading>> getReadings(
             @Parameter(description = "模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre", required = true)
             @PathVariable String moduleCode) {
         return ApiResult.success(practiceService.getReadingsByModule(moduleCode));
     }

     @Operation(summary = "查询模块听力材料", description = "根据考试模块编码查询听力材料、音频地址、原文和题目信息。")
     @GetMapping("/listenings/{moduleCode}")
     public ApiResult<List<Listening>> getListenings(
             @Parameter(description = "模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre", required = true)
             @PathVariable String moduleCode) {
         return ApiResult.success(practiceService.getListeningsByModule(moduleCode));
     }
 }
