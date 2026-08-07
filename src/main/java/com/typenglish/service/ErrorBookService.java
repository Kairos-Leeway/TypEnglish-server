package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.typenglish.common.BusinessException;
import com.typenglish.common.PageResult;
import com.typenglish.entity.ErrorBook;
import com.typenglish.entity.SentenceError;
import com.typenglish.entity.WordBank;
import com.typenglish.mapper.ErrorBookMapper;
import com.typenglish.mapper.SentenceErrorMapper;
import com.typenglish.mapper.WordBankMapper;
import com.typenglish.security.JwtInterceptor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ErrorBookService {

    private final ErrorBookMapper errorBookMapper;
    private final WordBankMapper wordBankMapper;
    private final SentenceErrorMapper sentenceErrorMapper;

    public ErrorBookService(ErrorBookMapper errorBookMapper, WordBankMapper wordBankMapper,
                            SentenceErrorMapper sentenceErrorMapper) {
        this.errorBookMapper = errorBookMapper;
        this.wordBankMapper = wordBankMapper;
        this.sentenceErrorMapper = sentenceErrorMapper;
    }

    /** 单词错题分页 */
    public PageResult<Map<String, Object>> listWords(int page, int size) {
        Long userId = currentUserId();

        var wrapper = new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId)
                .eq(ErrorBook::getMastered, false)
                .orderByDesc(ErrorBook::getLastErrorAt);

        Page<ErrorBook> mpPage = new Page<>(page, size);
        Page<ErrorBook> result = errorBookMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ErrorBook eb : result.getRecords()) {
            WordBank wb = wordBankMapper.selectById(eb.getWordId());
            if (wb == null) {
                // 词已删除，标记该错题记录为已掌握，不再返回
                eb.setMastered(true);
                errorBookMapper.updateById(eb);
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", eb.getId());
            m.put("errorCount", eb.getErrorCount());
            m.put("lastErrorAt", eb.getLastErrorAt());
            m.put("nextReviewAt", eb.getNextReviewAt());
            if (wb != null) {
                Map<String, Object> wordMap = new LinkedHashMap<>();
                wordMap.put("id", wb.getId());
                wordMap.put("word", wb.getWord());
                wordMap.put("translation", wb.getTranslation());
                wordMap.put("phonetic", wb.getPhonetic());
                wordMap.put("language", wb.getLanguage());
                m.put("word", wordMap);
            }
            items.add(m);
        }

        return new PageResult<>(items, result.getTotal(), page, size);
    }

    /** 句子错题分页 */
    public PageResult<Map<String, Object>> listSentences(int page, int size) {
        Long userId = currentUserId();
        var wrapper = new LambdaQueryWrapper<SentenceError>()
                .eq(SentenceError::getUserId, userId)
                .eq(SentenceError::getMastered, false)
                .orderByDesc(SentenceError::getCreatedAt);

        Page<SentenceError> mpPage = new Page<>(page, size);
        Page<SentenceError> result = sentenceErrorMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> items = new ArrayList<>();
        for (SentenceError se : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", se.getId());
            m.put("sentenceId", se.getSentenceId());
            m.put("english", se.getEnglish());
            m.put("chinese", se.getChinese());
            m.put("mode", se.getMode());
            m.put("slotResults", se.getSlotResults());
            m.put("totalSlots", se.getTotalSlots());
            m.put("correctSlots", se.getCorrectSlots());
            m.put("createdAt", se.getCreatedAt());
            items.add(m);
        }
        return new PageResult<>(items, result.getTotal(), page, size);
    }

    /** 标记单词错题已掌握 */
    public void markWordMastered(Long id) {
        Long userId = currentUserId();
        ErrorBook eb = errorBookMapper.selectOne(
                new LambdaQueryWrapper<ErrorBook>().eq(ErrorBook::getId, id).eq(ErrorBook::getUserId, userId));
        if (eb != null) {
            eb.setMastered(true);
            errorBookMapper.updateById(eb);
        }
    }

    /** 标记句子错题已掌握 */
    public void markSentenceMastered(Long id) {
        Long userId = currentUserId();
        SentenceError se = sentenceErrorMapper.selectOne(
                new LambdaQueryWrapper<SentenceError>().eq(SentenceError::getId, id).eq(SentenceError::getUserId, userId));
        if (se != null) {
            se.setMastered(true);
            sentenceErrorMapper.updateById(se);
        }
    }

    /** 清空单词错题 */
    public long clearAllWords() {
        Long userId = currentUserId();
        var wrapper = new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId).eq(ErrorBook::getMastered, false);
        List<ErrorBook> list = errorBookMapper.selectList(wrapper);
        for (ErrorBook eb : list) {
            eb.setMastered(true);
            errorBookMapper.updateById(eb);
        }
        return list.size();
    }

    /** 清空句子错题 */
    public long clearAllSentences() {
        Long userId = currentUserId();
        var wrapper = new LambdaQueryWrapper<SentenceError>()
                .eq(SentenceError::getUserId, userId).eq(SentenceError::getMastered, false);
        List<SentenceError> list = sentenceErrorMapper.selectList(wrapper);
        for (SentenceError se : list) {
            se.setMastered(true);
            sentenceErrorMapper.updateById(se);
        }
        return list.size();
    }

    /** 更新句子错题(重新练习后覆盖结果) */
    public boolean updateSentenceError(Long id, int correctSlots, int totalSlots, String slotResults) {
        Long userId = currentUserId();
        SentenceError se = sentenceErrorMapper.selectOne(
                new LambdaQueryWrapper<SentenceError>().eq(SentenceError::getId, id).eq(SentenceError::getUserId, userId));
        if (se == null) throw new BusinessException(404, "not found");

        se.setCorrectSlots(correctSlots);
        se.setTotalSlots(totalSlots);
        try {
            se.setSlotResults(slotResults);
        } catch (Exception ignored) {}
        if (correctSlots >= totalSlots && totalSlots > 0) {
            se.setMastered(true);
        }
        sentenceErrorMapper.updateById(se);
        return se.getMastered();
    }

    private Long currentUserId() {
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) throw new BusinessException(401, "未登录");
        return uid;
    }

    // ──────────── SM-2 间隔重复算法 ────────────

    /**
     * 根据答题结果自动计算 SM-2 quality，Controller 只需传 errorBookId + correct。
     *
     * 自动评分规则：
     *   errorCount==1 且正确  → quality=5 (首次复习就对了 = 完美)
     *   errorCount==1 且错误  → quality=2 (遗忘了)
     *   errorCount>=2 且正确 → quality=4 (吃力但对了)
     *   errorCount>=2 且错误 → quality=1 (还在错)
     */
    public Map<String, Object> autoReview(Long errorBookId, boolean correct) {
        Long userId = currentUserId();
        ErrorBook item = errorBookMapper.selectById(errorBookId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(404, "错题不存在");
        }

        int errCount = item.getErrorCount() != null ? item.getErrorCount() : 1;
        int quality;
        if (correct && errCount <= 1) {
            quality = 5;  // 首次复习就对了
        } else if (correct) {
            quality = 4;  // 多次后才对
        } else if (errCount <= 1) {
            quality = 2;  // 首次复习就忘了
        } else {
            quality = 1;  // 多次还是错
        }

        int nextInterval = applySm2(item, quality);
        return Map.of(
                "nextInterval", nextInterval,
                "nextReviewAt", item.getNextReviewAt().toString(),
                "ef", item.getEasinessFactor(),
                "repetitions", item.getRepetitions(),
                "quality", quality
        );
    }

    public int applySm2(ErrorBook item, int quality) {
        double ef = item.getEasinessFactor() != null ? item.getEasinessFactor() : 2.5;
        int interval = item.getReviewInterval() != null ? item.getReviewInterval() : 0;
        int reps = item.getRepetitions() != null ? item.getRepetitions() : 0;

        if (quality >= 3) {
            // 答对了：间隔递增
            if (reps == 0) {
                interval = 1;
            } else if (reps == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * ef);
            }
            reps++;
        } else {
            // 答错了：重置
            interval = 1;
            reps = 0;
        }

        // 更新 EF
        ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (ef < 1.3) ef = 1.3;

        item.setEasinessFactor(ef);
        item.setReviewInterval(interval);
        item.setRepetitions(reps);
        item.setNextReviewAt(java.time.LocalDateTime.now().plusDays(interval));
        item.setLastErrorAt(java.time.LocalDateTime.now());
        if (quality < 3) {
            item.setErrorCount((item.getErrorCount() != null ? item.getErrorCount() : 0) + 1);
        }

        errorBookMapper.updateById(item);
        return interval;
    }

    /**
     * 获取待复习的错题（nextReviewAt <= now，且未掌握），按复习时间升序。
     */
    public List<Map<String, Object>> getDueReviews(Long userId) {
        var wrapper = new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId)
                .eq(ErrorBook::getMastered, false)
                .le(ErrorBook::getNextReviewAt, java.time.LocalDateTime.now())
                .orderByAsc(ErrorBook::getNextReviewAt);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ErrorBook eb : errorBookMapper.selectList(wrapper)) {
            WordBank wb = wordBankMapper.selectById(eb.getWordId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("errorId", eb.getId());
            m.put("errorCount", eb.getErrorCount());
            m.put("ef", eb.getEasinessFactor());
            m.put("interval", eb.getReviewInterval());
            m.put("repetitions", eb.getRepetitions());
            if (wb != null) {
                Map<String, Object> wordMap = new LinkedHashMap<>();
                wordMap.put("id", wb.getId());
                wordMap.put("word", wb.getWord());
                wordMap.put("translation", wb.getTranslation());
                wordMap.put("phonetic", wb.getPhonetic());
                m.put("word", wordMap);
            }
            items.add(m);
        }
        return items;
    }
}
