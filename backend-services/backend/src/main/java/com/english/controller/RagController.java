package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.RagAnswerResponse;
import com.english.dto.RagDocumentRequest;
import com.english.dto.RagDocumentResponse;
import com.english.dto.RagQueryRequest;
import com.english.dto.RagSearchItem;
import com.english.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "RAG 知识库", description = "维护检索增强生成知识库文档，并基于知识库进行检索和问答")
@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @Operation(summary = "新增 RAG 文档", description = "把标题、正文和来源写入当前 RAG 知识库，后续可用于检索和问答。")
    @PostMapping("/documents")
    public ApiResult<RagDocumentResponse> addDocument(@RequestBody RagDocumentRequest request) {
        return ApiResult.success(ragService.addDocument(request));
    }

    @Operation(summary = "Upload RAG document", description = "Upload a PDF, DOCX, or DOC file, extract text, and add it to the RAG knowledge base.")
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<RagDocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "title", required = false) String title,
                                                         @RequestParam(value = "source", required = false) String source) {
        return ApiResult.success(ragService.addDocumentFile(file, title, source));
    }

    @Operation(summary = "查询 RAG 文档列表", description = "返回当前 RAG 知识库中已经写入的全部文档。")
    @GetMapping("/documents")
    public ApiResult<List<RagDocumentResponse>> listDocuments() {
        return ApiResult.success(ragService.listDocuments());
    }

    @Operation(summary = "检索 RAG 文档", description = "根据问题从知识库中检索最相关的文档片段，topK 可控制返回数量。")
    @PostMapping("/search")
    public ApiResult<List<RagSearchItem>> search(@RequestBody RagQueryRequest request) {
        return ApiResult.success(ragService.search(request.getQuestion(), request.getTopK()));
    }

    @Operation(summary = "RAG 问答", description = "根据问题先检索知识库，再返回当前 RAG 服务生成的回答和引用结果。")
    @PostMapping("/ask")
    public ApiResult<RagAnswerResponse> ask(@RequestBody RagQueryRequest request) {
        return ApiResult.success(ragService.ask(request.getQuestion(), request.getTopK()));
    }

    @Operation(summary = "RAG 流式问答", description = "根据问题检索知识库，并以 Server-Sent Events 流式返回回答 token 和引用资料。")
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody RagQueryRequest request, HttpServletRequest servletRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = servletRequest.getHeader("X-User-Id");
        CompletableFuture.runAsync(() -> ragService.askStream(request.getQuestion(), request.getTopK(), sessionId, new RagService.RagStreamHandler() {
            @Override
            public void onReferences(List<RagSearchItem> references) {
                sendEvent(emitter, "references", references);
            }

            @Override
            public void onToken(String token) {
                sendEvent(emitter, "token", token);
            }

            @Override
            public void onComplete() {
                sendEvent(emitter, "done", Map.of("done", true));
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                sendEvent(emitter, "error", Map.of("message", errorMessage(error)));
                emitter.complete();
            }
        }));
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException error) {
            emitter.completeWithError(error);
        }
    }

    private String errorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "RAG 流式问答失败" : message;
    }
}
