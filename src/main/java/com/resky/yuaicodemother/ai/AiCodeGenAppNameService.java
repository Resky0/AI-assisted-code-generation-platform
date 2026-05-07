package com.resky.yuaicodemother.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeGenAppNameService {
    /**
     * 总结对话标题
     * @param userMessage   用户消息
     * @return  appName
     */
    @SystemMessage(fromResource = "prompt/codegen-appname-summary-prompt.txt")
    String genAppName(String userMessage);
}
