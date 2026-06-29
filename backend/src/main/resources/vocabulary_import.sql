SET NAMES utf8mb4;
USE english_learning;

-- 手动导入示例：
-- mysql -uroot -p123456 --default-character-set=utf8mb4 english_learning < backend/src/main/resources/vocabulary_import.sql

-- 兜底创建考试模块，避免只导词库时模块不存在。
INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT '大学英语四级', 'cet4', 'CET-4 大学英语四级考试，适合大学生基础英语水平', 'cet4', 1
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'cet4');

INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT '大学英语六级', 'cet6', 'CET-6 大学英语六级考试，适合中高级英语水平', 'cet6', 2
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'cet6');

INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT '托福', 'toefl', 'TOEFL 托福考试，适合出国留学英语水平', 'toefl', 3
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'toefl');

INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT '雅思', 'ielts', 'IELTS 雅思考试，适合英联邦国家留学移民', 'ielts', 4
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'ielts');

INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT '考研英语', 'kaoyan', '全国硕士研究生入学统一考试英语', 'kaoyan', 5
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'kaoyan');

INSERT INTO exam_modules (name, code, description, icon, sort_order)
SELECT 'GRE', 'gre', 'GRE 美国研究生入学考试', 'gre', 6
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'gre');

CREATE TEMPORARY TABLE tmp_vocab_import (
    module_code VARCHAR(32) NOT NULL,
    word VARCHAR(128) NOT NULL,
    phonetic VARCHAR(128),
    meaning TEXT NOT NULL,
    example TEXT
);

INSERT INTO tmp_vocab_import (module_code, word, phonetic, meaning, example) VALUES
-- CET4
('cet4', 'abandon', '/əˈbændən/', 'v. 放弃；遗弃', 'They had to abandon the trip because of the storm.'),
('cet4', 'ability', '/əˈbɪləti/', 'n. 能力；才能', 'Reading improves your ability to think clearly.'),
('cet4', 'absorb', '/əbˈzɔːrb/', 'v. 吸收；理解', 'Plants absorb water through their roots.'),
('cet4', 'academic', '/ˌækəˈdemɪk/', 'adj. 学术的；学习的', 'She achieved excellent academic results.'),
('cet4', 'access', '/ˈækses/', 'n. 机会；通道 v. 使用', 'Students have free access to the digital library.'),
('cet4', 'accompany', '/əˈkʌmpəni/', 'v. 陪伴；伴随', 'A teacher will accompany the students on the tour.'),
('cet4', 'adapt', '/əˈdæpt/', 'v. 适应；改编', 'It takes time to adapt to a new environment.'),
('cet4', 'adequate', '/ˈædɪkwət/', 'adj. 足够的；适当的', 'We have adequate time to finish the task.'),
('cet4', 'admire', '/ədˈmaɪər/', 'v. 钦佩；欣赏', 'I admire her patience and determination.'),
('cet4', 'advance', '/ədˈvæns/', 'v. 促进；前进 n. 进步', 'Technology continues to advance rapidly.'),
('cet4', 'approach', '/əˈproʊtʃ/', 'n. 方法；接近 v. 接近', 'We need a better approach to this problem.'),
('cet4', 'arrange', '/əˈreɪndʒ/', 'v. 安排；整理', 'They arranged a meeting for Friday morning.'),
('cet4', 'attitude', '/ˈætɪtuːd/', 'n. 态度；看法', 'A positive attitude helps in difficult times.'),
('cet4', 'benefit', '/ˈbenɪfɪt/', 'n. 益处 v. 使受益', 'Regular exercise benefits both mind and body.'),
('cet4', 'establish', '/ɪˈstæblɪʃ/', 'v. 建立；确立', 'The school hopes to establish a reading club.'),

-- CET6
('cet6', 'ambiguous', '/æmˈbɪɡjuəs/', 'adj. 模棱两可的；含糊的', 'His answer was too ambiguous to be useful.'),
('cet6', 'coherent', '/koʊˈhɪrənt/', 'adj. 连贯的；一致的', 'She gave a coherent explanation of the issue.'),
('cet6', 'compile', '/kəmˈpaɪl/', 'v. 汇编；编译', 'The editor compiled the reports into one volume.'),
('cet6', 'consecutive', '/kənˈsekjətɪv/', 'adj. 连续的', 'He worked for ten consecutive days.'),
('cet6', 'controversial', '/ˌkɑːntrəˈvɜːrʃl/', 'adj. 有争议的', 'The policy remains highly controversial.'),
('cet6', 'designate', '/ˈdezɪɡneɪt/', 'v. 指定；命名', 'The city designated the area as a cultural zone.'),
('cet6', 'dramatic', '/drəˈmætɪk/', 'adj. 显著的；戏剧性的', 'The medicine produced a dramatic improvement.'),
('cet6', 'equivalent', '/ɪˈkwɪvələnt/', 'adj. 相等的；等同的 n. 对应物', 'One euro is not equivalent to one dollar.'),
('cet6', 'eventual', '/ɪˈventʃuəl/', 'adj. 最终的', 'Everyone hoped for the eventual success of the plan.'),
('cet6', 'explicit', '/ɪkˈsplɪsɪt/', 'adj. 明确的；清楚的', 'The manager gave explicit instructions to the team.'),
('cet6', 'feasible', '/ˈfiːzəbl/', 'adj. 可行的', 'This solution is feasible within our budget.'),
('cet6', 'inevitable', '/ɪnˈevɪtəbl/', 'adj. 不可避免的', 'Some degree of change is inevitable in any project.'),
('cet6', 'motivate', '/ˈmoʊtɪveɪt/', 'v. 激励；促使', 'Clear goals can motivate people to work harder.'),
('cet6', 'priority', '/praɪˈɔːrəti/', 'n. 优先事项', 'Safety should be our top priority.'),
('cet6', 'relevant', '/ˈreləvənt/', 'adj. 相关的；切题的', 'Please provide relevant evidence for your claim.'),

-- TOEFL
('toefl', 'abundant', '/əˈbʌndənt/', 'adj. 丰富的；充足的', 'The region has abundant natural resources.'),
('toefl', 'assess', '/əˈses/', 'v. 评估；评定', 'Researchers assess the impact of climate change carefully.'),
('toefl', 'comprise', '/kəmˈpraɪz/', 'v. 包含；组成', 'The committee comprises experts from several fields.'),
('toefl', 'derive', '/dɪˈraɪv/', 'v. 获得；源于', 'Many English words derive from Latin.'),
('toefl', 'diminish', '/dɪˈmɪnɪʃ/', 'v. 减少；削弱', 'Public trust may diminish after repeated mistakes.'),
('toefl', 'exceed', '/ɪkˈsiːd/', 'v. 超过', 'The results exceeded the scientists expectations.'),
('toefl', 'fluctuate', '/ˈflʌktʃueɪt/', 'v. 波动；起伏', 'Oil prices tend to fluctuate over time.'),
('toefl', 'framework', '/ˈfreɪmwɜːrk/', 'n. 框架；结构', 'The theory provides a useful framework for analysis.'),
('toefl', 'inherent', '/ɪnˈhɪrənt/', 'adj. 固有的；内在的', 'Risk is inherent in scientific exploration.'),
('toefl', 'interpret', '/ɪnˈtɜːrprət/', 'v. 解释；理解', 'Students must interpret the graph accurately.'),
('toefl', 'justify', '/ˈdʒʌstɪfaɪ/', 'v. 证明合理；为……辩护', 'The evidence does not justify that conclusion.'),
('toefl', 'modify', '/ˈmɑːdɪfaɪ/', 'v. 修改；调整', 'The researchers modified the experiment design.'),
('toefl', 'outcome', '/ˈaʊtkʌm/', 'n. 结果；后果', 'No one could predict the final outcome.'),
('toefl', 'retain', '/rɪˈteɪn/', 'v. 保持；保留', 'The material can retain heat for a long time.'),
('toefl', 'widespread', '/ˈwaɪdspred/', 'adj. 普遍的；广泛的', 'The discovery attracted widespread attention.'),

-- IELTS
('ielts', 'allocate', '/ˈæləkeɪt/', 'v. 分配；拨给', 'The city plans to allocate more funds to public transport.'),
('ielts', 'anticipate', '/ænˈtɪsɪpeɪt/', 'v. 预期；预料', 'Experts anticipate further growth in the housing market.'),
('ielts', 'controversy', '/ˈkɑːntrəvɜːrsi/', 'n. 争议；争论', 'The new policy has caused public controversy.'),
('ielts', 'demographic', '/ˌdeməˈɡræfɪk/', 'adj. 人口统计的', 'Demographic changes affect urban planning greatly.'),
('ielts', 'infrastructure', '/ˈɪnfrəstrʌktʃər/', 'n. 基础设施', 'Modern infrastructure is essential for economic growth.'),
('ielts', 'migration', '/maɪˈɡreɪʃn/', 'n. 迁徙；迁移', 'Rural to urban migration has increased in recent years.'),
('ielts', 'proportion', '/prəˈpɔːrʃn/', 'n. 比例；部分', 'A large proportion of residents commute by bus.'),
('ielts', 'regulate', '/ˈreɡjuleɪt/', 'v. 管理；规范', 'Governments should regulate industrial emissions.'),
('ielts', 'reliable', '/rɪˈlaɪəbl/', 'adj. 可靠的', 'Reliable public data helps researchers make sound decisions.'),
('ielts', 'residential', '/ˌrezɪˈdenʃl/', 'adj. 住宅的；居住的', 'The council approved a new residential area.'),
('ielts', 'sustainable', '/səˈsteɪnəbl/', 'adj. 可持续的', 'Sustainable farming can protect the environment.'),
('ielts', 'trend', '/trend/', 'n. 趋势；动向', 'This chart shows a clear upward trend.'),
('ielts', 'urbanization', '/ˌɜːrbənəˈzeɪʃn/', 'n. 城市化', 'Urbanization brings both opportunities and challenges.'),
('ielts', 'vulnerable', '/ˈvʌlnərəbl/', 'adj. 脆弱的；易受影响的', 'Children are especially vulnerable to air pollution.'),
('ielts', 'welfare', '/ˈwelfer/', 'n. 福利；福祉', 'Public welfare depends on fair access to healthcare.'),

-- 考研英语
('kaoyan', 'empirical', '/ɪmˈpɪrɪkl/', 'adj. 以实证为基础的；经验主义的', 'The theory needs empirical support from experiments.'),
('kaoyan', 'formulate', '/ˈfɔːrmjuleɪt/', 'v. 制定；阐述', 'The committee formulated a new research plan.'),
('kaoyan', 'hypothesis', '/haɪˈpɑːθəsɪs/', 'n. 假设；假说', 'The paper begins with a clear hypothesis.'),
('kaoyan', 'infer', '/ɪnˈfɜːr/', 'v. 推断；推论', 'We can infer the cause from the available evidence.'),
('kaoyan', 'integrate', '/ˈɪntɪɡreɪt/', 'v. 整合；使结合', 'The course integrates theory with practice.'),
('kaoyan', 'mechanism', '/ˈmekənɪzəm/', 'n. 机制；机理', 'Scientists are still studying the mechanism of memory.'),
('kaoyan', 'notion', '/ˈnoʊʃn/', 'n. 观念；概念', 'He rejected the notion that talent alone decides success.'),
('kaoyan', 'prospect', '/ˈprɑːspekt/', 'n. 前景；可能性', 'The job offers good prospects for advancement.'),
('kaoyan', 'reinforce', '/ˌriːɪnˈfɔːrs/', 'v. 加强；强化', 'Daily review can reinforce what you have learned.'),
('kaoyan', 'render', '/ˈrendər/', 'v. 使得；给予', 'Lack of sleep can render people less productive.'),
('kaoyan', 'substantial', '/səbˈstænʃl/', 'adj. 大量的；实质性的', 'The study produced substantial evidence.'),
('kaoyan', 'undergo', '/ˌʌndərˈɡoʊ/', 'v. 经历；承受', 'Patients must undergo several tests before surgery.'),
('kaoyan', 'valid', '/ˈvælɪd/', 'adj. 有效的；合理的', 'Your argument is valid only under certain conditions.'),
('kaoyan', 'whereas', '/werˈæz/', 'conj. 然而；鉴于', 'This method is simple, whereas that one is more accurate.'),
('kaoyan', 'yield', '/jiːld/', 'v. 产生；屈服 n. 产量', 'Careful analysis may yield unexpected results.'),

-- GRE
('gre', 'altruistic', '/ˌæltruˈɪstɪk/', 'adj. 利他的；无私的', 'Her altruistic motives won the trust of the team.'),
('gre', 'audacious', '/ɔːˈdeɪʃəs/', 'adj. 大胆的；鲁莽的', 'The startup made an audacious attempt to change the market.'),
('gre', 'ephemeral', '/ɪˈfemərəl/', 'adj. 短暂的；转瞬即逝的', 'Online fame can be surprisingly ephemeral.'),
('gre', 'meticulous', '/məˈtɪkjələs/', 'adj. 一丝不苟的；严谨的', 'She kept meticulous notes during the research.'),
('gre', 'obsolete', '/ˌɑːbsəˈliːt/', 'adj. 过时的；淘汰的', 'Many devices became obsolete after the update.'),
('gre', 'ostentatious', '/ˌɑːstenˈteɪʃəs/', 'adj. 炫耀的；铺张的', 'His ostentatious lifestyle impressed no one.'),
('gre', 'pervasive', '/pərˈveɪsɪv/', 'adj. 无处不在的；普遍存在的', 'Digital technology has a pervasive influence on society.'),
('gre', 'placate', '/ˈpleɪkeɪt/', 'v. 安抚；平息', 'They tried to placate the angry customers with refunds.'),
('gre', 'pragmatic', '/præɡˈmætɪk/', 'adj. 务实的；实用的', 'A pragmatic approach often solves complex problems faster.'),
('gre', 'resilient', '/rɪˈzɪliənt/', 'adj. 有韧性的；能迅速恢复的', 'Children are often more resilient than adults expect.'),
('gre', 'skeptical', '/ˈskeptɪkl/', 'adj. 怀疑的；存疑的', 'The committee remained skeptical about the proposal.'),
('gre', 'succinct', '/səkˈsɪŋkt/', 'adj. 简洁的；简明的', 'His summary was clear and succinct.'),
('gre', 'ubiquitous', '/juːˈbɪkwɪtəs/', 'adj. 无处不在的', 'Smartphones have become ubiquitous in modern life.'),
('gre', 'versatile', '/ˈvɜːrsətl/', 'adj. 多才多艺的；多用途的', 'This tool is versatile enough for several tasks.'),
('gre', 'wary', '/ˈweri/', 'adj. 谨慎的；提防的', 'Investors are wary of sudden market changes.');

INSERT INTO words (word, phonetic, meaning, example, module_id)
SELECT t.word, t.phonetic, t.meaning, t.example, m.id
FROM tmp_vocab_import t
JOIN exam_modules m ON m.code = t.module_code
LEFT JOIN words w ON w.module_id = m.id AND w.word = t.word
WHERE w.id IS NULL
ORDER BY m.sort_order, t.word;

DROP TEMPORARY TABLE tmp_vocab_import;

SELECT m.code AS module_code, COUNT(*) AS total_words
FROM words w
JOIN exam_modules m ON m.id = w.module_id
WHERE m.code IN ('cet4', 'cet6', 'toefl', 'ielts', 'kaoyan', 'gre')
GROUP BY m.code, m.sort_order
ORDER BY m.sort_order;
