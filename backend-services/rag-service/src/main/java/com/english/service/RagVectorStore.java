package com.english.service;

import com.english.dto.RagSearchItem;

import java.util.List;

public interface RagVectorStore {
    void ensureIndex();
    void upsert(RagVectorRecord record);
    List<RagSearchItem> search(float[] queryVector, int topK);
    void delete(String vectorId);
}
