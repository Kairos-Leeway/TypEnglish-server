package com.typenglish.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 每次 AI 请求创建一个实例，userId 不共享，因此并发对话不会串用户。 */
public class AiUserTools {
    private final Long userId;
    private final AiToolService tools;
    private final LearningCoachService coach;
    private final Consumer<AiToolExecutionEvent> events;

    public AiUserTools(Long userId, AiToolService tools, LearningCoachService coach,
                       Consumer<AiToolExecutionEvent> events) {
        this.userId = userId;
        this.tools = tools;
        this.coach = coach;
        this.events = events != null ? events : ignored -> {};
    }

    @Tool(description = "查询当前用户的单词错题本")
    public List<Map<String, Object>> getMyErrorBook(@ToolParam(description = "语言代码") String language) {
        return execute("getMyErrorBook", "正在读取单词错题", null,
                () -> tools.getMyErrorBook(userId, language));
    }

    @Tool(description = "查询当前用户的句子错题本")
    public List<Map<String, Object>> getMySentenceErrors(@ToolParam(description = "语言代码") String language) {
        return execute("getMySentenceErrors", "正在读取句子错题", null,
                () -> tools.getMySentenceErrors(userId, language));
    }

    @Tool(description = "查询当前用户的练习统计")
    public Map<String, Object> getMyPracticeStats() {
        return execute("getMyPracticeStats", "正在汇总练习数据", null,
                () -> tools.getMyPracticeStats(userId));
    }

    @Tool(description = "查看当前用户近期练习")
    public List<Map<String, Object>> getMyRecentPractices(@ToolParam(description = "数量，最多50") Integer limit) {
        return execute("getMyRecentPractices", "正在读取近期练习", null,
                () -> tools.getMyRecentPractices(userId, limit));
    }

    @Tool(description = "分析当前用户的薄弱环节")
    public Map<String, Object> analyzeMyWeakPoints(@ToolParam(description = "语言") String language) {
        return execute("analyzeMyWeakPoints", "正在分析薄弱环节", null,
                () -> tools.analyzeMyWeakPoints(userId, language));
    }

    @Tool(description = "查询单词详情")
    public Map<String, Object> getWordDetail(@ToolParam(description = "单词") String word,
                                             @ToolParam(description = "语言") String language) {
        return execute("getWordDetail", "正在查询单词详情", null,
                () -> tools.getWordDetail(word, language));
    }

    @Tool(description = "根据到期错题和近期表现推荐下一轮练习")
    public Map<String, Object> recommendNextSession(@ToolParam(description = "语言") String language) {
        return execute("recommendNextSession", "正在制定下一轮练习", null,
                () -> coach.recommendNextSession(userId, language));
    }

    @Tool(description = "创建一轮可直接开始的个性化练习")
    public Map<String, Object> createPracticeSession(
            @ToolParam(description = "语言") String language,
            @ToolParam(description = "题数，5到50") Integer count,
            @ToolParam(description = "模式 typing 或 cloze") String preferredMode) {
        return execute("createPracticeSession", "正在准备个性化练习", "练习已准备好",
                () -> coach.createPracticeSession(userId, language, count, preferredMode));
    }

    @Tool(description = "获取当前用户最近一段时间的学习趋势")
    public Map<String, Object> getLearningTrend(@ToolParam(description = "天数，7到90") Integer days) {
        return execute("getLearningTrend", "正在生成学习趋势", null,
                () -> coach.getLearningTrend(userId, days));
    }

    @Tool(description = "生成单词并写入词库")
    public Map<String, Object> generateWords(String topic, Integer count, Integer difficulty, String language) {
        return execute("generateWords", "正在生成单词", "单词生成完成",
                () -> tools.doGenerateWords(topic, count, difficulty, language));
    }

    @Tool(description = "生成句子并写入句库")
    public Map<String, Object> generateSentences(String topic, Integer count, Integer difficulty, String language) {
        return execute("generateSentences", "正在生成句子", "句子生成完成",
                () -> tools.doGenerateSentences(topic, count, difficulty, language));
    }

    private <T> T execute(String name, String title, String doneTitle, Supplier<T> supplier) {
        String id = UUID.randomUUID().toString();
        events.accept(new AiToolExecutionEvent(id, "start", name, title, null, null));
        try {
            T result = supplier.get();
            Map<String, Object> action = null;
            String summary = doneTitle;
            if (result instanceof Map<?, ?> map) {
                Object value = map.get("summary");
                if (value != null) summary = String.valueOf(value);
                Object rawAction = map.get("action");
                if (rawAction instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) rawAction;
                    action = cast;
                }
            }
            events.accept(new AiToolExecutionEvent(id, "done", name,
                    doneTitle != null ? doneTitle : title.replace("正在", "已"), summary, action));
            return result;
        } catch (RuntimeException e) {
            events.accept(new AiToolExecutionEvent(id, "error", name, "执行失败", e.getMessage(), null));
            throw e;
        }
    }
}
