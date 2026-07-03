package dev.langchain4j.service;

import dev.langchain4j.model.output.TokenUsage;

import java.time.Duration;

public final class TokenStreamLimitPolicy {
    private TokenStreamLimitPolicy() {}

    public static TokenStreamLimitException violation(TokenUsage usage, int completedToolRounds,
                                                       Duration elapsed, long maxTotalTokens,
                                                       int maxToolRounds, Duration maxDuration) {
        int total = usage == null || usage.totalTokenCount() == null ? 0 : usage.totalTokenCount();
        if (total >= maxTotalTokens) return new TokenStreamLimitException("单次任务 Token 上限已达到");
        if (completedToolRounds >= maxToolRounds) return new TokenStreamLimitException("工具调用轮数上限已达到");
        if (elapsed.compareTo(maxDuration) >= 0) return new TokenStreamLimitException("任务执行时间上限已达到");
        return null;
    }
}
