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
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", eb.getId());
            m.put("errorCount", eb.getErrorCount());
            m.put("lastErrorAt", eb.getLastErrorAt());
            m.put("nextReviewAt", eb.getNextReviewAt());
            if (wb != null) {
                m.put("word", Map.of("id", wb.getId(), "word", wb.getWord(),
                        "translation", wb.getTranslation(), "phonetic", wb.getPhonetic(),
                        "language", wb.getLanguage()));
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
}
