package com.typenglish.service;

import com.typenglish.entity.ErrorBook;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SpacedRepetitionSchedulerTest {
    private final SpacedRepetitionScheduler scheduler = new SpacedRepetitionScheduler();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Test
    void growsIntervalsAndMastersOnlyAfterFourSuccessfulReviews() {
        ErrorBook item = new ErrorBook();
        item.setErrorCount(1);

        assertEquals(1, scheduler.apply(item, 5, now).intervalDays());
        assertFalse(item.getMastered());
        assertEquals(6, scheduler.apply(item, 5, now).intervalDays());
        assertFalse(item.getMastered());
        assertEquals(15, scheduler.apply(item, 5, now).intervalDays());
        assertFalse(item.getMastered());
        assertEquals(30, scheduler.apply(item, 5, now).intervalDays());
        assertTrue(item.getMastered());
    }

    @Test
    void skipResetsProgressAndSchedulesTomorrow() {
        ErrorBook item = new ErrorBook();
        item.setErrorCount(2);
        item.setRepetitions(3);
        item.setReviewInterval(15);
        item.setEasinessFactor(2.5);
        item.setMastered(true);

        var schedule = scheduler.apply(item, scheduler.quality(false, 0, false, true), now);

        assertEquals(1, schedule.intervalDays());
        assertEquals(0, schedule.repetitions());
        assertFalse(schedule.mastered());
        assertEquals(3, item.getErrorCount());
        assertEquals(now.plusDays(1), item.getNextReviewAt());
    }

    @Test
    void qualityReflectsAttemptsHintsAndSkipping() {
        assertEquals(5, scheduler.quality(true, 1, false, false));
        assertEquals(4, scheduler.quality(true, 2, false, false));
        assertEquals(3, scheduler.quality(true, 1, true, false));
        assertEquals(1, scheduler.quality(false, 3, false, false));
        assertEquals(0, scheduler.quality(false, 0, false, true));
    }
}
