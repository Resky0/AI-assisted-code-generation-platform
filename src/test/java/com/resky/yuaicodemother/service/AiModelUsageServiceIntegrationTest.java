package com.resky.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.resky.yuaicodemother.model.entity.AiModelUsage;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.AiUsageSourceEnum;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiModelUsageServiceIntegrationTest {
    @Resource
    private AiModelUsageService usageService;

    @Test
    void savesAndReadsCamelCaseColumns() {
        String traceId = "mapping-test-" + UUID.randomUUID();
        AiModelUsage usage = new AiModelUsage();
        usage.setTraceId(traceId);
        usage.setUserId(1L);
        usage.setAppId(1L);
        usage.setCallType(AiUsageCallTypeEnum.ROUTING.name());
        usage.setModelName("mapping-test");
        usage.setInputTokens(1L);
        usage.setOutputTokens(2L);
        usage.setTotalTokens(3L);
        usage.setToolRounds(0);
        usage.setStatus(AiUsageStatusEnum.SUCCESS.name());
        usage.setUsageSource(AiUsageSourceEnum.PROVIDER.name());
        usage.setStartedTime(LocalDateTime.now());
        usage.setFinishedTime(LocalDateTime.now());
        usage.setLatencyMs(1L);
        try {
            assertTrue(usageService.save(usage));
            AiModelUsage saved = usageService.getOne(QueryWrapper.create().eq("traceId", traceId));
            assertNotNull(saved);
            assertEquals(3L, saved.getTotalTokens());
            assertEquals(AiUsageSourceEnum.PROVIDER.name(), saved.getUsageSource());
        } finally {
            usageService.remove(QueryWrapper.create().eq("traceId", traceId));
        }
    }
}
