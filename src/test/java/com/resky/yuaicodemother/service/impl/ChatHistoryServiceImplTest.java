package com.resky.yuaicodemother.service.impl;

import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.model.entity.ChatHistory;
import com.resky.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ChatHistoryServiceImplTest {

    @Test
    void shouldPreserveMessageLargerThanTextLimit() {
        ChatHistoryServiceImpl service = spy(new ChatHistoryServiceImpl());
        doReturn(true).when(service).save(any(ChatHistory.class));
        String message = "code-中文-😀\n".repeat(6_000);
        assertTrue(message.getBytes(StandardCharsets.UTF_8).length > 65_535);

        boolean saved = service.addChatMessage(
                1L, message, ChatHistoryMessageTypeEnum.AI.getValue(), 2L);

        ArgumentCaptor<ChatHistory> captor = ArgumentCaptor.forClass(ChatHistory.class);
        verify(service).save(captor.capture());
        assertTrue(saved);
        assertEquals(message, captor.getValue().getMessage());
    }

    @Test
    void shouldRejectMessageOverEightMiBByUtf8Bytes() {
        ChatHistoryServiceImpl service = new ChatHistoryServiceImpl();
        String message = "😀".repeat(2_097_153);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.addChatMessage(
                        1L, message, ChatHistoryMessageTypeEnum.AI.getValue(), 2L));

        assertEquals("聊天消息过长，UTF-8 编码后不能超过 8 MiB", exception.getMessage());
    }
}
