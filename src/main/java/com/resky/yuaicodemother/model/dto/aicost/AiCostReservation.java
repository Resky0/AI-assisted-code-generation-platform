package com.resky.yuaicodemother.model.dto.aicost;

import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import lombok.Data;

import java.time.Instant;

@Data
public class AiCostReservation {
    private String traceId;
    private long userId;
    private long appId;
    private AiUsageCallTypeEnum callType;
    private boolean admin;
    private String requestCountKey;
    private String globalPermitId;
    private String userPermitId;
    private long maxTokens;
    private int maxToolRounds;
    private Instant startedAt;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private int toolRounds;
    private boolean finished;
    private boolean budgetExceeded;
}
