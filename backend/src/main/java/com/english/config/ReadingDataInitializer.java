package com.english.config;

import com.english.entity.ExamModule;
import com.english.entity.Reading;
import com.english.mapper.ExamModuleMapper;
import com.english.mapper.ReadingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReadingDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ReadingDataInitializer.class);

    /**
     * 每个等级固定生成 20 篇阅读文章。
     *
     * 当前前端是一篇文章展示多道题，因此这里不直接生成 100 篇单题文章，
     * 而是使用“20 篇文章 x 每篇 5 道题 = 每个等级 100 道阅读理解题”的结构。
     */
    private static final int TARGET_PASSAGES_PER_MODULE = 20;
    private static final int QUESTIONS_PER_PASSAGE = 5;

    private final ReadingMapper readingMapper;
    private final ExamModuleMapper moduleMapper;

    public ReadingDataInitializer(ReadingMapper readingMapper, ExamModuleMapper moduleMapper) {
        this.readingMapper = readingMapper;
        this.moduleMapper = moduleMapper;
    }

    @Override
    public void run(String... args) {
        /*
         * 这里的数据是原创练习题，不抓取真题原文。
         * 原因：
         * 1. 真题/教材文章通常有版权，直接写入项目数据库有侵权风险。
         * 2. 原创题更适合演示系统功能，也可以后续替换为你自己拥有版权的数据。
         *
         * Map 的 key 必须和 exam_modules 表里的 code 一致，例如 cet4、cet6。
         */
        Map<String, List<Topic>> topicsByModule = Map.of(
                "cet4", List.of(
                        new Topic("Campus Study Plans", "college students", "weekly planning", "study efficiency", "balanced routines"),
                        new Topic("Library Learning Spaces", "university libraries", "quiet reading areas", "academic focus", "shared resources"),
                        new Topic("Part-time Jobs", "student workers", "time pressure", "practical experience", "financial responsibility"),
                        new Topic("Online Courses", "digital lessons", "flexible schedules", "self discipline", "teacher feedback"),
                        new Topic("Healthy Sleep", "young adults", "regular sleep", "memory improvement", "late-night screen use")
                ),
                "cet6", List.of(
                        new Topic("Digital Reading Habits", "modern readers", "screen-based reading", "deep attention", "information overload"),
                        new Topic("Urban Commuting", "city residents", "public transportation", "carbon reduction", "travel convenience"),
                        new Topic("Workplace Communication", "new employees", "clear messages", "team cooperation", "professional trust"),
                        new Topic("Food Waste", "households", "meal planning", "resource conservation", "consumer awareness"),
                        new Topic("Lifelong Learning", "adult learners", "continuous study", "career adaptation", "personal growth")
                ),
                "kaoyan", List.of(
                        new Topic("Critical Thinking", "academic readers", "evidence evaluation", "independent judgment", "hidden assumptions"),
                        new Topic("Scientific Publishing", "research communities", "peer review", "knowledge quality", "publication pressure"),
                        new Topic("Education Equity", "policy makers", "resource distribution", "social mobility", "regional differences"),
                        new Topic("Technology and Labor", "workers", "automation", "skill renewal", "job transformation"),
                        new Topic("Cultural Memory", "local communities", "historical records", "identity formation", "modern change")
                ),
                "toefl", List.of(
                        new Topic("Campus Sustainability", "universities", "energy-saving buildings", "environmental responsibility", "student participation"),
                        new Topic("Marine Ecosystems", "coastal scientists", "habitat protection", "species diversity", "human activity"),
                        new Topic("Museum Education", "museum visitors", "interactive exhibits", "informal learning", "public engagement"),
                        new Topic("Volcanic Landscapes", "geologists", "rock formation", "earth history", "natural hazards"),
                        new Topic("Animal Communication", "biologists", "signal patterns", "group survival", "behavioral adaptation")
                ),
                "ielts", List.of(
                        new Topic("Urban Green Spaces", "city planners", "public parks", "mental health", "housing demand"),
                        new Topic("Renewable Energy", "national governments", "wind and solar power", "long-term investment", "energy security"),
                        new Topic("Tourism Management", "local businesses", "visitor growth", "cultural preservation", "community benefits"),
                        new Topic("Public Health Campaigns", "health agencies", "clear information", "preventive behavior", "public trust"),
                        new Topic("Remote Work", "office employees", "flexible workplaces", "productivity", "social isolation")
                ),
                "gre", List.of(
                        new Topic("Scientific Models", "researchers", "simplified systems", "predictive accuracy", "model limitations"),
                        new Topic("Moral Decision Making", "philosophers", "ethical reasoning", "conflicting duties", "social consequences"),
                        new Topic("Economic Signals", "market analysts", "price changes", "resource allocation", "consumer expectations"),
                        new Topic("Linguistic Change", "language scholars", "usage patterns", "cultural contact", "historical evidence"),
                        new Topic("Archaeological Inference", "archaeologists", "material remains", "past societies", "interpretive caution")
                )
        );

        for (Map.Entry<String, List<Topic>> entry : topicsByModule.entrySet()) {
            ensureModuleReadings(entry.getKey(), entry.getValue());
        }
    }

    private void ensureModuleReadings(String moduleCode, List<Topic> topics) {
        // 先找到等级考试模块。模块不存在时直接跳过，避免外键 module_id 写入失败。
        ExamModule module = moduleMapper.findByCode(moduleCode);
        if (module == null) {
            log.warn("阅读理解题库初始化跳过：模块不存在，moduleCode={}", moduleCode);
            return;
        }

        /*
         * 幂等写入：
         * - 只检查本初始化器生成的固定标题。
         * - 如果标题已存在，说明之前已经导入过，不重复插入。
         * - 不删除已有人工数据，避免覆盖你后续手动维护的阅读题。
         */
        List<String> existingTitles = readingMapper.findByModuleCode(moduleCode)
                .stream()
                .map(Reading::getTitle)
                .toList();

        int inserted = 0;
        for (int index = 0; index < TARGET_PASSAGES_PER_MODULE; index++) {
            Topic topic = topics.get(index % topics.size());
            int variant = index + 1;
            String title = buildTitle(moduleCode, topic, variant);
            if (existingTitles.contains(title)) {
                continue;
            }

            Reading reading = new Reading();
            reading.setModule(module);
            reading.setFeatured(index < 5);
            reading.setTitle(title);
            reading.setContent(buildContent(topic, variant));
            reading.setQuestions(buildQuestions(topic, variant));
            readingMapper.save(reading);
            inserted++;
        }

        log.info("阅读理解题库初始化完成：moduleCode={}，新增文章={}，每篇题数={}，新增题目={}",
                moduleCode, inserted, QUESTIONS_PER_PASSAGE, inserted * QUESTIONS_PER_PASSAGE);
    }

    private String buildTitle(String moduleCode, Topic topic, int variant) {
        // 固定标题用于判断是否已经导入过，避免应用每次启动都重复插入同一批题。
        return "Auto Reading Bank " + moduleCode.toUpperCase() + " " + variant + " - " + topic.title();
    }

    private String buildContent(Topic topic, int variant) {
        /*
         * 生成一篇短阅读文章。
         * topic 中保存主题、对象、关注点、好处、挑战；
         * variant 用来让同一主题下的文章标题和结尾不同，便于形成多篇练习文章。
         */
        return topic.title() + " has become a useful subject for English reading practice. "
                + "In this passage, " + topic.actor() + " pay attention to " + topic.focus()
                + " because it can influence " + topic.benefit() + ". "
                + "The situation is not simple. People often welcome the advantages, but they also have to consider "
                + topic.challenge() + ". "
                + "For example, a small change in daily decisions may produce a larger effect over time. "
                + "When " + topic.actor() + " compare different choices, they usually discover that success depends on planning, patience, and reliable information. "
                + "The passage suggests that " + topic.focus() + " should not be treated as a short-term fashion. "
                + "Instead, it is a practical habit that can support " + topic.benefit()
                + " if people understand both its promise and its limits. "
                + "This is why the topic continues to appear in discussions of study, work, and public life. "
                + "Variant " + variant + " adds a slightly different example but keeps the same central idea for practice.";
    }

    private String buildQuestions(Topic topic, int variant) {
        /*
         * questions 字段在数据库中是 TEXT，前端按 JSON 数组解析。
         * 每道题格式：
         * {
         *   "q": "题干",
         *   "options": ["A选项", "B选项", "C选项", "D选项"],
         *   "answer": 0
         * }
         *
         * answer 使用数组下标，0 表示第一个选项，也就是 A。
         */
        return "["
                + question("What is the main topic of the passage?",
                topic.title(), "A personal travel story", "A grammar rule", "A weather report", 0) + ","
                + question("Why do people pay attention to " + topic.focus() + "?",
                "Because it can influence " + topic.benefit(), "Because it is always easy", "Because it removes all problems", "Because it has no limits", 0) + ","
                + question("What challenge does the passage mention?",
                topic.challenge(), "The absence of any choice", "A lack of language", "The end of public life", 0) + ","
                + question("What does the passage suggest is needed for success?",
                "Planning, patience, and reliable information", "Luck only", "Ignoring evidence", "Avoiding practice", 0) + ","
                + question("Which statement best matches the author's view?",
                topic.focus() + " is useful but should be understood carefully",
                topic.focus() + " is useless in modern life",
                topic.focus() + " should be followed without thought",
                "Only experts can understand the topic", 0)
                + "]";
    }

    private String question(String q, String a, String b, String c, String d, int answer) {
        // 手动拼接 JSON 前必须转义反斜杠和双引号，避免文章/选项内容破坏 JSON 结构。
        return "{\"q\":\"" + escape(q) + "\",\"options\":[\""
                + escape(a) + "\",\"" + escape(b) + "\",\"" + escape(c) + "\",\"" + escape(d)
                + "\"],\"answer\":" + answer + "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 题目模板数据。
     *
     * @param title     文章主题标题
     * @param actor     文章中的主要对象
     * @param focus     文章关注点
     * @param benefit   文章强调的积极影响
     * @param challenge 文章提到的挑战
     */
    private record Topic(String title, String actor, String focus, String benefit, String challenge) {}
}
