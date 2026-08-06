-- 句子库
CREATE TABLE IF NOT EXISTS `sentence_bank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `language` VARCHAR(8) NOT NULL COMMENT '语言代码',
    `chinese` VARCHAR(1024) NOT NULL COMMENT '中文句子',
    `english` VARCHAR(1024) NOT NULL COMMENT '英文翻译',
    `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '难度 1-5',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_lang_diff` (`language`, `difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='句子库';

-- 种子句子
INSERT INTO sentence_bank (language, chinese, english, difficulty) VALUES
('en', '我今天早上起床很晚,所以迟到了。', 'I got up very late this morning so I was late for work.', 1),
('en', '他正在学习如何用Python编写程序。', 'He is learning how to write programs in Python.', 2),
('en', '尽管外面下着大雨,她还是决定出去散步。', 'Although it was raining heavily outside she decided to go for a walk.', 3),
('en', '如果你想成功,就必须比其他人更加努力。', 'If you want to succeed you have to work harder than others.', 2),
('en', '这家餐厅的食物非常美味,但是价格有点贵。', 'The food at this restaurant is very delicious but the price is a bit expensive.', 2),
('en', '我昨天在图书馆看了一本关于人工智能的书。', 'Yesterday I read a book about artificial intelligence in the library.', 2),
('en', '她不仅会说英语,还会说法语和日语。', 'She can speak not only English but also French and Japanese.', 3),
('en', '我们应该保护环境,减少使用塑料制品。', 'We should protect the environment and reduce the use of plastic products.', 2),
('en', '这个计划听起来不错,但我们还需要更多细节。', 'The plan sounds good but we still need more details.', 2),
('en', '他花了三个月的时间才完成这个项目。', 'It took him three months to complete this project.', 2),
('en', '无论发生什么,我都会一直支持你。', 'No matter what happens I will always support you.', 2),
('en', '老师要求我们在下周之前交作业。', 'The teacher asked us to hand in the homework before next week.', 1),
('en', '他已经习惯了每天早上去公园跑步。', 'He has gotten used to going for a run in the park every morning.', 3),
('en', '这本书太有趣了,我忍不住一口气读完了。', 'The book was so interesting that I could not help reading it in one sitting.', 3),
('en', '我们应该抓住每一个学习新知识的机会。', 'We should seize every opportunity to learn new knowledge.', 2);
