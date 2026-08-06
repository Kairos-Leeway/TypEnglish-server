package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typenglish.common.PageResult;
import com.typenglish.entity.WordBank;
import com.typenglish.mapper.WordBankMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 词库管理 — Redis 缓存 → DB → AI 多级查询
 */
@Service
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);
    private final WordBankMapper wordBankMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WordService(WordBankMapper wordBankMapper, RedisTemplate<String, Object> redisTemplate,
                       OpenAiChatModel chatModel) {
        this.wordBankMapper = wordBankMapper;
        this.redisTemplate = redisTemplate;
        this.chatModel = chatModel;
    }

    /** 分页查询词库 */
    public PageResult<WordBank> list(String language, int page, int size, Integer difficulty) {
        LambdaQueryWrapper<WordBank> wrapper = new LambdaQueryWrapper<WordBank>()
                .eq(WordBank::getLanguage, language);
        if (difficulty != null) {
            wrapper.eq(WordBank::getDifficulty, difficulty);
        }
        wrapper.orderByAsc(WordBank::getWord);

        // 用 MyBatis-Plus 分页
        Page<WordBank> mpPage = new Page<>(page, size);
        Page<WordBank> result = wordBankMapper.selectPage(mpPage, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    /** 单词快速查询（Redis → DB → AI） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> lookup(String language, String word) {
        String cacheKey = "word:" + language + ":" + word.toLowerCase();
        String cacheKeyMiss = "word:miss:" + language + ":" + word.toLowerCase();

        // 1. 命中缺失标记
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKeyMiss))) {
            return Map.of("found", false, "word", word);
        }

        // 2. Redis 缓存（检查是否残缺，残缺则穿透去 DB 走 AI 补全）
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !isIncomplete(cached)) {
            return cached;
        }
        // 缓存残缺，删掉重走 DB 路径
        if (cached != null) {
            redisTemplate.delete(cacheKey);
        }

        // 3. 数据库
        WordBank wb = wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>()
                .eq(WordBank::getLanguage, language)
                .eq(WordBank::getWord, word));
        if (wb != null) {
            boolean needsAiFill = isBlank(wb.getPhonetic()) || isBlank(wb.getTranslation()) || isBlank(wb.getPartOfSpeech()) || isBlank(wb.getExample());
            if (needsAiFill) {
                Map<String, Object> fill = lookupByAi(word, language);
                if (fill != null) {
                    boolean updated = false;
                    if (isBlank(wb.getPhonetic()) && fill.get("phonetic") instanceof String s && !s.isBlank()) { wb.setPhonetic(s); updated = true; }
                    if (isBlank(wb.getTranslation()) && fill.get("translation") instanceof String s && !s.isBlank()) { wb.setTranslation(s); updated = true; }
                    if (isBlank(wb.getPartOfSpeech()) && fill.get("partOfSpeech") instanceof String s && !s.isBlank()) { wb.setPartOfSpeech(s); updated = true; }
                    if (isBlank(wb.getExample()) && fill.get("example") instanceof String s && !s.isBlank()) { wb.setExample(s); updated = true; }
                    if (updated) {
                        try { wordBankMapper.updateById(wb); } catch (Exception ignored) {}
                        redisTemplate.delete(cacheKey);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("found", true);
            result.put("word", wb.getWord());
            result.put("phonetic", wb.getPhonetic());
            result.put("translation", wb.getTranslation());
            result.put("partOfSpeech", wb.getPartOfSpeech());
            result.put("example", wb.getExample());
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofDays(7));
            return result;
        }

        // 4. AI 查询并入库
        Map<String, Object> aiResult = lookupByAi(word, language);
        if (aiResult != null) {
            WordBank nw = new WordBank();
            nw.setLanguage(language); nw.setWord(word);
            nw.setPhonetic((String) aiResult.get("phonetic"));
            nw.setTranslation((String) aiResult.get("translation"));
            nw.setPartOfSpeech((String) aiResult.get("partOfSpeech"));
            nw.setExample((String) aiResult.get("example"));
            nw.setCategory("AI生成");
            nw.setDifficulty(1);
            try { wordBankMapper.insert(nw); } catch (Exception ignored) {}
            redisTemplate.opsForValue().set(cacheKey, aiResult, Duration.ofDays(7));
            redisTemplate.delete(cacheKeyMiss);
            return aiResult;
        }

        // 5. 标记缺失防穿透
        redisTemplate.opsForValue().set(cacheKeyMiss, "", Duration.ofMinutes(5));
        return Map.of("found", false, "word", word);
    }

    /** 批量导入单词 */
    public record ImportResult(int created, int skipped) {}
    public ImportResult importWords(List<Map<String, Object>> words, String language) {
        int created = 0, skipped = 0;
        for (Map<String, Object> w : words) {
            String word = (String) w.get("word");
            String translation = (String) w.get("translation");
            if (word == null || translation == null) continue;

            var exists = wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>()
                    .eq(WordBank::getLanguage, language)
                    .eq(WordBank::getWord, word));
            if (exists != null) { skipped++; continue; }

            WordBank wb = new WordBank();
            wb.setLanguage(language);
            wb.setWord(word);
            wb.setTranslation(translation);
            wb.setPhonetic((String) w.get("phonetic"));
            wb.setPartOfSpeech((String) w.get("partOfSpeech"));
            wb.setExample((String) w.get("example"));
            wb.setDifficulty(w.get("difficulty") instanceof Number n ? n.intValue() : 1);
            wordBankMapper.insert(wb);
            created++;
        }
        return new ImportResult(created, skipped);
    }

    /** 删除单个单词 */
    public void deleteById(Long id) {
        wordBankMapper.deleteById(id);
    }

    /** 批量删除整个分类 */
    public long deleteByCategory(String category, String language) {
        if (category == null || category.isBlank()) return 0;
        var wrapper = new LambdaQueryWrapper<WordBank>()
                .eq(WordBank::getLanguage, language)
                .eq(WordBank::getCategory, category);
        long count = wordBankMapper.selectCount(wrapper);
        wordBankMapper.delete(wrapper);
        return count;
    }

    // ---- private ----

    private Map<String, Object> lookupByAi(String word, String language) {
        try {
            String prompt = "你是一个词典专家。请查询英语单词\"" + word + "\"并提供：word(原词)、phonetic(国际音标，如 /ˈæp.əl/)、translation(中文翻译)、partOfSpeech(词性，如 n./v./adj./adv.)、example(一个完整英文例句+中文翻译)。只返回JSON对象，不要解释。示例：{\"word\":\"apple\",\"phonetic\":\"/ˈæp.əl/\",\"translation\":\"苹果\",\"partOfSpeech\":\"n.\",\"example\":\"I ate a red apple this morning. (我今天早上吃了一个红苹果。)\"}";
            String resp = ChatClient.builder(chatModel).build().prompt().user(prompt).call().content();
            if (resp == null) return null;
            String json = resp.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            String trans = (String) result.get("translation");
            if (trans != null && trans.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF)) {
                result.put("found", true);
                result.putIfAbsent("word", word);
                return result;
            }
        } catch (Exception e) {
            log.warn("AI lookup failed for '{}': {}", word, e.getMessage());
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 缓存数据是否残缺（缺音标/翻译/词性/例句） */
    private static boolean isIncomplete(Map<String, Object> cached) {
        return isBlank((String) cached.get("phonetic"))
                || isBlank((String) cached.get("translation"))
                || isBlank((String) cached.get("partOfSpeech"))
                || isBlank((String) cached.get("example"));
    }
}
