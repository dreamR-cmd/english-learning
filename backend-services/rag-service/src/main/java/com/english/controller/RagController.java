package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.RagAnswerResponse;
import com.english.dto.RagDocumentRequest;
import com.english.dto.RagDocumentResponse;
import com.english.dto.RagQueryRequest;
import com.english.dto.RagSearchItem;
import com.english.service.RagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "RAG Knowledge Base")
@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/documents")
    public ApiResult<RagDocumentResponse> addDocument(@RequestBody RagDocumentRequest request) {
        return ApiResult.success(ragService.addDocument(request));
    }

    @GetMapping("/documents")
    public ApiResult<List<RagDocumentResponse>> listDocuments() {
        return ApiResult.success(ragService.listDocuments());
    }

    @PostMapping("/search")
    public ApiResult<List<RagSearchItem>> search(@RequestBody RagQueryRequest request) {
        return ApiResult.success(ragService.search(request.getQuestion(), request.getTopK()));
    }

    @PostMapping("/ask")
    public ApiResult<RagAnswerResponse> ask(@RequestBody RagQueryRequest request) {
        return ApiResult.success(ragService.ask(request.getQuestion(), request.getTopK()));
    }
}
