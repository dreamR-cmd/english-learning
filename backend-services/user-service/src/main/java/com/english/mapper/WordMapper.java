 package com.english.mapper;
 
 import com.english.entity.Word;
 import org.springframework.data.jpa.repository.JpaRepository;
 import java.util.List;
 
 public interface WordMapper extends JpaRepository<Word, Long> {
     List<Word> findByModuleId(Long moduleId);
     List<Word> findByModuleCode(String moduleCode);
 }
