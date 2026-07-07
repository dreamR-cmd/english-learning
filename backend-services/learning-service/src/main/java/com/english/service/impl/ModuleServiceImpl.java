 package com.english.service.impl;
 
 import com.english.entity.ExamModule;
 import com.english.mapper.ExamModuleMapper;
 import com.english.service.ModuleService;
 import org.springframework.stereotype.Service;
 import java.util.List;
 
 @Service
 public class ModuleServiceImpl implements ModuleService {
     private final ExamModuleMapper moduleMapper;
 
     public ModuleServiceImpl(ExamModuleMapper moduleMapper) {
         this.moduleMapper = moduleMapper;
     }
 
    @Override
    public List<ExamModule> getAllModules() {
        return moduleMapper.findAllByOrderBySortOrderAscIdAsc();
    }
 
     @Override
     public ExamModule getModuleByCode(String code) {
         ExamModule module = moduleMapper.findByCode(code);
         if (module == null) throw new RuntimeException("模块不存在: " + code);
         return module;
     }
 }
