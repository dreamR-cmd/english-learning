INSERT INTO users (id, username, password, nickname, daily_word_target) VALUES
  (1, 'admin', '123456', 'Admin', 20),
  (2, 'user', '123456', 'Learner', 20);

INSERT INTO exam_modules (id, name, code, description, icon, sort_order) VALUES
  (1, 'CET-4', 'cet4', 'College English Test Band 4 practice module.', 'book-open', 1),
  (2, 'CET-6', 'cet6', 'College English Test Band 6 practice module.', 'graduation-cap', 2),
  (3, 'TOEFL', 'toefl', 'TOEFL vocabulary, reading, and listening practice.', 'globe', 3),
  (4, 'IELTS', 'ielts', 'IELTS vocabulary, reading, and listening practice.', 'headphones', 4),
  (5, 'Postgraduate English', 'kaoyan', 'Postgraduate entrance exam English practice.', 'notebook-pen', 5),
  (6, 'GRE', 'gre', 'GRE vocabulary and reading practice.', 'brain', 6);

INSERT INTO words (id, word, phonetic, meaning, example, module_id) VALUES
  (1, 'abandon', '/abandon/', 'v. to leave behind or give up', 'He had to abandon his original plan.', 1),
  (2, 'ability', '/ability/', 'n. skill or power to do something', 'She has the ability to solve difficult problems.', 1),
  (3, 'absorb', '/absorb/', 'v. to take in liquid, information, or energy', 'Plants absorb carbon dioxide.', 1),
  (4, 'academic', '/academic/', 'adj. related to education or scholarship', 'He has a strong academic background.', 1),
  (5, 'accelerate', '/accelerate/', 'v. to increase speed or progress', 'The car accelerated quickly.', 1),
  (6, 'access', '/access/', 'n. a way to enter or use something; v. to open or use', 'Students have access to the library.', 1),
  (7, 'accommodate', '/accommodate/', 'v. to provide space or meet a need', 'The hotel can accommodate 500 guests.', 1),
  (8, 'accompany', '/accompany/', 'v. to go with someone', 'She accompanied me to the airport.', 1),
  (9, 'accomplish', '/accomplish/', 'v. to complete successfully', 'He accomplished his goal.', 1),
  (10, 'accurate', '/accurate/', 'adj. correct and exact', 'The data is accurate.', 1),
  (11, 'paradigm', '/paradigm/', 'n. a model or pattern', 'This marks a paradigm shift in thinking.', 2),
  (12, 'ambiguous', '/ambiguous/', 'adj. having more than one possible meaning', 'The statement was ambiguous.', 2),
  (13, 'eloquent', '/eloquent/', 'adj. fluent and persuasive in speaking or writing', 'She gave an eloquent speech.', 2),
  (14, 'abundant', '/abundant/', 'adj. more than enough; plentiful', 'The region has abundant natural resources.', 3),
  (15, 'comprehensive', '/comprehensive/', 'adj. complete and including many details', 'We need a comprehensive solution.', 3),
  (16, 'demonstrate', '/demonstrate/', 'v. to show clearly', 'The experiment demonstrates the theory.', 3),
  (17, 'migration', '/migration/', 'n. movement from one place to another', 'Bird migration is a fascinating phenomenon.', 4),
  (18, 'sustainable', '/sustainable/', 'adj. able to continue without damaging resources', 'We need sustainable development.', 4),
  (19, 'urbanization', '/urbanization/', 'n. the process of cities growing', 'Urbanization is accelerating in developing countries.', 4),
  (20, 'hypothesis', '/hypothesis/', 'n. an idea proposed for testing', 'The scientist proposed a new hypothesis.', 5),
  (21, 'empirical', '/empirical/', 'adj. based on observation or experience', 'We need empirical evidence.', 5),
  (22, 'ephemeral', '/ephemeral/', 'adj. lasting for a very short time', 'Fame is ephemeral.', 6),
  (23, 'ubiquitous', '/ubiquitous/', 'adj. found everywhere', 'Smartphones have become ubiquitous.', 6),
  (24, 'pragmatic', '/pragmatic/', 'adj. practical and realistic', 'We need a pragmatic approach.', 6);

INSERT INTO readings (id, title, content, questions, is_featured, module_id) VALUES
  (1, 'The Importance of Learning English',
   'English is used in business, science, technology, and education around the world. Learning English opens up opportunities for personal and professional growth. In many countries, English is taught as a second language from an early age.',
   '[{"q":"Why is English useful worldwide?","options":["It is used in many fields","It has no grammar","It is the oldest language","It is only used in schools"],"answer":0},{"q":"What can learning English create?","options":["Fewer choices","New opportunities","Less communication","Only exams"],"answer":1}]',
   true, 1),
  (2, 'Environmental Protection',
   'Environmental protection has become a pressing global issue. Climate change, pollution, and loss of biodiversity threaten the planet. Individuals can help by reducing waste, conserving energy, and supporting sustainable practices.',
   '[{"q":"What threatens the planet?","options":["Climate change and pollution","Better schools","Longer holidays","New books"],"answer":0},{"q":"How can individuals help?","options":["By wasting energy","By reducing waste","By buying more goods","By ignoring nature"],"answer":1}]',
   true, 1),
  (3, 'Climate Change and Its Impact',
   'Climate change is one of the major challenges of the twenty-first century. Scientific evidence shows that global temperatures have risen substantially, largely because of human activity such as burning fossil fuels and cutting down forests.',
   '[{"q":"What is a major cause of climate change in the passage?","options":["Human activity","Ocean tides","Old books","Moonlight"],"answer":0},{"q":"What has happened to global temperatures?","options":["They have risen","They have disappeared","They never change","They are unknown"],"answer":0}]',
   true, 3);

INSERT INTO listenings (id, title, audio_url, transcript, questions, module_id) VALUES
  (1, 'Campus Life Conversation', NULL,
   'A: Hi, welcome to the university library. Can I help you find something? B: Yes, I am looking for books on English literature. A: They are on the third floor, section C. You can also use the online catalog. B: Thank you. How long can I borrow books? A: For undergraduate students, up to four weeks. You can renew online.',
   '[{"q":"Where does this conversation take place?","options":["Classroom","Library","Dormitory","Cafeteria"],"answer":1},{"q":"How long can undergraduate students borrow books?","options":["Two weeks","Three weeks","Four weeks","Five weeks"],"answer":2},{"q":"How can books be renewed?","options":["By phone","In person","Online","By email"],"answer":2}]',
   1);
