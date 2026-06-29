 package com.english.controller;

 import com.english.dto.ApiResult;
 import com.english.dto.WordPracticeItem;
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.service.PracticeService;
 import org.springframework.web.bind.annotation.*;

 import java.util.List;

 @RestController
 @RequestMapping("/api/practice")
 public class PracticeController {
     private final PracticeService practiceService;

     public PracticeController(PracticeService practiceService) {
         this.practiceService = practiceService;
     }

     @GetMapping("/words/{moduleCode}")
     public ApiResult<List<WordPracticeItem>> getWords(@PathVariable String moduleCode,
                                                       @RequestParam(required = false) Long userId) {
         return ApiResult.success(practiceService.getWordsByModule(moduleCode, userId));
     }

     @GetMapping("/words/daily")
     public ApiResult<List<WordPracticeItem>> getDailyWords(@RequestParam Long userId) {
         return ApiResult.success(practiceService.getDailyWords(userId));
     }

     @GetMapping("/readings/{moduleCode}")
     public ApiResult<List<Reading>> getReadings(@PathVariable String moduleCode) {
         return ApiResult.success(practiceService.getReadingsByModule(moduleCode));
     }

     @GetMapping("/listenings/{moduleCode}")
     public ApiResult<List<Listening>> getListenings(@PathVariable String moduleCode) {
         return ApiResult.success(practiceService.getListeningsByModule(moduleCode));
     }
 }
