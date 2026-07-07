package com.english.config;

import com.english.entity.ExamModule;
import com.english.mapper.ExamModuleMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures non-exam navigation entries can be managed from the existing module admin page.
 */
@Component
public class NavigationModuleInitializer implements CommandLineRunner {
    private final ExamModuleMapper moduleMapper;

    public NavigationModuleInitializer(ExamModuleMapper moduleMapper) {
        this.moduleMapper = moduleMapper;
    }

    @Override
    public void run(String... args) {
        ensureModule(
                "shop",
                "学习商城",
                "精选课程、真题资料与备考书籍，配合等级考试模块系统学习。",
                "🛒",
                "/shop",
                -20
        );
        ensureModule(
                "selected-readings",
                "精选读物",
                "独立于阅读理解题库，整理适合英语学习者的分级读物和公版经典。",
                "📖",
                "/selected-readings",
                -10
        );
    }

    private void ensureModule(String code, String name, String description, String icon, String routePath, int sortOrder) {
        ExamModule existing = moduleMapper.findByCode(code);
        if (existing != null) {
            if (existing.getRoutePath() == null || existing.getRoutePath().isBlank()) {
                existing.setRoutePath(routePath);
                moduleMapper.save(existing);
            }
            return;
        }

        ExamModule module = new ExamModule();
        module.setCode(code);
        module.setName(name);
        module.setDescription(description);
        module.setIcon(icon);
        module.setRoutePath(routePath);
        module.setSortOrder(sortOrder);
        moduleMapper.save(module);
    }
}
