package com.resky.yuaicodemother.service;

import com.resky.yuaicodemother.config.AiCostControlProperties;
import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.service.impl.AiCostControlServiceImpl;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.RPermitExpirableSemaphore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiCostControlServiceImplTest {
    private AiCostControlProperties properties;
    private AiModelUsageService usageService;
    private AiCostControlServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiCostControlProperties();
        properties.setEnabled(false);
        usageService = mock(AiModelUsageService.class);
        service = new AiCostControlServiceImpl(properties, mock(StringRedisTemplate.class),
                mock(RedissonClient.class), usageService, "test-model");
    }

    @Test
    void classifiesInitialAndEditWithDifferentLimits() {
        User user = user(1L, "user");
        AiCostReservation initial = service.reserve(user, 10L, true, "127.0.0.1");
        AiCostReservation edit = service.reserve(user, 10L, false, "127.0.0.1");
        assertEquals(AiUsageCallTypeEnum.INITIAL_GENERATION, initial.getCallType());
        assertEquals(40_000, initial.getMaxTokens());
        assertEquals(10, initial.getMaxToolRounds());
        assertEquals(AiUsageCallTypeEnum.EDIT, edit.getCallType());
        assertEquals(15_000, edit.getMaxTokens());
        assertEquals(5, edit.getMaxToolRounds());
    }

    @Test
    void cumulativeUsageIsAppliedOnlyAsNewTotal() {
        AiCostReservation reservation = service.reserve(user(1L, "user"), 10L, true, "127.0.0.1");
        service.reportUsage(reservation, new TokenUsage(60, 40, 100), 1);
        service.reportUsage(reservation, new TokenUsage(60, 40, 100), 1);
        assertEquals(100, reservation.getTotalTokens());
        assertEquals(60, reservation.getInputTokens());
        assertEquals(40, reservation.getOutputTokens());
        assertEquals(1, reservation.getToolRounds());
        verify(usageService, times(1)).update(any(), any(com.mybatisflex.core.query.QueryWrapper.class));
    }

    @Test
    void missingUsageIsRecordedAsUnavailable() {
        User user = user(1L, "user");
        service.recordStandalone(user, null, AiUsageCallTypeEnum.ROUTING, "test-model",
                null, AiUsageStatusEnum.SUCCESS, null);
        verify(usageService).save(argThat(record -> "UNAVAILABLE".equals(record.getUsageSource())
                && record.getTotalTokens() == 0));
    }

    @Test
    void adminBypassesPersonalCallReservationButKeepsIpAndConcurrency() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);
        RedissonClient redisson = mock(RedissonClient.class);
        RPermitExpirableSemaphore semaphore = mock(RPermitExpirableSemaphore.class);
        when(redisson.getPermitExpirableSemaphore(anyString())).thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), anyLong(), any())).thenReturn("permit");
        properties.setEnabled(true);
        properties.setEnforcementEnabled(true);
        service = new AiCostControlServiceImpl(properties, redis, redisson, usageService, "test-model");

        AiCostReservation reservation = service.reserve(user(1L, "admin"), 10L, true, "127.0.0.1");
        assertTrue(reservation.isAdmin());
        assertNull(reservation.getRequestCountKey());
        verify(redis, times(1)).execute(any(DefaultRedisScript.class), anyList(),
                anyString(), anyString(), anyString());
        verify(semaphore, times(2)).tryAcquire(anyLong(), anyLong(), any());
    }

    @Test
    void zeroUsageFailureRefundsPersonalCountAndReleasesPermits() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);
        RedissonClient redisson = mock(RedissonClient.class);
        RPermitExpirableSemaphore semaphore = mock(RPermitExpirableSemaphore.class);
        when(redisson.getPermitExpirableSemaphore(anyString())).thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), anyLong(), any())).thenReturn("permit");
        properties.setEnabled(true);
        service = new AiCostControlServiceImpl(properties, redis, redisson, usageService, "test-model");

        AiCostReservation reservation = service.reserve(user(1L, "user"), 10L, true, "127.0.0.1");
        service.fail(reservation, new RuntimeException("before model"), AiUsageStatusEnum.FAILED);
        verify(values).decrement(reservation.getRequestCountKey());
        verify(semaphore, times(2)).tryRelease("permit");
    }

    private static User user(long id, String role) {
        User user = new User();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }
}
