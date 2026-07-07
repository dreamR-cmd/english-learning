 package com.english.mapper;
 
 import com.english.entity.Listening;
 import org.springframework.data.jpa.repository.JpaRepository;
 import java.util.List;
 
 public interface ListeningMapper extends JpaRepository<Listening, Long> {
     List<Listening> findByModuleCode(String moduleCode);
 }
