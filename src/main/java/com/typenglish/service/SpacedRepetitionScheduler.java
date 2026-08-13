package com.typenglish.service;

import com.typenglish.entity.ErrorBook;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 集中管理错题的 SM-2 调度规则，不负责数据库读写。 */
@Component
public class SpacedRepetitionScheduler {

    public ReviewSchedule apply(ErrorBook item, int quality, LocalDateTime now) {
        int normalizedQuality = Math.max(0, Math.min(5, quality));
        double ef = item.getEasinessFactor() != null ? item.getEasinessFactor() : 2.5;
        int interval = item.getReviewInterval() != null ? item.getReviewInterval() : 0;
        int repetitions = item.getRepetitions() != null ? item.getRepetitions() : 0;

        if (normalizedQuality >= 3) {
            if (repetitions == 0) interval = 1;
            else if (repetitions == 1) interval = 6;
            else if (repetitions == 2) interval = 15;
            else if (repetitions == 3) interval = 30;
            else interval = Math.max(1, (int) Math.round(interval * ef));
            repetitions++;
        } else {
            interval = 1;
            repetitions = 0;
        }

        ef += 0.1 - (5 - normalizedQuality) * (0.08 + (5 - normalizedQuality) * 0.02);
        ef = Math.max(1.3, ef);

        item.setEasinessFactor(ef);
        item.setReviewInterval(interval);
        item.setRepetitions(repetitions);
        item.setNextReviewAt(now.plusDays(interval));
        item.setMastered(normalizedQuality >= 3 && repetitions >= 4);
        if (normalizedQuality < 3) {
            item.setLastErrorAt(now);
            item.setErrorCount((item.getErrorCount() != null ? item.getErrorCount() : 0) + 1);
        }
        return new ReviewSchedule(interval, item.getNextReviewAt(), ef, repetitions, item.getMastered());
    }

    public int quality(boolean correct, int attempts, boolean hintUsed, boolean skipped) {
        if (skipped) return 0;
        if (!correct) return attempts >= 3 ? 1 : 2;
        if (hintUsed) return 3;
        if (attempts <= 1) return 5;
        return 4;
    }

    public record ReviewSchedule(int intervalDays, LocalDateTime nextReviewAt,
                                 double easinessFactor, int repetitions, boolean mastered) {}
}
