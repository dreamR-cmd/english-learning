 package com.english.service;
 
 import com.english.entity.ExamModule;
 import java.util.List;
 
 public interface ModuleService {
     List<ExamModule> getAllModules();
     ExamModule getModuleByCode(String code);
 }
