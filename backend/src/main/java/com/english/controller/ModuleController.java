 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.entity.ExamModule;
 import com.english.service.CountdownService;
 import com.english.service.ModuleService;
 import org.springframework.web.bind.annotation.*;
 import java.util.List;

 @RestController
 @RequestMapping("/api/modules")
 public class ModuleController {
     private final ModuleService moduleService;
     private final CountdownService countdownService;

     public ModuleController(ModuleService moduleService, CountdownService countdownService) {
         this.moduleService = moduleService;
         this.countdownService = countdownService;
     }

     @GetMapping
     public ApiResult<List<ExamModule>> getAllModules() {
         List<ExamModule> modules = moduleService.getAllModules();
         modules.forEach(m -> m.setExamCountdown(countdownService.getCountdown(m.getCode())));
         return ApiResult.success(modules);
     }

     @GetMapping("/{code}")
     public ApiResult<ExamModule> getModuleByCode(@PathVariable String code) {
         try {
             ExamModule module = moduleService.getModuleByCode(code);
             module.setExamCountdown(countdownService.getCountdown(module.getCode()));
             return ApiResult.success(module);
         } catch (RuntimeException e) {
             return ApiResult.error(404, e.getMessage());
         }
     }
 }
