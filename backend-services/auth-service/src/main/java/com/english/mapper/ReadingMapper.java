 package com.english.mapper;
 
 import com.english.entity.Reading;
 import org.springframework.data.jpa.repository.JpaRepository;
 import java.util.List;
 
 public interface ReadingMapper extends JpaRepository<Reading, Long> {
     List<Reading> findByModuleCode(String moduleCode);
     List<Reading> findByModuleIdAndFeaturedTrue(Long moduleId);
 }
