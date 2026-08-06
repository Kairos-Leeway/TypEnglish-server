-- 给 word_bank 加 category 字段 + 丰富种子数据
ALTER TABLE word_bank ADD COLUMN IF NOT EXISTS category VARCHAR(64) DEFAULT '默认' COMMENT '分类';

-- 按章节分类的种子单词
INSERT IGNORE INTO word_bank (language, category, word, phonetic, translation, part_of_speech, example, difficulty) VALUES
-- CET-4 基础
('en', 'CET-4', 'abandon', '/əˈbændən/', '放弃', 'v.', 'He had to abandon his plan.', 2),
('en', 'CET-4', 'ability', '/əˈbɪləti/', '能力', 'n.', 'She has the ability to succeed.', 1),
('en', 'CET-4', 'abroad', '/əˈbrɔːd/', '在国外', 'adv.', 'He went abroad for study.', 1),
('en', 'CET-4', 'absence', '/ˈæbsəns/', '缺席', 'n.', 'His absence was noticed.', 2),
('en', 'CET-4', 'absolute', '/ˈæbsəluːt/', '绝对的', 'adj.', 'I have absolute confidence.', 2),
('en', 'CET-4', 'absorb', '/əbˈzɔːrb/', '吸收', 'v.', 'Plants absorb sunlight.', 2),
('en', 'CET-4', 'abstract', '/ˈæbstrækt/', '抽象的', 'adj.', 'The idea is too abstract.', 3),
('en', 'CET-4', 'abundant', '/əˈbʌndənt/', '丰富的', 'adj.', 'We have abundant resources.', 3),
('en', 'CET-4', 'academic', '/ˌækəˈdemɪk/', '学术的', 'adj.', 'She has academic interests.', 2),
('en', 'CET-4', 'accelerate', '/əkˈseləreɪt/', '加速', 'v.', 'The car began to accelerate.', 3),
('en', 'CET-4', 'access', '/ˈækses/', '进入;通道', 'n./v.', 'You need a password to access.', 2),
('en', 'CET-4', 'accompany', '/əˈkʌmpəni/', '陪伴', 'v.', 'I will accompany you.', 2),
('en', 'CET-4', 'accomplish', '/əˈkɑːmplɪʃ/', '完成', 'v.', 'He accomplished his goal.', 3),
('en', 'CET-4', 'account', '/əˈkaʊnt/', '账户;解释', 'n./v.', 'Please give an account of what happened.', 2),
('en', 'CET-4', 'accurate', '/ˈækjərət/', '精确的', 'adj.', 'The data is accurate.', 2),

-- CET-4 进阶
('en', 'CET-4', 'acknowledge', '/əkˈnɑːlɪdʒ/', '承认', 'v.', 'He acknowledged his mistake.', 3),
('en', 'CET-4', 'acquire', '/əˈkwaɪər/', '获得', 'v.', 'She acquired new skills.', 3),
('en', 'CET-4', 'adapt', '/əˈdæpt/', '适应', 'v.', 'We must adapt to change.', 2),
('en', 'CET-4', 'adequate', '/ˈædɪkwət/', '足够的', 'adj.', 'The supply is adequate.', 3),
('en', 'CET-4', 'adjust', '/əˈdʒʌst/', '调整', 'v.', 'Please adjust the volume.', 2),
('en', 'CET-4', 'administration', '/ədˌmɪnɪˈstreɪʃn/', '管理', 'n.', 'The administration made a decision.', 3),
('en', 'CET-4', 'admire', '/ədˈmaɪər/', '钦佩', 'v.', 'I admire your courage.', 2),
('en', 'CET-4', 'adopt', '/əˈdɑːpt/', '采纳;收养', 'v.', 'They decided to adopt the policy.', 2),
('en', 'CET-4', 'advertise', '/ˈædvərtaɪz/', '做广告', 'v.', 'They advertise on TV.', 2),
('en', 'CET-4', 'affair', '/əˈfer/', '事务', 'n.', 'It is a private affair.', 2),

-- CET-6
('en', 'CET-6', 'abolish', '/əˈbɑːlɪʃ/', '废除', 'v.', 'They voted to abolish the law.', 4),
('en', 'CET-6', 'absurd', '/əbˈsɜːrd/', '荒谬的', 'adj.', 'The idea seems absurd.', 4),
('en', 'CET-6', 'acquaint', '/əˈkweɪnt/', '使熟悉', 'v.', 'Let me acquaint you with the facts.', 4),
('en', 'CET-6', 'adhere', '/ədˈhɪr/', '遵守;粘附', 'v.', 'We must adhere to the rules.', 4),
('en', 'CET-6', 'adjacent', '/əˈdʒeɪsnt/', '邻近的', 'adj.', 'The two buildings are adjacent.', 4),
('en', 'CET-6', 'adolescent', '/ˌædəˈlesnt/', '青少年', 'n./adj.', 'Adolescent behavior can be puzzling.', 3),
('en', 'CET-6', 'aggravate', '/ˈæɡrəveɪt/', '加重', 'v.', 'The medicine aggravated the pain.', 5),
('en', 'CET-6', 'alienate', '/ˈeɪliəneɪt/', '疏远', 'v.', 'His behavior alienated his friends.', 5),
('en', 'CET-6', 'alleviate', '/əˈliːvieɪt/', '减轻', 'v.', 'This medicine alleviates pain.', 4),
('en', 'CET-6', 'ambiguous', '/æmˈbɪɡjuəs/', '模糊的', 'adj.', 'The statement was ambiguous.', 4),

-- 日常口语
('en', '日常', 'awesome', '/ˈɔːsəm/', '太棒了', 'adj.', 'That movie was awesome!', 1),
('en', '日常', 'basically', '/ˈbeɪsɪkli/', '基本上', 'adv.', 'Basically, I agree with you.', 1),
('en', '日常', 'definitely', '/ˈdefɪnətli/', '绝对地', 'adv.', 'I will definitely come.', 1),
('en', '日常', 'exactly', '/ɪɡˈzæktli/', '正是如此', 'adv.', 'That is exactly what I mean.', 1),
('en', '日常', 'absolutely', '/ˈæbsəluːtli/', '绝对', 'adv.', 'You are absolutely right.', 1),
('en', '日常', 'apparently', '/əˈpærəntli/', '显然', 'adv.', 'Apparently, he was wrong.', 2),
('en', '日常', 'literally', '/ˈlɪtərəli/', '字面上;简直', 'adv.', 'I was literally exhausted.', 2),
('en', '日常', 'honestly', '/ˈɑːnɪstli/', '老实说', 'adv.', 'Honestly, I do not know.', 1),
('en', '日常', 'unfortunately', '/ʌnˈfɔːrtʃənətli/', '不幸地', 'adv.', 'Unfortunately, it rained.', 2),
('en', '日常', 'obviously', '/ˈɑːbviəsli/', '显然', 'adv.', 'Obviously, he is wrong.', 1),

-- 商务英语
('en', '商务', 'negotiate', '/nɪˈɡoʊʃieɪt/', '谈判', 'v.', 'They negotiated a better deal.', 3),
('en', '商务', 'revenue', '/ˈrevənuː/', '收入', 'n.', 'The company increased its revenue.', 3),
('en', '商务', 'strategy', '/ˈstrætədʒi/', '策略', 'n.', 'We need a new strategy.', 2),
('en', '商务', 'implement', '/ˈɪmplɪment/', '实施', 'v.', 'We will implement the plan.', 3),
('en', '商务', 'stakeholder', '/ˈsteɪkhoʊldər/', '利益相关者', 'n.', 'All stakeholders were informed.', 4),
('en', '商务', 'deadline', '/ˈdedlaɪn/', '截止日期', 'n.', 'The deadline is Friday.', 2),
('en', '商务', 'budget', '/ˈbʌdʒɪt/', '预算', 'n.', 'We need to cut the budget.', 2),
('en', '商务', 'collaborate', '/kəˈlæbəreɪt/', '合作', 'v.', 'We collaborate with partners.', 3),
('en', '商务', 'proposal', '/prəˈpoʊzl/', '提案', 'n.', 'She wrote a detailed proposal.', 3),
('en', '商务', 'efficient', '/ɪˈfɪʃnt/', '高效的', 'adj.', 'The new system is efficient.', 2);
