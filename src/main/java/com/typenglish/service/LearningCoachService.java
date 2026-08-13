package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.entity.ErrorBook;
import com.typenglish.entity.SentenceError;
import com.typenglish.mapper.ErrorBookMapper;
import com.typenglish.mapper.PracticeRecordMapper;
import com.typenglish.mapper.SentenceErrorMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 用户学习决策的统一入口，AI 工具只负责把这些结果暴露给模型。 */
@Service
public class LearningCoachService {
    private final ErrorBookMapper errorBookMapper;
    private final SentenceErrorMapper sentenceErrorMapper;
    private final PracticeRecordMapper practiceRecordMapper;

    public LearningCoachService(ErrorBookMapper errorBookMapper,
                                SentenceErrorMapper sentenceErrorMapper,
                                PracticeRecordMapper practiceRecordMapper) {
        this.errorBookMapper = errorBookMapper;
        this.sentenceErrorMapper = sentenceErrorMapper;
        this.practiceRecordMapper = practiceRecordMapper;
    }

    public Map<String, Object> getLearningTrend(Long userId, Integer days) {
        int range = Math.max(7, Math.min(days != null ? days : 30, 90));
        List<Map<String, Object>> raw = practiceRecordMapper.dailyStats(userId, range);
        List<Map<String, Object>> points = raw.stream().map(row -> {
            long count = number(row.get("count"));
            long correct = number(row.get("correct"));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", String.valueOf(row.get("date")));
            point.put("count", count);
            point.put("correct", correct);
            point.put("accuracy", count > 0 ? Math.round(correct * 100.0 / count) : 0);
            return point;
        }).toList();

        long total = points.stream().mapToLong(p -> number(p.get("count"))).sum();
        long correct = points.stream().mapToLong(p -> number(p.get("correct"))).sum();
        int streak = calculateStreak(points);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", range);
        result.put("total", total);
        result.put("correct", correct);
        result.put("accuracy", total > 0 ? Math.round(correct * 100.0 / total) : 0);
        result.put("streakDays", streak);
        result.put("points", points);
        result.put("summary", total == 0 ? "最近还没有练习记录" :
                "近 " + range + " 天练习 " + total + " 题，正确率 " + result.get("accuracy") + "%");
        return result;
    }

    public Map<String, Object> recommendNextSession(Long userId, String language) {
        String lang = LanguageCodeNormalizer.normalize(language);
        long due = errorBookMapper.selectCount(new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId).eq(ErrorBook::getMastered, false)
                .le(ErrorBook::getNextReviewAt, LocalDateTime.now()));
        long activeErrors = errorBookMapper.selectCount(new LambdaQueryWrapper<ErrorBook>()
                .eq(ErrorBook::getUserId, userId).eq(ErrorBook::getMastered, false));
        long sentenceErrors = sentenceErrorMapper.selectCount(new LambdaQueryWrapper<SentenceError>()
                .eq(SentenceError::getUserId, userId).eq(SentenceError::getMastered, false));
        Map<String, Object> trend = getLearningTrend(userId, 14);

        int count = (int) Math.max(8, Math.min(20, due > 0 ? due + 4 : 10));
        String mode = due > 0 || activeErrors >= sentenceErrors ? "typing" : "cloze";
        String reason = due > 0
                ? "有 " + due + " 个单词已到复习时间，优先巩固再加入少量新词"
                : sentenceErrors > activeErrors
                    ? "句子错题较多，建议用完形填空训练语境与搭配"
                    : "当前没有到期复习，适合进行一轮短时新词练习";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("language", lang);
        result.put("mode", mode);
        result.put("count", count);
        result.put("dueReviews", due);
        result.put("activeWordErrors", activeErrors);
        result.put("activeSentenceErrors", sentenceErrors);
        result.put("recentAccuracy", trend.get("accuracy"));
        result.put("reason", reason);
        return result;
    }

    public Map<String, Object> createPracticeSession(Long userId, String language,
                                                      Integer count, String preferredMode) {
        Map<String, Object> recommendation = recommendNextSession(userId, language);
        int safeCount = Math.max(5, Math.min(count != null ? count :
                ((Number) recommendation.get("count")).intValue(), 50));
        String mode = preferredMode == null || preferredMode.isBlank()
                ? String.valueOf(recommendation.get("mode")) : preferredMode;
        String route = "cloze".equals(mode) ? "/practice/cloze" : "/practice";

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "start_practice");
        action.put("label", "开始这轮练习");
        action.put("route", route);
        action.put("query", Map.of(
                "language", recommendation.get("language"),
                "count", safeCount,
                "coach", "1"));

        Map<String, Object> result = new LinkedHashMap<>(recommendation);
        result.put("count", safeCount);
        result.put("mode", mode);
        result.put("action", action);
        result.put("summary", "已准备 " + safeCount + " 题的个性化练习");
        return result;
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0;
    }

    private int calculateStreak(List<Map<String, Object>> points) {
        Set<LocalDate> dates = new HashSet<>();
        for (Map<String, Object> point : points) {
            try { dates.add(LocalDate.parse(String.valueOf(point.get("date")))); }
            catch (Exception ignored) {}
        }
        LocalDate cursor = LocalDate.now();
        if (!dates.contains(cursor)) cursor = cursor.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }
        return streak;
    }
}
