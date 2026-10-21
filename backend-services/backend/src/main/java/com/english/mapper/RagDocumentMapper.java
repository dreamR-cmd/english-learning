package com.english.mapper;

import com.english.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagDocumentMapper extends JpaRepository<RagDocument, Long> {
    List<RagDocument> findAllByOrderByCreatedAtDesc();
}
