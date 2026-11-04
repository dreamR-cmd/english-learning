package com.english.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class RagDocumentFileParser {
    private static final Logger log = LoggerFactory.getLogger(RagDocumentFileParser.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "bmp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "mkv", "avi", "webm");
    // 视频默认每 10 秒抽 1 帧（fps=1/10），可通过 rag.ocr.video-frame-interval-seconds 调整
    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._\\-\\u4e00-\\u9fff]");

    private final long maxUploadBytes;
    private final RagOcrService ocrService;
    private final int maxOcrPdfPages;
    private final int videoFrameIntervalSeconds;

    public RagDocumentFileParser(
            @Value("${rag.document.max-upload-bytes:20971520}") long maxUploadBytes,
            RagOcrService ocrService,
            @Value("${rag.ocr.max-pdf-pages:50}") int maxOcrPdfPages,
            @Value("${rag.ocr.video-frame-interval-seconds:10}") int videoFrameIntervalSeconds) {
        this.maxUploadBytes = Math.max(maxUploadBytes, 1L);
        this.ocrService = ocrService;
        this.maxOcrPdfPages = Math.max(maxOcrPdfPages, 1);
        this.videoFrameIntervalSeconds = Math.max(videoFrameIntervalSeconds, 1);
    }

    public ParsedDocument parse(MultipartFile file) {
        validateFile(file);
        String originalFilename = cleanFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);

        String content;
        try {
            if (IMAGE_EXTENSIONS.contains(extension)) {
                content = ocrService.recognize(file.getBytes());
            } else if (VIDEO_EXTENSIONS.contains(extension)) {
                content = extractVideo(file, extension);
            } else {
                content = switch (extension) {
                    case "pdf" -> extractPdf(file);
                    case "docx" -> extractDocx(file);
                    case "doc" -> extractDoc(file);
                    default -> throw new IllegalArgumentException("Unsupported document type: " + extension);
                };
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("Failed to parse uploaded document: " + originalFilename, error);
        }

        String normalized = normalize(content);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Uploaded document does not contain extractable text");
        }
        return new ParsedDocument(titleFromFilename(originalFilename), originalFilename, normalized);
    }

    //判断文件是否为空，是否超过最大上传限制
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("Uploaded file exceeds max size: " + maxUploadBytes + " bytes");
        }
    }

    /**
     * PDF 逐页处理：优先抽取文本层；文本层为空的页（扫描件/图片页）渲染成图片交给视觉模型 OCR。
     * OCR 页数受 rag.ocr.max-pdf-pages 限制，超出部分跳过并记日志，避免上传超大扫描件拖垮请求。
     */
    private String extractPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted PDF files are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            int ocrPages = 0;
            StringBuilder content = new StringBuilder();
            for (int page = 0; page < pageCount; page++) {
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);
                String pageText = stripper.getText(document);
                if (pageText == null || pageText.isBlank()) {
                    if (ocrPages >= maxOcrPdfPages) {
                        log.warn("[Parser] skip OCR for pdf page {}: reached max-ocr-pages {}", page + 1, maxOcrPdfPages);
                        continue;
                    }
                    ocrPages++;
                    pageText = ocrService.recognize(toJpegBytes(renderer.renderImageWithDPI(page, 150)));
                }
                if (pageText != null && !pageText.isBlank()) {
                    if (content.length() > 0) {
                        content.append("\n\n");
                    }
                    content.append(pageText.trim());
                }
            }
            return content.toString();
        }
    }

    private byte[] toJpegBytes(java.awt.image.BufferedImage image) throws IOException {
        try (var out = new java.io.ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        }
    }

    /**
     * 视频处理：依赖系统安装的 ffmpeg。先把上传内容落临时文件，
     * 按固定间隔抽取关键帧，逐帧 OCR（字幕/板书/幻灯片文字），把帧文字按时间顺序拼接。
     * ffmpeg 不存在或抽帧失败时抛出明确错误。
     */
    private String extractVideo(MultipartFile file, String extension) {
        Path tempFile = null;
        Path framesDir = null;
        try {
            tempFile = Files.createTempFile("rag-video-", "." + extension);
            file.transferTo(tempFile.toFile());
            framesDir = Files.createTempDirectory("rag-frames-");

            ProcessBuilder ffmpeg = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", tempFile.toString(),
                    "-vf", "fps=1/" + videoFrameIntervalSeconds,
                    "-q:v", "2",
                    framesDir.resolve("frame%04d.jpg").toString());
            ffmpeg.redirectErrorStream(true);
            Process process = ffmpeg.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(10, TimeUnit.MINUTES) || process.exitValue() != 0) {
                process.destroyForcibly();
                log.warn("[Parser] ffmpeg frame extraction failed: {}", output.lines().skip(Math.max(0, countLines(output) - 5)).limit(5).toList());
                throw new IllegalArgumentException("Video processing failed. Is ffmpeg installed?");
            }

            List<String> frameTexts = new ArrayList<>();
            try (Stream<Path> frames = Files.list(framesDir)) {
                List<Path> frameFiles = frames
                        .filter(p -> p.getFileName().toString().endsWith(".jpg"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList();
                for (Path frame : frameFiles) {
                    String text = ocrService.recognize(Files.readAllBytes(frame));
                    if (!text.isBlank() && (frameTexts.isEmpty() || !text.equals(frameTexts.get(frameTexts.size() - 1)))) {
                        frameTexts.add(text);
                    }
                }
            }
            return String.join("\n\n", frameTexts);
        } catch (IOException error) {
            throw new IllegalArgumentException("Failed to process video file", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Video processing interrupted", error);
        } finally {
            cleanupQuietly(tempFile);
            cleanupQuietly(framesDir);
        }
    }

    private int countLines(String text) {
        return (int) text.lines().count();
    }

    private void cleanupQuietly(Path path) {
        if (path == null) {
            return;
        }
        try (Stream<Path> children = Files.isDirectory(path) ? Files.list(path) : Stream.empty()) {
            children.forEach(child -> cleanupQuietly(child));
        } catch (IOException ignored) {
            // 清理失败不影响主流程
        }
        try {
            if (Files.isDirectory(path)) {
                Files.deleteIfExists(path);
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // 清理失败不影响主流程
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
        //替换掉不可见字符，换行符统一为 \n，连续空格和制表符替换为单个空格，连续换行符替换为两个换行符，去掉首尾空白
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

    //文件名清理，去掉路径，只保留文件名
    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "uploaded-document";
        }
        String normalized = filename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    //获取文件扩展名，返回小写形式
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
