package com.typenglish.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.LinkedHashMap;
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

    @Tool(description = "当用户想让 AI 出题但尚未明确题型、主题、数量或难度时，展示出题参数表单。调用后必须等待用户提交表单，不能继续生成题目")
    public Map<String, Object> requestQuestionGenerationForm() {
        return execute("requestQuestionGenerationForm", "正在准备出题设置", "请设置出题参数",
                this::questionGenerationForm);
    }

    @Tool(description = "仅当用户已经明确题型、主题、数量和难度，或刚刚提交出题表单时，生成单词并写入词库")
    public Map<String, Object> generateWords(String topic, Integer count, Integer difficulty, String language) {
        return execute("generateWords", "正在生成单词", "单词生成完成",
                () -> tools.doGenerateWords(safeTopic(topic), safeCount(count),
                        safeDifficulty(difficulty), safeLanguage(language)));
    }

    @Tool(description = "仅当用户已经明确题型、主题、数量和难度，或刚刚提交出题表单时，生成句子并写入句库")
    public Map<String, Object> generateSentences(String topic, Integer count, Integer difficulty, String language) {
        return execute("generateSentences", "正在生成句子", "句子生成完成",
                () -> tools.doGenerateSentences(safeTopic(topic), safeCount(count),
                        safeDifficulty(difficulty), safeLanguage(language)));
    }

    private Map<String, Object> questionGenerationForm() {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "input_form");
        action.put("formId", "question_generation");
        action.put("label", "确定并生成");
        action.put("fields", List.of(
                Map.of(
                        "name", "mode",
                        "type", "select",
                        "label", "题目类型",
                        "required", true,
                        "defaultValue", "word",
                        "options", List.of(
                                Map.of("label", "单词", "value", "word"),
                                Map.of("label", "句子", "value", "sentence"))),
                Map.of(
                        "name", "topic",
                        "type", "text",
                        "label", "相关主题",
                        "required", true,
                        "defaultValue", "日常",
                        "placeholder", "例如：旅行、商务、科技",
                        "maxLength", 60),
                Map.of(
                        "name", "count",
                        "type", "number",
                        "label", "出题数量",
                        "required", true,
                        "defaultValue", 10,
                        "min", 5,
                        "max", 50,
                        "step", 5),
                Map.of(
                        "name", "difficulty",
                        "type", "rating",
                        "label", "难度",
                        "required", true,
                        "defaultValue", 2,
                        "min", 1,
                        "max", 5)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("awaitingUserInput", true);
        result.put("summary", "填写题型、主题、数量和难度后，我会继续出题");
        result.put("action", action);
        return result;
    }

    private String safeTopic(String topic) {
        String normalized = topic == null ? "" : topic.trim();
        if (normalized.isEmpty()) return "日常";
        return normalized.substring(0, Math.min(normalized.length(), 60));
    }

    private int safeCount(Integer count) {
        return Math.max(5, Math.min(count != null ? count : 10, 50));
    }

    private int safeDifficulty(Integer difficulty) {
        return Math.max(1, Math.min(difficulty != null ? difficulty : 2, 5));
    }

    private String safeLanguage(String language) {
        return List.of("en", "ja", "de").contains(language) ? language : "en";
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
