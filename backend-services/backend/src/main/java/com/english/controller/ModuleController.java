 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.entity.ExamModule;
 import com.english.service.CountdownService;
 import com.english.service.ModuleService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.Parameter;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.web.bind.annotation.*;
 import java.util.List;

 @Tag(name = "考试模块接口", description = "CET、考研、雅思等考试模块查询接口")
 @RestController
 @RequestMapping("/api/modules")
 public class ModuleController {
     private final ModuleService moduleService;
     private final CountdownService countdownService;

     public ModuleController(ModuleService moduleService, CountdownService countdownService) {
         this.moduleService = moduleService;
         this.countdownService = countdownService;
     }

     @Operation(summary = "查询全部考试模块", description = "返回所有考试模块，并附带对应考试倒计时信息。")
     @GetMapping
     public ApiResult<List<ExamModule>> getAllModules() {
         List<ExamModule> modules = moduleService.getAllModules();
         modules.forEach(m -> m.setExamCountdown(countdownService.getCountdown(m.getCode())));
         return ApiResult.success(modules);
     }

     @Operation(summary = "按编码查询考试模块", description = "根据模块编码查询单个考试模块，并附带考试倒计时信息。")
     @GetMapping("/{code}")
     public ApiResult<ExamModule> getModuleByCode(
             @Parameter(description = "模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre", required = true)
             @PathVariable String code) {
         try {
             ExamModule module = moduleService.getModuleByCode(code);
             module.setExamCountdown(countdownService.getCountdown(module.getCode()));
             return ApiResult.success(module);
         } catch (RuntimeException e) {
             return ApiResult.error(404, e.getMessage());
         }
     }
 }
