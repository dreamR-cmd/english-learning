 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.entity.Word;
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
     public ApiResult<List<Word>> getWords(@PathVariable String moduleCode) {
         return ApiResult.success(practiceService.getWordsByModule(moduleCode));
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
