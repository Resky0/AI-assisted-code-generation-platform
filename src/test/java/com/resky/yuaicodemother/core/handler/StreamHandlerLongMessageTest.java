package com.resky.yuaicodemother.core.handler;

import cn.hutool.json.JSONUtil;
import com.resky.yuaicodemother.ai.model.message.AiResponseMessage;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.resky.yuaicodemother.service.ChatHistoryService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StreamHandlerLongMessageTest {

    private static final long APP_ID = 1L;
    private static final long USER_ID = 2L;

    @Test
    void simpleTextHandlerShouldPersistCompleteLongMessage() {
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        User user = createUser();
        String firstChunk = "html-中文-😀".repeat(3_000);
        String secondChunk = "css-样式-🚀".repeat(3_000);
        String expected = firstChunk + secondChunk;
        assertTrue(expected.getBytes(StandardCharsets.UTF_8).length > 65_535);

        List<String> output = new SimpleTextStreamHandler()
                .handle(Flux.just(firstChunk, secondChunk), chatHistoryService, APP_ID, user)
                .collectList()
                .block();

        assertEquals(List.of(firstChunk, secondChunk), output);
        verify(chatHistoryService).addChatMessage(
                APP_ID, expected, ChatHistoryMessageTypeEnum.AI.getValue(), USER_ID);
    }

    @Test
    void jsonHandlerShouldPersistCompleteLongMessage() {
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        User user = createUser();
        String firstChunk = "vue-组件-😀".repeat(3_000);
        String secondChunk = "tool-result-结果-🚀".repeat(3_000);
        String expected = firstChunk + secondChunk;
        assertTrue(expected.getBytes(StandardCharsets.UTF_8).length > 65_535);
        Flux<String> input = Flux.just(
                JSONUtil.toJsonStr(new AiResponseMessage(firstChunk)),
                JSONUtil.toJsonStr(new AiResponseMessage(secondChunk)));

        List<String> output = new JsonMessageStreamHandler()
                .handle(input, chatHistoryService, APP_ID, user)
                .collectList()
                .block();

        assertEquals(List.of(firstChunk, secondChunk), output);
        verify(chatHistoryService).addChatMessage(
                APP_ID, expected, ChatHistoryMessageTypeEnum.AI.getValue(), USER_ID);
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }
}
