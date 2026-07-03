package com.resky.yuaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AiUsageDailyVO {
    private LocalDate date;
    private long calls;
    private long successCalls;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
}
