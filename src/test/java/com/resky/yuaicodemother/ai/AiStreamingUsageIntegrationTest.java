package com.resky.yuaicodemother.ai;

import cn.hutool.core.io.FileUtil;
import com.resky.yuaicodemother.constant.AppConstant;
import com.resky.yuaicodemother.core.AiCodeGeneratorFacade;
import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.resky.yuaicodemother.service.AiCostControlService;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class AiStreamingUsageIntegrationTest {
    private static final long TEST_APP_ID = 990000000000000001L;

    @Resource
    private AiCodeGeneratorFacade facade;

    @MockitoBean
    private AiCostControlService costControlService;

    @Test
    void htmlStreamingReportsProviderUsage() {
        AiCostReservation reservation = new AiCostReservation();
        reservation.setTraceId("stream-usage-test");
        reservation.setUserId(1L);
        reservation.setAppId(TEST_APP_ID);
        reservation.setCallType(AiUsageCallTypeEnum.INITIAL_GENERATION);
        reservation.setMaxTokens(40_000);
        reservation.setMaxToolRounds(10);
        reservation.setStartedAt(Instant.now());
        File outputDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR, "html_" + TEST_APP_ID);
        try {
            List<String> chunks = facade.generateAndSaveCodeStream(
                    "生成一个只显示当前时间的极简 HTML 页面", CodeGenTypeEnum.HTML, TEST_APP_ID, reservation)
                    .collectList().block();
            assertNotNull(chunks);
            assertFalse(chunks.isEmpty());
            ArgumentCaptor<TokenUsage> usageCaptor = ArgumentCaptor.forClass(TokenUsage.class);
            verify(costControlService, atLeastOnce()).reportUsage(eq(reservation), usageCaptor.capture(), eq(0));
            TokenUsage usage = usageCaptor.getValue();
            assertNotNull(usage.totalTokenCount());
            assertTrue(usage.totalTokenCount() > 0);
            verify(costControlService).complete(reservation);
        } finally {
            FileUtil.del(outputDir);
        }
    }
}
