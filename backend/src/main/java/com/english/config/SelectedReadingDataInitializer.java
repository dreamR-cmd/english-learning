package com.english.config;

import com.english.entity.SelectedReading;
import com.english.mapper.SelectedReadingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SelectedReadingDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SelectedReadingDataInitializer.class);
    private static final int MAX_CONTENT_LENGTH = 180_000;

    private final SelectedReadingMapper selectedReadingMapper;
    private final RestClient restClient;

    public SelectedReadingDataInitializer(SelectedReadingMapper selectedReadingMapper) {
        this.selectedReadingMapper = selectedReadingMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "english-learning-app/1.0")
                .build();
    }

    @Override
    public void run(String... args) {
        List<SeedReading> readings = List.of(
                new SeedReading("Alice's Adventures in Wonderland", "Project Gutenberg", "B1", "公版经典", "适合兴趣阅读",
                        "经典童话，故事性强，适合从短章节开始培养英文原著阅读习惯。",
                        "https://www.gutenberg.org/files/11/11-0.txt"),
                new SeedReading("The Adventures of Sherlock Holmes", "Project Gutenberg", "B2", "推理故事", "适合细节阅读",
                        "短篇推理故事合集，适合训练线索追踪、人物动机和上下文推断。",
                        "https://www.gutenberg.org/files/1661/1661-0.txt"),
                new SeedReading("Pride and Prejudice", "Project Gutenberg", "B2-C1", "文学经典", "适合文学阅读",
                        "语言较正式，适合提升长句理解、人物关系分析和文学表达感知。",
                        "https://www.gutenberg.org/files/1342/1342-0.txt"),
                new SeedReading("The Time Machine", "Project Gutenberg", "B2", "科幻经典", "适合主题阅读",
                        "篇幅相对可控，适合讨论科技、未来和社会变化。",
                        "https://www.gutenberg.org/files/35/35-0.txt"),
                new SeedReading("The Secret Garden", "Project Gutenberg", "B1-B2", "成长故事", "适合章节阅读",
                        "故事温和清晰，适合训练环境描写、人物变化和情节发展理解。",
                        "https://www.gutenberg.org/files/113/113-0.txt"),
                new SeedReading("The Wonderful Wizard of Oz", "Project Gutenberg", "B1", "奇幻经典", "适合轻松泛读",
                        "语言相对直观，情节推进快，适合作为英文原著入门读物。",
                        "https://www.gutenberg.org/files/55/55-0.txt")
        );

        int inserted = 0;
        int fetched = 0;
        for (int i = 0; i < readings.size(); i++) {
            SeedReading seed = readings.get(i);
            SelectedReading reading = selectedReadingMapper.findByTitle(seed.title()).orElse(null);
            if (reading == null) {
                reading = new SelectedReading();
                reading.setTitle(seed.title());
                inserted++;
            }

            reading.setSource(seed.source());
            reading.setLevel(seed.level());
            reading.setType(seed.type());
            reading.setSuggestedFor(seed.suggestedFor());
            reading.setDescription(seed.description());
            reading.setUrl(seed.textUrl());
            reading.setSortOrder(i + 1);

            String content = fetchAndClean(seed.textUrl());
            if (!content.isBlank()) {
                reading.setContent(content);
                fetched++;
            } else if (reading.getContent() == null || reading.getContent().isBlank()) {
                reading.setContent("This public-domain reading could not be downloaded. Please restart the backend when the network is available.");
            }

            selectedReadingMapper.save(reading);
        }

        log.info("精选读物初始化完成：新增读物={}，成功抓取正文={}，总读物={}", inserted, fetched, readings.size());
    }

    private String fetchAndClean(String url) {
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                return "";
            }

            String raw = new String(body, StandardCharsets.UTF_8);
            return trimToReadableLength(cleanGutenbergText(raw));
        } catch (Exception error) {
            log.warn("精选读物抓取失败：url={}，error={}", url, error.getMessage());
            return "";
        }
    }

    private String cleanGutenbergText(String raw) {
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        normalized = removeHeader(normalized);
        normalized = removeFooter(normalized);
        normalized = Pattern.compile("(?m)^\\s*\\[Illustration[^\\n]*\\]\\s*$").matcher(normalized).replaceAll("");
        normalized = Pattern.compile("[ \\t]+").matcher(normalized).replaceAll(" ");
        normalized = Pattern.compile("\\n{3,}").matcher(normalized).replaceAll("\n\n");
        return normalized.trim();
    }

    private String removeHeader(String text) {
        int marker = text.indexOf("*** START OF THE PROJECT GUTENBERG EBOOK");
        if (marker < 0) {
            marker = text.indexOf("*** START OF THIS PROJECT GUTENBERG EBOOK");
        }
        if (marker < 0) {
            return text;
        }
        int lineEnd = text.indexOf('\n', marker);
        return lineEnd >= 0 ? text.substring(lineEnd + 1) : text;
    }

    private String removeFooter(String text) {
        int marker = text.indexOf("*** END OF THE PROJECT GUTENBERG EBOOK");
        if (marker < 0) {
            marker = text.indexOf("*** END OF THIS PROJECT GUTENBERG EBOOK");
        }
        return marker >= 0 ? text.substring(0, marker) : text;
    }

    private String trimToReadableLength(String content) {
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }

        int cut = content.lastIndexOf("\n\n", MAX_CONTENT_LENGTH);
        if (cut < MAX_CONTENT_LENGTH / 2) {
            cut = MAX_CONTENT_LENGTH;
        }
        return content.substring(0, cut).trim()
                + "\n\n[Content shortened for in-app reading. The full public-domain source is recorded in the database source URL.]";
    }

    private record SeedReading(
            String title,
            String source,
            String level,
            String type,
            String suggestedFor,
            String description,
            String textUrl
    ) {}
}
