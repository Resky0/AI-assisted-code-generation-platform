package com.resky.yuaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiUsageSummaryVO {
    private long globalDailyBudget;
    private long todayTokens;
    private double budgetUsageRate;
    private long totalCalls;
    private long successCalls;
    private double successRate;
    private long inputTokens;
    private long outputTokens;
    private List<AiUsageDailyVO> daily;
}
