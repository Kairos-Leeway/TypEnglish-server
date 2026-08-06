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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);
    private final ErrorBookMapper errorBookMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final WordBankMapper wordBankMapper;
    private final SentenceBankMapper sentenceBankMapper;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiToolService(ErrorBookMapper errorBookMapper, PracticeRecordMapper practiceRecordMapper,
                         WordBankMapper wordBankMapper, SentenceBankMapper sentenceBankMapper,
                         OpenAiChatModel chatModel) {
        this.errorBookMapper = errorBookMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.wordBankMapper = wordBankMapper;
        this.sentenceBankMapper = sentenceBankMapper;
        this.chatModel = chatModel;
    }

    private Long currentUserId() {
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) throw new BusinessException(401, "未登录");
        return uid;
    }

    @Tool(description = "查询用户错题本")
    public List<Map<String, Object>> getMyErrorBook(@ToolParam(description = "语言代码") String language) {
        List<ErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<ErrorBook>().eq(ErrorBook::getUserId, currentUserId())
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
        int n = limit != null ? limit : 10;
        return practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, currentUserId()).orderByDesc(PracticeRecord::getCreatedAt).last("LIMIT " + n))
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
        List<ErrorBook> errors = errorBookMapper.selectList(new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, currentUserId()).eq(ErrorBook::getMastered, false).orderByDesc(ErrorBook::getErrorCount));
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

    @Tool(description = "生成单词并写入词库")
    public Map<String, Object> generateWords(
            @ToolParam(description = "主题") String topic,
            @ToolParam(description = "数量") Integer count,
            @ToolParam(description = "难度") Integer difficulty,
            @ToolParam(description = "语言") String language) {

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

        String lang = language != null ? language : "en";
        int n = count != null ? Math.min(count, 10) : 5;
        int diff = difficulty != null ? difficulty : 2;
        String ts = topic != null ? topic : "日常";

        String prompt = "生成" + n + "个英语句子,主题\"" + ts + "\",难度" + diff + "/5。每句:chinese(中文),english(英文)。自然流畅,8-15词。只返回JSON数组:[{\"chinese\":\"...\",\"english\":\"...\"}]";

        String response = ChatClient.builder(chatModel).build().prompt().user(prompt).call().content();
        String json = response.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
        if (json.startsWith("[")) { json = json.substring(json.indexOf('[')); int end = json.lastIndexOf(']'); if (end > 0) json = json.substring(0, end + 1); }

        int created = 0;
        List<Map<String, Object>> sentenceList = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = objectMapper.readValue(json, List.class);

            for (Map<String, Object> item : items) {
                SentenceBank sb = new SentenceBank();
                sb.setLanguage(lang); sb.setChinese((String) item.get("chinese"));
                sb.setEnglish((String) item.get("english")); sb.setDifficulty(diff);
                sentenceBankMapper.insert(sb);
                sentenceList.add(Map.of("chinese", item.get("chinese"), "english", item.get("english")));
                created++;
            }
        } catch (Exception e) { log.error("Parse error", e); return Map.of("success", false, "error", "解析失败"); }
        return Map.of("success", true, "created", created, "topic", ts, "sentences", sentenceList);
    }

    /** 解析 AI 返回的句子 JSON，失败时用正则兜底提取（保留供其他功能使用） */
    @SuppressWarnings("unused")
    private Map<String, Map<String, String>> parseWordDetails(String json) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        // 1. 尝试 JSON 解析
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = objectMapper.readValue(json, List.class);
            for (Map<String, Object> w : list) {
                String word = (String) w.get("word");
                if (word == null) continue;
                Map<String, String> detail = new LinkedHashMap<>();
                Object phonetic = w.get("phonetic");
                if (phonetic != null && !phonetic.toString().isBlank()) detail.put("phonetic", phonetic.toString());
                Object trans = w.get("translation");
                // 确保翻译不是英文（如果翻译里只有英文/数字/标点，大概率是 AI 没翻）
                if (trans != null && !trans.toString().isBlank() && containsChinese(trans.toString())) {
                    detail.put("translation", trans.toString());
                }
                Object pos = w.get("partOfSpeech");
                if (pos != null && !pos.toString().isBlank()) detail.put("partOfSpeech", pos.toString());
                if (!detail.isEmpty()) result.put(word.toLowerCase(), detail);
            }
            if (!result.isEmpty()) return result;
        } catch (Exception e) { log.warn("JSON parse failed: {}", e.getMessage()); }

        // 2. 正则兜底：尝试逐行提取 "word" / "phonetic" / "translation" / "partOfSpeech"
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"word\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"translation\"\\s*:\\s*\"([^\"]+)\""
            ).matcher(json);
            while (m.find()) {
                String w = m.group(1);
                String trans = m.group(2);
                if (w != null && trans != null && containsChinese(trans)) {
                    result.putIfAbsent(w.toLowerCase(), new LinkedHashMap<>(Map.of("translation", trans)));
                }
            }
        } catch (Exception e) { log.warn("Regex fallback failed: {}", e.getMessage()); }
        return result;
    }

    /** 检查字符串是否包含中文字符 */
    private boolean containsChinese(String s) {
        return s != null && s.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }
}
