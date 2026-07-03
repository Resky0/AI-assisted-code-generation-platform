package com.resky.yuaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiQuotaVO {
    private boolean serviceAvailable;
    private boolean admin;
    private int initialRemaining;
    private int editRemaining;
    private long tokenRemaining;
    private long tokenLimit;
    private long globalTokenRemaining;
}
