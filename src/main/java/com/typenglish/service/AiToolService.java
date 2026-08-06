package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.common.BusinessException;
import com.typenglish.entity.*;
import com.typenglish.mapper.*;
import com.typenglish.security.JwtInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);

    /**
     * 全局 userId，由 AiChatService.stream() 在线程切换前设置。
     * 单用户场景下同一时刻只有一个流式对话在执行，static 即可。
     */
    static volatile Long currentStreamUserId;

    private final ErrorBookMapper errorBookMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final WordBankMapper wordBankMapper;
    private final SentenceBankMapper sentenceBankMapper;
    private final OpenAiChatModel chatModel;
    private final Executor aiGenerateExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiToolService(ErrorBookMapper errorBookMapper, PracticeRecordMapper practiceRecordMapper,
                         WordBankMapper wordBankMapper, SentenceBankMapper sentenceBankMapper,
                         OpenAiChatModel chatModel,
                         @Qualifier("aiGenerateExecutor") Executor aiGenerateExecutor) {
        this.errorBookMapper = errorBookMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.wordBankMapper = wordBankMapper;
        this.sentenceBankMapper = sentenceBankMapper;
        this.chatModel = chatModel;
        this.aiGenerateExecutor = aiGenerateExecutor;
    }

    private Long currentUserId() {
        // 优先取流式对话的 userId（跨线程无限制）
        if (currentStreamUserId != null) return currentStreamUserId;
        // fallback: JWT 拦截器的 ThreadLocal（Controller 直接调用）
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) throw new BusinessException(401, "未登录");
        return uid;
    }

    // ──────────── 以下是 @Tool 方法 ────────────

    @Tool(description = "查询用户错题本")
    public List<Map<String, Object>> getMyErrorBook(@ToolParam(description = "语言代码") String language) {
        Long uid = currentUserId();
        List<ErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<ErrorBook>().eq(ErrorBook::getUserId, uid)
                        .eq(ErrorBook::getMastered, false).orderByDesc(ErrorBook::getErrorCount));
        return errors.stream().map(e -> {
            WordBank w = wordBankMapper.selectById(e.getWordId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("errorId", e.getId());
            m.put("word", w != null ? w.getWord() : "未知");
            m.put("translation", w != null ? w.getTranslation() : "");
            m.put("errorCount", e.getErrorCount());
            return m;
        }).collect(Collectors.toList());
    }

    @Tool(description = "查询练习统计")
    public Map<String, Object> getMyPracticeStats() {
        Long uid = currentUserId();
        Map<String, Object> stats = practiceRecordMapper.stats(uid);
        List<Map<String, Object>> byMode = practiceRecordMapper.countByMode(uid);
        long total = ((Number) stats.getOrDefault("total", 0)).longValue();
        long correct = ((Number) stats.getOrDefault("correct", 0)).longValue();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalPractices", total); r.put("correctCount", correct);
        r.put("accuracy", total > 0 ? Math.round((double) correct / total * 100) + "%" : "0%");
        r.put("byMode", byMode);
        return r;
    }

    @Tool(description = "查询单词详情")
    public Map<String, Object> getWordDetail(@ToolParam(description = "单词") String word,
                                              @ToolParam(description = "语言") String language) {
        WordBank wb = wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>()
                .eq(WordBank::getWord, word).eq(WordBank::getLanguage, language != null ? language : "en"));
        if (wb == null) return Map.of("found", false);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("found", true); m.put("word", wb.getWord()); m.put("phonetic", wb.getPhonetic());
        m.put("translation", wb.getTranslation()); m.put("partOfSpeech", wb.getPartOfSpeech());
        m.put("example", wb.getExample()); m.put("difficulty", wb.getDifficulty());
        return m;
    }

    @Tool(description = "查看近期练习")
    public List<Map<String, Object>> getMyRecentPractices(@ToolParam(description = "数量") Integer limit) {
        Long uid = currentUserId();
        int n = limit != null ? limit : 10;
        return practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, uid).orderByDesc(PracticeRecord::getCreatedAt).last("LIMIT " + n))
                .stream().map(r -> {
                    WordBank w = wordBankMapper.selectById(r.getWordId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("word", w != null ? w.getWord() : ""); m.put("mode", r.getMode());
                    m.put("correct", r.getCorrect()); m.put("answer", r.getAnswer());
                    return m;
                }).collect(Collectors.toList());
    }

    @Tool(description = "分析薄弱环节")
    public Map<String, Object> analyzeMyWeakPoints(@ToolParam(description = "语言") String language) {
        Long uid = currentUserId();
        List<ErrorBook> errors = errorBookMapper.selectList(new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, uid).eq(ErrorBook::getMastered, false).orderByDesc(ErrorBook::getErrorCount));
        List<Map<String, Object>> weak = new ArrayList<>();
        for (ErrorBook e : errors) {
            WordBank w = wordBankMapper.selectById(e.getWordId());
            if (w != null && (language == null || w.getLanguage().equals(language))) {
                weak.add(Map.of("word", w.getWord(), "translation", w.getTranslation(), "errorCount", e.getErrorCount()));
                if (weak.size() >= 10) break;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("language", language != null ? language : "all"); r.put("weakWordCount", weak.size()); r.put("weakWords", weak);
        if (!weak.isEmpty()) r.put("suggestion", "建议重点复习: " + weak.stream().map(w -> (String) w.get("word")).collect(Collectors.joining(", ")));
        return r;
    }

    // ──────────── 以下方法同时被 Controller 和 AI tool 调用 ────────────

    @Tool(description = "生成单词并写入词库")
    public Map<String, Object> generateWords(
            @ToolParam(description = "主题") String topic,
            @ToolParam(description = "数量") Integer count,
            @ToolParam(description = "难度") Integer difficulty,
            @ToolParam(description = "语言") String language) {
        return doGenerateWords(topic, count, difficulty, language);
    }

    public Map<String, Object> doGenerateWords(String topic, Integer count, Integer difficulty, String language) {
        String lang = language != null ? language : "en";
        int n = count != null ? count : 10;
        int diff = difficulty != null ? difficulty : 2;
        String ts = topic != null ? topic : "日常";

        String langName = Map.of("en", "英语", "ja", "日语", "de", "德语").getOrDefault(lang, lang);
        String prompt = "请生成" + n + "个" + langName + "单词,主题\"" + ts + "\",难度" + diff + "/5。每个:word,phonetic,translation(中文),partOfSpeech,example,difficulty。只返回JSON数组:[{\"word\":\"...\",\"phonetic\":\"...\",\"translation\":\"...\",\"partOfSpeech\":\"...\",\"example\":\"...\",\"difficulty\":" + diff + "}]";

        String response = ChatClient.builder(chatModel).build().prompt().user(prompt).call().content();
        String json = response.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
        if (json.startsWith("[")) { json = json.substring(json.indexOf('[')); int end = json.lastIndexOf(']'); if (end > 0) json = json.substring(0, end + 1); }

        int created = 0, skipped = 0;
        List<String> generated = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> words = objectMapper.readValue(json, List.class);
            for (Map<String, Object> w : words) {
                String word = (String) w.get("word");
                if (word == null) continue;
                if (wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>().eq(WordBank::getLanguage, lang).eq(WordBank::getWord, word)) != null) { skipped++; continue; }
                WordBank wb = new WordBank();
                wb.setLanguage(lang); wb.setWord(word); wb.setTranslation((String) w.getOrDefault("translation", ""));
                wb.setPhonetic((String) w.get("phonetic")); wb.setPartOfSpeech((String) w.get("partOfSpeech"));
                wb.setExample((String) w.get("example")); wb.setCategory("AI生成");
                Object d = w.get("difficulty"); wb.setDifficulty(d instanceof Number ? ((Number) d).intValue() : diff);
                wordBankMapper.insert(wb); generated.add(word); created++;
            }
        } catch (Exception e) { log.error("Parse error", e); return Map.of("success", false, "error", "解析失败"); }
        return Map.of("success", true, "created", created, "skipped", skipped, "topic", ts, "words", generated);
    }

    @Tool(description = "生成句子并写入句库（单词不预翻译，hover时自动补全）")
    public Map<String, Object> generateSentences(
            @ToolParam(description = "主题") String topic,
            @ToolParam(description = "数量") Integer count,
            @ToolParam(description = "难度") Integer difficulty,
            @ToolParam(description = "语言") String language) {
        return doGenerateSentences(topic, count, difficulty, language);
    }

    public Map<String, Object> doGenerateSentences(String topic, Integer count, Integer difficulty, String language) {
        return doGenerateSentencesInternal(topic, count, difficulty, language);
    }

    /**
     * 生成一批句子（无数量上限），供 SSE 分批接口调用。
     * 返回 {"success":true,"created":N,"sentences":[...]} 不含外层 topic 等包装。
     */
    public Map<String, Object> doGenerateSentencesBatch(String topic, Integer count, Integer difficulty, String language) {
        return doGenerateSentencesInternal(topic, count, difficulty, language);
    }

    private Map<String, Object> doGenerateSentencesInternal(String topic, Integer count, Integer difficulty, String language) {
        String lang = language != null ? language : "en";
        int n = count != null ? count : 5;
        int diff = difficulty != null ? difficulty : 2;
        String ts = topic != null ? topic : "日常";

        String prompt = "生成" + n + "个英语句子,主题\"" + ts + "\",难度" + diff + "/5。每句:chinese(中文),english(英文)。自然流畅,8-15词。只返回JSON数组:[{\"chinese\":\"...\",\"english\":\"...\"}]";

        // 最多重试 2 次，处理 LLM 偶发的响应读取错误
        Exception lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String response = ChatClient.builder(chatModel).build().prompt().user(prompt).call().content();
                String json = response.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
                if (json.startsWith("[")) {
                    json = json.substring(json.indexOf('['));
                    int end = json.lastIndexOf(']');
                    if (end > 0) json = json.substring(0, end + 1);
                }

                int created = 0;
                List<Map<String, Object>> sentenceList = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = objectMapper.readValue(json, List.class);
                for (Map<String, Object> item : items) {
                    SentenceBank sb = new SentenceBank();
                    sb.setLanguage(lang);
                    sb.setChinese((String) item.get("chinese"));
                    sb.setEnglish((String) item.get("english"));
                    sb.setDifficulty(diff);
                    sentenceBankMapper.insert(sb);
                    sentenceList.add(Map.of("chinese", item.get("chinese"), "english", item.get("english")));
                    created++;
                }
                return Map.of("success", true, "created", created, "sentences", sentenceList);
            } catch (Exception e) {
                lastError = e;
                log.warn("Batch generate attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt < 1) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }
        log.error("Batch generate all attempts failed", lastError);
        return Map.of("success", false, "error", "生成失败: " + (lastError != null ? lastError.getMessage() : "未知错误"));
    }

    private boolean containsChinese(String s) {
        return s != null && s.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }

    // ──────────── SSE 分批生成 ────────────

    private static final int BATCH_SIZE = 5;
    private static final int MAX_TOTAL = 100;

    /**
     * 通过 SseEmitter 分批生成句子并推送进度。
     * 业务编排（分批循环、进度计算、结果聚合）都在这里，
     * Controller 只需一行调用。
     */
    public SseEmitter generateSentencesStream(String topic, int count, int diff, String lang) {
        final int total = Math.max(1, Math.min(count, MAX_TOTAL));
        final int totalBatches = (int) Math.ceil((double) total / BATCH_SIZE);

        SseEmitter emitter = new SseEmitter(600_000L);

        aiGenerateExecutor.execute(() -> {
            int totalCreated = 0;
            List<Map<String, Object>> allSentences = new ArrayList<>();
            try {
                for (int i = 0; i < totalBatches; i++) {
                    int remaining = total - totalCreated;
                    int thisBatch = Math.min(BATCH_SIZE, remaining);
                    if (thisBatch <= 0) break;

                    Map<String, Object> batchResult = doGenerateSentencesBatch(
                            topic, thisBatch, diff, lang);

                    if (Boolean.TRUE.equals(batchResult.get("success"))) {
                        int created = ((Number) batchResult.getOrDefault("created", 0)).intValue();
                        totalCreated += created;
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> sents =
                                (List<Map<String, Object>>) batchResult.get("sentences");
                        if (sents != null) allSentences.addAll(sents);

                        Map<String, Object> progress = new LinkedHashMap<>();
                        progress.put("type", "progress");
                        progress.put("done", totalCreated);
                        progress.put("total", total);
                        progress.put("batch", i + 1);
                        progress.put("totalBatches", totalBatches);
                        emitter.send(SseEmitter.event().name("progress").data(progress));
                    } else {
                        emitter.send(SseEmitter.event().name("error")
                                .data(Map.of("type", "error", "msg", batchResult.getOrDefault("error", "生成失败"))));
                        break;
                    }
                }

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("type", "done");
                done.put("created", totalCreated);
                done.put("topic", topic);
                done.put("sentences", allSentences);
                emitter.send(SseEmitter.event().name("done").data(done));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE generate stream error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
