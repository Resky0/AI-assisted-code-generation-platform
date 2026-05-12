package com.resky.yuaicodemother.ai;

import com.resky.yuaicodemother.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * AI 代码生成应用标题服务工厂
 *
 * @author yupi
 */
@Slf4j
@Configuration
@DependsOn("springContextUtil")
public class AiCodeGenAppNameServiceFactory {

    /**
     * 创建AI代码生成类型路由服务实例
     */
    public AiCodeGenAppNameService createAiCodeGenAppNameService() {
        // 动态获取多例的路由 ChatModel，支持并发
        ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenAppNameService.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 创建 AI代码生成应用标题服务实例
     */
    @Bean
    public AiCodeGenAppNameService aiCodeGenAppNameService() {
        return createAiCodeGenAppNameService();
    }
}
