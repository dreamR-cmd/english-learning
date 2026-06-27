 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.entity.ExamModule;
 import com.english.service.ModuleService;
 import org.springframework.web.bind.annotation.*;
 import java.util.List;
 
 @RestController
 @RequestMapping("/api/modules")
 public class ModuleController {
     private final ModuleService moduleService;
 
     public ModuleController(ModuleService moduleService) {
         this.moduleService = moduleService;
     }
 
     @GetMapping
     public ApiResult<List<ExamModule>> getAllModules() {
         return ApiResult.success(moduleService.getAllModules());
     }
 
     @GetMapping("/{code}")
     public ApiResult<ExamModule> getModuleByCode(@PathVariable String code) {
         try {
             return ApiResult.success(moduleService.getModuleByCode(code));
         } catch (RuntimeException e) {
             return ApiResult.error(404, e.getMessage());
         }
     }
 }
