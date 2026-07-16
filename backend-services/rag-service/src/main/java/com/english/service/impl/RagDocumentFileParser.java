package com.english.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class RagDocumentFileParser {
    private final long maxUploadBytes;

    public RagDocumentFileParser(@Value("${rag.document.max-upload-bytes:20971520}") long maxUploadBytes) {
        this.maxUploadBytes = Math.max(maxUploadBytes, 1L);
    }

    public ParsedDocument parse(MultipartFile file) {
        validateFile(file);
        String originalFilename = cleanFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);

        String content;
        try {
            content = switch (extension) {
                case "pdf" -> extractPdf(file);
                case "docx" -> extractDocx(file);
                case "doc" -> extractDoc(file);
                default -> throw new IllegalArgumentException("Unsupported document type: " + extension);
            };
        } catch (IOException error) {
            throw new IllegalArgumentException("Failed to parse uploaded document: " + originalFilename, error);
        }

        String normalized = normalize(content);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Uploaded document does not contain extractable text");
        }
        return new ParsedDocument(titleFromFilename(originalFilename), originalFilename, normalized);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("Uploaded file exceeds max size: " + maxUploadBytes + " bytes");
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted PDF files are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replace('\u0000', ' ')
                .replace('\u000B', '\n')
                .replace('\f', '\n')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "uploaded-document";
        }
        String normalized = filename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String titleFromFilename(String filename) {
        int dot = filename.lastIndexOf('.');
        String title = dot > 0 ? filename.substring(0, dot) : filename;
        return title.isBlank() ? "Untitled Document" : title;
    }

    public record ParsedDocument(String title, String source, String content) {
    }
}
