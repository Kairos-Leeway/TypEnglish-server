package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.entity.*;
import com.typenglish.mapper.*;
import com.typenglish.security.JwtInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PracticeService {

    private final WordBankMapper wordBankMapper;
    private final ErrorBookMapper errorBookMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final SentenceErrorMapper sentenceErrorMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PracticeService(WordBankMapper wordBankMapper,
                           ErrorBookMapper errorBookMapper,
                           PracticeRecordMapper practiceRecordMapper,
                           SentenceErrorMapper sentenceErrorMapper) {
        this.wordBankMapper = wordBankMapper;
        this.errorBookMapper = errorBookMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.sentenceErrorMapper = sentenceErrorMapper;
    }

    public SessionResult createSession(String language, int count, String category) {
        Long userId = JwtInterceptor.CURRENT_USER.get();

        List<ErrorBook> dueErrors = errorBookMapper.findDueReviews(userId, count / 2);
        List<Long> errorWordIds = dueErrors.stream().map(ErrorBook::getWordId).toList();
        int remaining = count - dueErrors.size();

        List<WordBank> freshWords;
        if (remaining > 0) {
            if (category != null && !category.isEmpty()) {
                freshWords = wordBankMapper.randomPickByCategory(language, category, remaining);
            } else {
                freshWords = wordBankMapper.randomPickExcluding(language, errorWordIds, remaining);
            }
        } else {
            freshWords = List.of();
        }

        List<WordBank> sessionWords = new ArrayList<>();
        dueErrors.forEach(e -> {
            WordBank w = wordBankMapper.selectById(e.getWordId());
            if (w != null) sessionWords.add(w);
        });
        sessionWords.addAll(freshWords);

        return new SessionResult(sessionWords, dueErrors.size(), freshWords.size());
    }

    @Transactional
    public void submitAnswer(Long wordId, String mode, Boolean correct, String answer, String wordText, String language) {
        if (wordId == null && wordText != null && !wordText.isBlank() && language != null) {
            WordBank wb = wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>()
                    .eq(WordBank::getWord, wordText).eq(WordBank::getLanguage, language));
            if (wb != null) {
                wordId = wb.getId();
            } else {
                WordBank newWord = new WordBank();
                newWord.setLanguage(language);
                newWord.setWord(wordText);
                newWord.setTranslation(wordText);
                newWord.setDifficulty(1);
                newWord.setCategory("AI生成");
                wordBankMapper.insert(newWord);
                wordId = newWord.getId();
            }
        }
        if (wordId == null) return;

        Long userId = JwtInterceptor.CURRENT_USER.get();

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setWordId(wordId);
        record.setMode(mode);
        record.setCorrect(correct);
        record.setAnswer(answer);
        practiceRecordMapper.insert(record);

        LambdaQueryWrapper<ErrorBook> wrapper = new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId)
                .eq(ErrorBook::getWordId, wordId);
        ErrorBook existing = errorBookMapper.selectOne(wrapper);

        if (Boolean.FALSE.equals(correct)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextReview = now.plusDays(1);
            if (existing != null) {
                existing.setErrorCount(existing.getErrorCount() != null ? existing.getErrorCount() + 1 : 1);
                existing.setLastErrorAt(now);
                existing.setNextReviewAt(nextReview);
                existing.setMastered(false);
                errorBookMapper.updateById(existing);
            } else {
                ErrorBook eb = new ErrorBook();
                eb.setUserId(userId);
                eb.setWordId(wordId);
                eb.setErrorCount(1);
                eb.setLastErrorAt(now);
                eb.setNextReviewAt(nextReview);
                eb.setMastered(false);
                errorBookMapper.insert(eb);
            }
        } else {
            if (existing != null) {
                existing.setMastered(true);
                existing.setNextReviewAt(LocalDateTime.now().plusDays(3));
                errorBookMapper.updateById(existing);
            }
        }
    }

    /** 记录句子级错题 */
    public record SentenceErrorResult(Long id) {}
    public SentenceErrorResult recordSentenceError(Map<String, Object> body) {
        Long userId = JwtInterceptor.CURRENT_USER.get();
        SentenceError se = new SentenceError();
        se.setUserId(userId);
        se.setSentenceId(body.get("sentenceId") instanceof Number n ? n.longValue() : null);
        se.setEnglish((String) body.get("english"));
        se.setChinese((String) body.get("chinese"));
        se.setMode((String) body.get("mode"));
        se.setTotalSlots(body.get("totalSlots") instanceof Number n ? n.intValue() : 0);
        se.setCorrectSlots(body.get("correctSlots") instanceof Number n ? n.intValue() : 0);
        se.setMastered(false);
        try {
            se.setSlotResults(objectMapper.writeValueAsString(body.get("slotResults")));
        } catch (JsonProcessingException e) {
            se.setSlotResults("[]");
        }
        sentenceErrorMapper.insert(se);
        return new SentenceErrorResult(se.getId());
    }

    /** 练习统计 */
    public Map<String, Object> getStats() {
        Long userId = JwtInterceptor.CURRENT_USER.get();
        Map<String, Object> stats = practiceRecordMapper.stats(userId);
        List<Map<String, Object>> byMode = practiceRecordMapper.countByMode(userId);

        long total = ((Number) stats.getOrDefault("total", 0)).longValue();
        long correct = ((Number) stats.getOrDefault("correct", 0)).longValue();
        int accuracy = total > 0 ? (int) Math.round((double) correct / total * 100) : 0;

        return Map.of("total", total, "correct", correct, "accuracy", accuracy, "byMode", byMode);
    }

    public record SessionResult(List<WordBank> words, int fromErrorBook, int fromBank) {}
}
