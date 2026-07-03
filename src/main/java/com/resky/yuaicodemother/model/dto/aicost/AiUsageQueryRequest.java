package com.resky.yuaicodemother.model.dto.aicost;

import com.resky.yuaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiUsageQueryRequest extends PageRequest {
    private Long userId;
    private Long appId;
    private String callType;
    private String status;
}
