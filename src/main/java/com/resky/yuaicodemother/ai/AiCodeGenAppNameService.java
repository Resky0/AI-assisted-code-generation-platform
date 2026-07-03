package com.resky.yuaicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.Result;

public interface AiCodeGenAppNameService {
    /**
     * 总结对话标题
     * @param userMessage   用户消息
     * @return  appName
     */
    @SystemMessage(fromResource = "prompt/codegen-appname-summary-prompt.txt")
    Result<String> genAppName(String userMessage);
}
