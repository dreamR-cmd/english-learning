package com.english.mapper;

import com.english.entity.ExamModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamModuleMapper extends JpaRepository<ExamModule, Long> {
    ExamModule findByCode(String code);
    List<ExamModule> findAllByOrderBySortOrderAscIdAsc();
}
