package com.english.service;

import com.english.dto.RagAnswerResponse;
import com.english.dto.RagDocumentRequest;
import com.english.dto.RagDocumentResponse;
import com.english.dto.RagSearchItem;

import java.util.List;

public interface RagService {
    RagDocumentResponse addDocument(RagDocumentRequest request);
    List<RagDocumentResponse> listDocuments();
    List<RagSearchItem> search(String question, Integer topK);
    RagAnswerResponse ask(String question, Integer topK);
}
