package com.typenglish.service;

import com.typenglish.common.BusinessException;
import com.typenglish.entity.AiConversation;
import com.typenglish.mapper.AiConversationMapper;
import com.typenglish.mapper.AiConversationMessageMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.typenglish.entity.AiConversationMessage;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationServiceTest {
    private final AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
    private final AiConversationMessageMapper messageMapper = mock(AiConversationMessageMapper.class);
    private final ConversationService service = new ConversationService(conversationMapper, messageMapper);

    @Test
    void rejectsReadingConversationOwnedByAnotherUser() {
        when(conversationMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getMessages(42L, 7L));
        verifyNoInteractions(messageMapper);
    }

    @Test
    void rejectsWritingMessageBeforeInsertWhenOwnershipDoesNotMatch() {
        when(conversationMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.saveMessage(42L, 7L, "user", "hello"));
        verifyNoInteractions(messageMapper);
    }

    @Test
    void allowsOwnedConversation() {
        AiConversation conversation = new AiConversation();
        conversation.setId(42L);
        conversation.setUserId(7L);
        when(conversationMapper.selectOne(any())).thenReturn(conversation);

        service.getMessages(42L, 7L);

        verify(messageMapper).selectList(any());
    }

    @Test
    void persistsAndRestoresPracticeToolActionWithAssistantMessage() {
        AiConversation conversation = new AiConversation();
        conversation.setId(42L);
        conversation.setUserId(7L);
        when(conversationMapper.selectOne(any())).thenReturn(conversation);
        ArgumentCaptor<AiConversationMessage> saved = ArgumentCaptor.forClass(AiConversationMessage.class);

        Map<String, Object> action = Map.of("type", "start_practice", "route", "/practice");
        Map<String, Object> tool = Map.of("id", "tool-1", "phase", "done", "name",
                "createPracticeSession", "title", "练习已准备好", "action", action);
        service.saveMessage(42L, 7L, "assistant", "可以开始了", List.of(tool));
        verify(messageMapper).insert(saved.capture());
        when(messageMapper.selectList(any())).thenReturn(List.of(saved.getValue()));

        AiConversationMessage restored = service.getMessages(42L, 7L).get(0);

        assertEquals("可以开始了", restored.getContent());
        assertEquals("createPracticeSession", restored.getTools().get(0).get("name"));
        @SuppressWarnings("unchecked") Map<String, Object> restoredAction =
                (Map<String, Object>) restored.getTools().get(0).get("action");
        assertEquals("/practice", restoredAction.get("route"));
    }
}
