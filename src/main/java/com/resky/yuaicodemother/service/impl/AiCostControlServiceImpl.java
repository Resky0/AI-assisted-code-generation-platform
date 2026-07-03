package com.resky.yuaicodemother.service.impl;

import com.resky.yuaicodemother.config.AiCostControlProperties;
import com.resky.yuaicodemother.constant.UserConstant;
import com.resky.yuaicodemother.exception.AiBudgetExceededException;
import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.entity.AiModelUsage;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.AiUsageSourceEnum;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.model.vo.AiQuotaVO;
import com.resky.yuaicodemother.service.AiCostControlService;
import com.resky.yuaicodemother.service.AiModelUsageService;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiCostControlServiceImpl implements AiCostControlService {
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local limit = tonumber(ARGV[1])
            if ARGV[3] == '1' and current >= limit then return -1 end
            current = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return current
            """, Long.class);
    private static final DefaultRedisScript<Long> TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local userCurrent = tonumber(redis.call('GET', KEYS[1]) or '0')
            local globalCurrent = tonumber(redis.call('GET', KEYS[2]) or '0')
            local delta = tonumber(ARGV[1])
            local userNew = userCurrent
            if ARGV[4] == '0' then
              userNew = redis.call('INCRBY', KEYS[1], delta)
              redis.call('EXPIRE', KEYS[1], ARGV[5])
            end
            local globalNew = redis.call('INCRBY', KEYS[2], delta)
            redis.call('EXPIRE', KEYS[2], ARGV[5])
            if ARGV[6] == '1' then
              if globalNew > tonumber(ARGV[3]) then return -2 end
              if ARGV[4] == '0' and userNew > tonumber(ARGV[2]) then return -1 end
            end
            return globalNew
            """, Long.class);

    private final AiCostControlProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final AiModelUsageService usageService;
    private final String modelName;

    public AiCostControlServiceImpl(AiCostControlProperties properties, StringRedisTemplate redisTemplate,
                                    RedissonClient redissonClient, AiModelUsageService usageService,
                                    @Value("${langchain4j.open-ai.reasoning-streaming-chat-model.model-name:unknown}") String modelName) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.usageService = usageService;
        this.modelName = modelName;
    }

    @Override
    public AiCostReservation reserve(User user, long appId, boolean initial, String clientIp) {
        boolean admin = UserConstant.ADMIN_ROLE.equals(user.getUserRole());
        AiCostReservation reservation = new AiCostReservation();
        reservation.setTraceId(UUID.randomUUID().toString());
        reservation.setUserId(user.getId());
        reservation.setAppId(appId);
        reservation.setCallType(initial ? AiUsageCallTypeEnum.INITIAL_GENERATION : AiUsageCallTypeEnum.EDIT);
        reservation.setAdmin(admin);
        reservation.setMaxTokens(initial ? properties.getInitialTraceTokens() : properties.getEditTraceTokens());
        reservation.setMaxToolRounds(initial ? properties.getInitialToolRounds() : properties.getEditToolRounds());
        reservation.setStartedAt(Instant.now());
        if (!properties.isEnabled()) {
            createUsage(reservation);
            return reservation;
        }
        try {
            checkAvailableBudget(user.getId(), admin);
            if (!admin) {
                String countKey = key("calls:" + user.getId() + ":" + (initial ? "initial" : "edit"));
                long limit = initial ? properties.getInitialCallsPerDay() : properties.getEditCallsPerDay();
                reserveCounter(countKey, limit, ErrorCode.AI_QUOTA_EXCEEDED);
                reservation.setRequestCountKey(countKey);
            }
            reserveCounter(key("ip-ai:" + safeIp(clientIp)), properties.getAiRequestsPerIpPerDay(), ErrorCode.AI_QUOTA_EXCEEDED);
            acquirePermits(reservation);
            createUsage(reservation);
            return reservation;
        } catch (BusinessException e) {
            refundCount(reservation.getRequestCountKey());
            releasePermits(reservation);
            throw e;
        } catch (Exception e) {
            refundCount(reservation.getRequestCountKey());
            releasePermits(reservation);
            log.error("Redis cost control unavailable", e);
            throw new BusinessException(ErrorCode.AI_BUSY, "AI quota service unavailable, please try again later");
        }
    }

    private void acquirePermits(AiCostReservation reservation) throws InterruptedException {
        RPermitExpirableSemaphore global = redissonClient.getPermitExpirableSemaphore("ai:cost:semaphore:global");
        global.trySetPermits(properties.getGlobalConcurrency());
        String globalId = global.tryAcquire(0, properties.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
        if (globalId == null) throw new BusinessException(ErrorCode.AI_BUSY);
        reservation.setGlobalPermitId(globalId);
        RPermitExpirableSemaphore perUser = redissonClient.getPermitExpirableSemaphore("ai:cost:semaphore:user:" + reservation.getUserId());
        perUser.trySetPermits(properties.getUserConcurrency());
        String userId = perUser.tryAcquire(0, properties.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
        if (userId == null) throw new BusinessException(ErrorCode.AI_BUSY);
        reservation.setUserPermitId(userId);
    }

    @Override
    public synchronized void reportUsage(AiCostReservation reservation, TokenUsage usage, int toolRounds) {
        reservation.setToolRounds(Math.max(reservation.getToolRounds(), toolRounds));
        if (usage == null) return;
        long cumulative = value(usage.totalTokenCount());
        long delta = Math.max(0, cumulative - reservation.getTotalTokens());
        if (delta == 0) return;
        reservation.setInputTokens(value(usage.inputTokenCount()));
        reservation.setOutputTokens(value(usage.outputTokenCount()));
        reservation.setTotalTokens(cumulative);
        if (properties.isEnabled()) {
            try {
                Long result = redisTemplate.execute(TOKEN_SCRIPT,
                        List.of(key("tokens:user:" + reservation.getUserId()), key("tokens:global")),
                        Long.toString(delta), Long.toString(properties.getUserTokensPerDay()),
                        Long.toString(properties.getGlobalTokensPerDay()), reservation.isAdmin() ? "1" : "0",
                        Long.toString(properties.getKeyTtl().toSeconds()), properties.isEnforcementEnabled() ? "1" : "0");
                if (result != null && result == -1) {
                    updateRunningUsage(reservation);
                    throw new AiBudgetExceededException("今日个人 Token 额度已用完");
                }
                if (result != null && result == -2) {
                    updateRunningUsage(reservation);
                    throw new AiBudgetExceededException("今日全站 Token 预算已用完");
                }
            } catch (AiBudgetExceededException e) {
                throw e;
            } catch (Exception e) {
                throw new AiBudgetExceededException("额度服务暂不可用，已停止新的模型调用");
            }
        }
        updateRunningUsage(reservation);
    }

    @Override
    public void complete(AiCostReservation reservation) {
        finish(reservation, AiUsageStatusEnum.SUCCESS, null);
    }

    @Override
    public void fail(AiCostReservation reservation, Throwable error, AiUsageStatusEnum status) {
        finish(reservation, status, error);
    }

    private synchronized void finish(AiCostReservation reservation, AiUsageStatusEnum status, Throwable error) {
        if (reservation == null || reservation.isFinished()) return;
        reservation.setFinished(true);
        if (reservation.getTotalTokens() == 0) refundCount(reservation.getRequestCountKey());
        releasePermits(reservation);
        AiModelUsage update = new AiModelUsage();
        update.setInputTokens(reservation.getInputTokens());
        update.setOutputTokens(reservation.getOutputTokens());
        update.setTotalTokens(reservation.getTotalTokens());
        update.setToolRounds(reservation.getToolRounds());
        update.setStatus(status.name());
        update.setUsageSource(reservation.getTotalTokens() > 0 ? AiUsageSourceEnum.PROVIDER.name() : AiUsageSourceEnum.UNAVAILABLE.name());
        update.setErrorMessage(error == null ? null : abbreviate(error.getMessage()));
        update.setFinishedTime(LocalDateTime.now());
        update.setLatencyMs(Duration.between(reservation.getStartedAt(), Instant.now()).toMillis());
        safeUpdateByTrace(reservation.getTraceId(), update);
    }

    @Override
    public void recordStandalone(User user, Long appId, AiUsageCallTypeEnum callType, String model,
                                 TokenUsage usage, AiUsageStatusEnum status, Throwable error) {
        AiModelUsage record = new AiModelUsage();
        record.setTraceId(UUID.randomUUID().toString());
        record.setUserId(user.getId());
        record.setAppId(appId);
        record.setCallType(callType.name());
        record.setModelName(model);
        record.setInputTokens(usage == null ? 0L : value(usage.inputTokenCount()));
        record.setOutputTokens(usage == null ? 0L : value(usage.outputTokenCount()));
        record.setTotalTokens(usage == null ? 0L : value(usage.totalTokenCount()));
        record.setToolRounds(0);
        record.setStatus(status.name());
        record.setUsageSource(usage == null ? AiUsageSourceEnum.UNAVAILABLE.name() : AiUsageSourceEnum.PROVIDER.name());
        record.setErrorMessage(error == null ? null : abbreviate(error.getMessage()));
        record.setStartedTime(LocalDateTime.now());
        record.setFinishedTime(LocalDateTime.now());
        record.setLatencyMs(0L);
        BusinessException costError = null;
        long total = record.getTotalTokens();
        if (properties.isEnabled() && total > 0) {
            try {
                boolean admin = UserConstant.ADMIN_ROLE.equals(user.getUserRole());
                Long result = redisTemplate.execute(TOKEN_SCRIPT,
                        List.of(key("tokens:user:" + user.getId()), key("tokens:global")),
                        Long.toString(total), Long.toString(properties.getUserTokensPerDay()),
                        Long.toString(properties.getGlobalTokensPerDay()), admin ? "1" : "0",
                        Long.toString(properties.getKeyTtl().toSeconds()), properties.isEnforcementEnabled() ? "1" : "0");
                if (result != null && (result == -1 || result == -2)) {
                    record.setStatus(AiUsageStatusEnum.BUDGET_EXCEEDED.name());
                    record.setErrorMessage(result == -1 ? "Daily user token budget exceeded" : "Daily global token budget exceeded");
                    costError = new BusinessException(ErrorCode.AI_BUDGET_EXCEEDED, record.getErrorMessage());
                }
            } catch (BusinessException e) {
                costError = e;
            } catch (Exception e) {
                record.setStatus(AiUsageStatusEnum.FAILED.name());
                record.setErrorMessage("Redis cost control unavailable");
                costError = new BusinessException(ErrorCode.AI_BUSY, record.getErrorMessage());
            }
        }
        try { usageService.save(record); } catch (Exception e) { log.warn("Unable to save AI usage audit", e); }
        if (costError != null) throw costError;
    }

    @Override
    public AiQuotaVO getQuota(User user) {
        boolean admin = UserConstant.ADMIN_ROLE.equals(user.getUserRole());
        try {
            long initial = number(redisTemplate.opsForValue().get(key("calls:" + user.getId() + ":initial")));
            long edit = number(redisTemplate.opsForValue().get(key("calls:" + user.getId() + ":edit")));
            long userTokens = number(redisTemplate.opsForValue().get(key("tokens:user:" + user.getId())));
            long globalTokens = number(redisTemplate.opsForValue().get(key("tokens:global")));
            return AiQuotaVO.builder().serviceAvailable(true).admin(admin)
                    .initialRemaining(admin ? properties.getInitialCallsPerDay() : remaining(properties.getInitialCallsPerDay(), initial))
                    .editRemaining(admin ? properties.getEditCallsPerDay() : remaining(properties.getEditCallsPerDay(), edit))
                    .tokenLimit(properties.getUserTokensPerDay())
                    .tokenRemaining(admin ? properties.getUserTokensPerDay() : Math.max(0, properties.getUserTokensPerDay() - userTokens))
                    .globalTokenRemaining(Math.max(0, properties.getGlobalTokensPerDay() - globalTokens)).build();
        } catch (Exception e) {
            return AiQuotaVO.builder().serviceAvailable(false).admin(admin).initialRemaining(0).editRemaining(0)
                    .tokenLimit(properties.getUserTokensPerDay()).tokenRemaining(0).globalTokenRemaining(0).build();
        }
    }

    @Override
    public void checkRegistration(String clientIp) {
        try {
            String ip = safeIp(clientIp);
            reserveCounter("ai:cost:register:hour:" + hourKey() + ":" + ip, properties.getRegistrationPerHour(), ErrorCode.TOO_MANY_REQUEST);
            try {
                reserveCounter(key("register:day:" + ip), properties.getRegistrationPerDay(), ErrorCode.TOO_MANY_REQUEST);
            } catch (BusinessException e) {
                redisTemplate.opsForValue().decrement("ai:cost:register:hour:" + hourKey() + ":" + ip);
                throw e;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "Registration rate limiter unavailable");
        }
    }

    private void reserveCounter(String key, long limit, ErrorCode errorCode) {
        Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(key), Long.toString(limit),
                Long.toString(properties.getKeyTtl().toSeconds()), properties.isEnforcementEnabled() ? "1" : "0");
        if (result != null && result == -1) throw new BusinessException(errorCode);
    }

    private void checkAvailableBudget(long userId, boolean admin) {
        if (!properties.isEnforcementEnabled()) return;
        long global = number(redisTemplate.opsForValue().get(key("tokens:global")));
        if (global >= properties.getGlobalTokensPerDay()) {
            throw new BusinessException(ErrorCode.AI_BUDGET_EXCEEDED, "Daily global AI token budget exhausted");
        }
        if (!admin) {
            long user = number(redisTemplate.opsForValue().get(key("tokens:user:" + userId)));
            if (user >= properties.getUserTokensPerDay()) {
                throw new BusinessException(ErrorCode.AI_BUDGET_EXCEEDED, "Daily user AI token budget exhausted");
            }
        }
    }

    private void createUsage(AiCostReservation r) {
        AiModelUsage record = new AiModelUsage();
        record.setTraceId(r.getTraceId()); record.setUserId(r.getUserId()); record.setAppId(r.getAppId());
        record.setCallType(r.getCallType().name()); record.setModelName(modelName);
        record.setInputTokens(0L); record.setOutputTokens(0L); record.setTotalTokens(0L); record.setToolRounds(0);
        record.setStatus(AiUsageStatusEnum.RUNNING.name()); record.setUsageSource(AiUsageSourceEnum.UNAVAILABLE.name());
        record.setStartedTime(LocalDateTime.now());
        try { usageService.save(record); } catch (Exception e) { log.warn("Unable to save AI usage audit", e); }
    }

    private void updateRunningUsage(AiCostReservation r) {
        AiModelUsage update = new AiModelUsage(); update.setInputTokens(r.getInputTokens());
        update.setOutputTokens(r.getOutputTokens()); update.setTotalTokens(r.getTotalTokens());
        update.setToolRounds(r.getToolRounds()); update.setUsageSource(AiUsageSourceEnum.PROVIDER.name());
        safeUpdateByTrace(r.getTraceId(), update);
    }

    private void safeUpdateByTrace(String traceId, AiModelUsage update) {
        try { usageService.update(update, com.mybatisflex.core.query.QueryWrapper.create().eq("traceId", traceId)); }
        catch (Exception e) { log.warn("Unable to update AI usage audit trace={}", traceId, e); }
    }

    private void releasePermits(AiCostReservation r) {
        if (r == null) return;
        try {
            if (r.getUserPermitId() != null) redissonClient.getPermitExpirableSemaphore("ai:cost:semaphore:user:" + r.getUserId()).tryRelease(r.getUserPermitId());
            if (r.getGlobalPermitId() != null) redissonClient.getPermitExpirableSemaphore("ai:cost:semaphore:global").tryRelease(r.getGlobalPermitId());
        } catch (Exception e) { log.warn("Unable to release AI permits trace={}", r.getTraceId(), e); }
    }

    private void refundCount(String key) {
        if (key == null) return;
        try { redisTemplate.opsForValue().decrement(key); } catch (Exception e) { log.warn("Unable to refund quota key={}", key, e); }
    }

    private String key(String suffix) { return "ai:cost:" + LocalDate.now(zone()) + ":" + suffix; }
    private String hourKey() { return java.time.ZonedDateTime.now(zone()).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")); }
    private ZoneId zone() { return ZoneId.of(properties.getTimezone()); }
    private static String safeIp(String ip) { return ip == null ? "unknown" : ip.replaceAll("[^0-9a-fA-F:.]", "_"); }
    private static long value(Integer value) { return value == null ? 0 : value.longValue(); }
    private static long number(String value) { return value == null ? 0 : Long.parseLong(value); }
    private static int remaining(int limit, long used) { return (int) Math.max(0, limit - used); }
    private static String abbreviate(String value) { return value == null ? null : value.substring(0, Math.min(1000, value.length())); }
}
