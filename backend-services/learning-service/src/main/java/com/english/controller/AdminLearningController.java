package com.english.controller;

import com.english.dto.ApiResult;
import com.english.entity.ExamModule;
import com.english.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台学习管理接口", description = "后台模块管理接口")
@RequestMapping("/api/admin/learning")
public class AdminLearningController {
    private final ModuleService moduleService;

    public AdminLearningController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Operation(summary = "后台查询全部模块", description = "查询所有考试模块，用于后台模块管理。")
    @GetMapping("/modules")
    public ApiResult<List<ExamModule>> getModules() {
        return ApiResult.success(moduleService.getAllModules());
    }

    @Operation(summary = "新建模块", description = "后台新增考试模块。")
    @PostMapping("/modules")
    public ApiResult<ExamModule> createModule(@RequestBody ExamModule module) {
        module.setId(null);
        return ApiResult.success(moduleService.saveModule(module));
    }

    @Operation(summary = "更新模块", description = "后台更新考试模块。")
    @PutMapping("/modules/{moduleId}")
    public ApiResult<ExamModule> updateModule(@PathVariable Long moduleId, @RequestBody ExamModule module) {
        module.setId(moduleId);
        return ApiResult.success(moduleService.saveModule(module));
    }

    @Operation(summary = "删除模块", description = "后台删除考试模块。")
    @DeleteMapping("/modules/{moduleId}")
    public ApiResult<Void> deleteModule(@PathVariable Long moduleId) {
        moduleService.deleteModule(moduleId);
        return ApiResult.success("删除成功", null);
    }
}
