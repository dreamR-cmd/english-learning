-- 插入默认用户
INSERT IGNORE INTO users (id, username, password, nickname) VALUES (1, 'admin', '123456', '管理员');
INSERT IGNORE INTO users (id, username, password, nickname) VALUES (2, 'user', '123456', '学习用户');

-- 插入考试模块
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (1, '大学英语四级', 'cet4', 'CET-4 大学英语四级考试，适合大学生基础英语水平', '📘', 1);
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (2, '大学英语六级', 'cet6', 'CET-6 大学英语六级考试，适合中高级英语水平', '📗', 2);
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (3, '托福', 'toefl', 'TOEFL 托福考试，适合出国留学英语水平', '📕', 3);
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (4, '雅思', 'ielts', 'IELTS 雅思考试，适合英联邦国家留学移民', '📙', 4);
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (5, '考研英语', 'kaoyan', '全国硕士研究生入学统一考试英语', '📚', 5);
INSERT IGNORE INTO exam_modules (id, name, code, description, icon, sort_order) VALUES (6, 'GRE', 'gre', 'GRE 美国研究生入学考试', '📓', 6);

-- 插入 CET4 单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (1, 'abandon', '/əˈbændən/', 'v. 放弃；遗弃', 'He had to abandon his plan.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (2, 'ability', '/əˈbɪləti/', 'n. 能力；才能', 'She has the ability to solve problems.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (3, 'absorb', '/əbˈzɔːrb/', 'v. 吸收；吸引', 'Plants absorb carbon dioxide.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (4, 'academic', '/ˌækəˈdemɪk/', 'adj. 学术的；学院的', 'He has a strong academic background.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (5, 'accelerate', '/əkˈseləreɪt/', 'v. 加速；促进', 'The car accelerated quickly.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (6, 'access', '/ˈækses/', 'n. 通道；进入 v. 访问', 'Students have access to the library.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (7, 'accommodate', '/əˈkɒmədeɪt/', 'v. 容纳；向…提供住处', 'The hotel can accommodate 500 guests.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (8, 'accompany', '/əˈkʌmpəni/', 'v. 陪伴；伴随', 'She accompanied me to the airport.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (9, 'accomplish', '/əˈkʌmplɪʃ/', 'v. 完成；实现', 'He accomplished his goal.', 1);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (10, 'accurate', '/ˈækjərət/', 'adj. 准确的；精确的', 'The data is accurate.', 1);

-- CET4 阅读
INSERT IGNORE INTO readings (id, title, content, questions, is_featured, module_id) VALUES (1, 'The Importance of Learning English',
'English has become a global language. It is used in business, science, technology, and education. Learning English opens up many opportunities for personal and professional growth. More than 1.5 billion people speak English worldwide, making it the most widely spoken language. In many countries, English is taught as a second language from an early age. The ability to communicate in English is increasingly seen as an essential skill in the modern world.',
'[\{\"q\":\"Why is English considered a global language?\",\"options\":[\"It is easy to learn\",\"It is used in many fields\",\"It has simple grammar\",\"It is the oldest language\"],\"answer\":1},{\"q\":\"How many people speak English worldwide?\",\"options\":[\"500 million\",\"1 billion\",\"1.5 billion\",\"2 billion\"],\"answer\":2}]',
true, 1);
INSERT IGNORE INTO readings (id, title, content, questions, is_featured, module_id) VALUES (2, 'Environmental Protection',
'Environmental protection has become a pressing global issue. Climate change, pollution, and loss of biodiversity threaten our planet. Many countries have taken measures to reduce carbon emissions and protect natural resources. Individuals can also contribute by reducing waste, conserving energy, and supporting sustainable practices.',
'[\{\"q\":\"What threatens our planet according to the passage?\",\"options\":[\"Economic growth\",\"Population increase\",\"Climate change and pollution\",\"Urban development\"],\"answer\":2},{\"q\":\"How can individuals contribute?\",\"options\":[\"By using more energy\",\"By reducing waste\",\"By buying more goods\",\"By traveling more\"],\"answer\":1}]',
true, 1);

-- CET4 听力
INSERT IGNORE INTO listenings (id, title, transcript, questions, module_id) VALUES (1, 'Campus Life Conversation',
'A: Hi, welcome to the university library. Can I help you find something?
B: Yes, I am looking for books on English literature.
A: They are on the third floor, section C. You can also use the online catalog.
B: Thank you. By the way, how long can I borrow books?
A: For undergraduate students, up to four weeks. You can renew online.
B: That is great. Thanks for your help.',
'[\{\"q\":\"Where does this conversation take place?\",\"options\":[\"Classroom\",\"Library\",\"Dormitory\",\"Cafeteria\"],\"answer\":1},{\"q\":\"How long can undergraduate students borrow books?\",\"options\":[\"Two weeks\",\"Three weeks\",\"Four weeks\",\"Five weeks\"],\"answer\":2},{\"q\":\"How can books be renewed?\",\"options\":[\"By phone\",\"In person\",\"Online\",\"By email\"],\"answer\":2}]',
1);

-- 托福单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (11, 'abundant', '/əˈbʌndənt/', 'adj. 丰富的；充裕的', 'The region has abundant natural resources.', 3);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (12, 'comprehensive', '/ˌkɒmprɪˈhensɪv/', 'adj. 全面的；综合的', 'We need a comprehensive solution.', 3);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (13, 'demonstrate', '/ˈdemənstreɪt/', 'v. 证明；演示', 'The experiment demonstrates the theory.', 3);

-- 雅思单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (14, 'migration', '/maɪˈɡreɪʃn/', 'n. 迁移；移居', 'Bird migration is a fascinating phenomenon.', 4);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (15, 'sustainable', '/səˈsteɪnəbl/', 'adj. 可持续的', 'We need sustainable development.', 4);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (16, 'urbanization', '/ˌɜːrbənəˈzeɪʃn/', 'n. 城市化', 'Urbanization is accelerating in developing countries.', 4);

-- CET6 单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (17, 'paradigm', '/ˈpærədaɪm/', 'n. 范式；典范', 'This marks a paradigm shift in thinking.', 2);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (18, 'ambiguous', '/æmˈbɪɡjuəs/', 'adj. 模糊的；含糊的', 'The statement was ambiguous.', 2);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (19, 'eloquent', '/ˈeləkwənt/', 'adj. 雄辩的；有口才的', 'She gave an eloquent speech.', 2);

-- TOEFL 阅读
INSERT IGNORE INTO readings (id, title, content, questions, is_featured, module_id) VALUES (3, 'Climate Change and Its Impact',
'Climate change represents one of the most significant challenges facing humanity in the 21st century. Scientific evidence indicates that global temperatures have risen substantially over the past century, primarily due to human activities such as the burning of fossil fuels and deforestation. The consequences include rising sea levels, more frequent extreme weather events, and disruptions to ecosystems worldwide. Addressing climate change requires international cooperation and a transition to sustainable energy sources.',
'[\{\"q\":\"What is the primary cause of climate change according to the passage?\",\"options\":[\"Volcanic activity\",\"Human activities\",\"Solar radiation\",\"Ocean currents\"],\"answer\":1},{\"q\":\"Which is NOT mentioned as a consequence of climate change?\",\"options\":[\"Rising sea levels\",\"Extreme weather\",\"Improved agriculture\",\"Ecosystem disruption\"],\"answer\":2}]',
true, 3);

-- 考研单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (20, 'hypothesis', '/haɪˈpɒθəsɪs/', 'n. 假说；假设', 'The scientist proposed a new hypothesis.', 5);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (21, 'empirical', '/ɪmˈpɪrɪkl/', 'adj. 经验主义的', 'We need empirical evidence.', 5);

-- GRE 单词
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (22, 'ephemeral', '/ɪˈfemərəl/', 'adj. 短暂的；转瞬即逝的', 'Fame is ephemeral.', 6);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (23, 'ubiquitous', '/juːˈbɪkwɪtəs/', 'adj. 无处不在的', 'Smartphones have become ubiquitous.', 6);
INSERT IGNORE INTO words (id, word, phonetic, meaning, example, module_id) VALUES (24, 'pragmatic', '/præɡˈmætɪk/', 'adj. 实用的；务实的', 'We need a pragmatic approach.', 6);
