package com.resky.yuaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成应用标题服务工厂
 *
 * @author yupi
 */
@Slf4j
@Configuration
public class AiCodeGenAppNameServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AI代码生成应用标题服务实例
     */
    @Bean
    public AiCodeGenAppNameService aiCodeGenAppNameService() {
        return AiServices.builder(AiCodeGenAppNameService.class)
                .chatModel(chatModel)
                .build();
    }
}
