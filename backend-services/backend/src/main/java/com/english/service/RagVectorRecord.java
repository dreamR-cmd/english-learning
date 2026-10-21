package com.english.service;

public record RagVectorRecord(
        String vectorId,
        Long documentId,
        Long chunkId,
        String title,
        String source,
        String content,
        float[] embedding
) {}
