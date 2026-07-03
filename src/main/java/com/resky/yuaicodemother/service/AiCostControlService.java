package com.resky.yuaicodemother.service;

import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.model.vo.AiQuotaVO;
import dev.langchain4j.model.output.TokenUsage;

public interface AiCostControlService {
    AiCostReservation reserve(User user, long appId, boolean initial, String clientIp);
    void reportUsage(AiCostReservation reservation, TokenUsage cumulativeUsage, int toolRounds);
    void complete(AiCostReservation reservation);
    void fail(AiCostReservation reservation, Throwable error, AiUsageStatusEnum status);
    void recordStandalone(User user, Long appId, AiUsageCallTypeEnum callType, String modelName,
                          TokenUsage usage, AiUsageStatusEnum status, Throwable error);
    AiQuotaVO getQuota(User user);
    void checkRegistration(String clientIp);
}
