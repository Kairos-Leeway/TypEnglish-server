package com.typenglish.service;

import com.typenglish.entity.WordBank;
import com.typenglish.mapper.ErrorBookMapper;
import com.typenglish.mapper.WordBankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class WordServiceLookupTest {
    private final WordBankMapper wordBankMapper = mock(WordBankMapper.class);
    private final ErrorBookMapper errorBookMapper = mock(ErrorBookMapper.class);
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
    private final OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
    private final AzureTranslatorService azureTranslator = mock(AzureTranslatorService.class);
    private WordService wordService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    void unknownWordUsesAzureResultAndPersistsItWithoutCallingAi() {
        Map<String, Object> azureResult = new LinkedHashMap<>();
        azureResult.put("found", true);
        azureResult.put("word", "quiet");
        azureResult.put("phonetic", "/kwaɪət/");
        azureResult.put("translation", "安静的");
        azureResult.put("partOfSpeech", "adj.");
        azureResult.put("example", "a quiet room (一个安静的房间)");
        when(azureTranslator.isConfigured()).thenReturn(true);
        when(azureTranslator.lookup("quiet", "en")).thenReturn(azureResult);
        when(wordBankMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = wordService.lookup("en", "quiet");

        assertEquals("安静的", result.get("translation"));
        verify(azureTranslator).lookup("quiet", "en");
        ArgumentCaptor<WordBank> insertedWord = ArgumentCaptor.forClass(WordBank.class);
        verify(wordBankMapper).insert(insertedWord.capture());
        assertEquals("quiet", insertedWord.getValue().getWord());
        assertEquals("词典查询", insertedWord.getValue().getCategory());
        verifyNoInteractions(chatModel);
    }

}
