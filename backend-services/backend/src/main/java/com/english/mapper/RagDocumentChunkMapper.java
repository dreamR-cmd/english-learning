package com.english.mapper;

import com.english.entity.RagDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RagDocumentChunkMapper extends JpaRepository<RagDocumentChunk, Long> {
    List<RagDocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Query("""
            select c from RagDocumentChunk c
            where lower(c.title) like lower(concat('%', :term, '%'))
               or lower(c.content) like lower(concat('%', :term, '%'))
            order by c.createdAt desc
            """)
    List<RagDocumentChunk> searchByTerm(@Param("term") String term);

    @Transactional
    void deleteByDocumentId(Long documentId);
}
