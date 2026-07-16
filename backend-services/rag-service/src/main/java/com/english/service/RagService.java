package com.english.service;

import com.english.dto.RagAnswerResponse;
import com.english.dto.RagDocumentRequest;
import com.english.dto.RagDocumentResponse;
import com.english.dto.RagSearchItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagService {
    RagDocumentResponse addDocument(RagDocumentRequest request);
    RagDocumentResponse addDocumentFile(MultipartFile file, String title, String source);
    List<RagDocumentResponse> listDocuments();
    List<RagSearchItem> search(String question, Integer topK);
    RagAnswerResponse ask(String question, Integer topK);

    void askStream(String question, Integer topK, String sessionId, RagStreamHandler handler);

    interface RagStreamHandler {
        void onReferences(List<RagSearchItem> references);
        void onToken(String token);
        void onComplete();
        void onError(Throwable error);
    }
}
