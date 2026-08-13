package com.typenglish.service;

import com.typenglish.mapper.ErrorBookMapper;
import com.typenglish.mapper.PracticeRecordMapper;
import com.typenglish.mapper.SentenceErrorMapper;
import com.typenglish.mapper.WordBankMapper;
import com.typenglish.security.JwtInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PracticeServiceSessionTest {
    private final WordBankMapper wordBankMapper = mock(WordBankMapper.class);
    private final ErrorBookMapper errorBookMapper = mock(ErrorBookMapper.class);
    private final PracticeRecordMapper practiceRecordMapper = mock(PracticeRecordMapper.class);
    private final SentenceErrorMapper sentenceErrorMapper = mock(SentenceErrorMapper.class);
    private final PracticeService service = new PracticeService(wordBankMapper, errorBookMapper,
            practiceRecordMapper, sentenceErrorMapper, new SpacedRepetitionScheduler());

    @AfterEach
    void clearUser() {
        JwtInterceptor.CURRENT_USER.remove();
    }

    @Test
    void aiLanguageNameStillLoadsTheEnglishWordBank() {
        JwtInterceptor.CURRENT_USER.set(7L);
        when(errorBookMapper.findDueReviews(7L, 5)).thenReturn(List.of());
        when(wordBankMapper.randomPickExcluding("en", List.of(), 10)).thenReturn(List.of());

        var session = service.createSession("英语", 10, null);

        verify(wordBankMapper).randomPickExcluding("en", List.of(), 10);
        assertTrue(session.words().isEmpty());
    }
}
