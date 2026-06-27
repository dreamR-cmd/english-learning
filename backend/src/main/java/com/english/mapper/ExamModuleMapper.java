 package com.english.mapper;
 
 import com.english.entity.ExamModule;
 import org.springframework.data.jpa.repository.JpaRepository;
 
 public interface ExamModuleMapper extends JpaRepository<ExamModule, Long> {
     ExamModule findByCode(String code);
 }
