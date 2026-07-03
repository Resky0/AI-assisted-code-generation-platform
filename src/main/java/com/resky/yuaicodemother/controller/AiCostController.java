package com.resky.yuaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.resky.yuaicodemother.annotation.AuthCheck;
import com.resky.yuaicodemother.common.BaseResponse;
import com.resky.yuaicodemother.common.ResultUtils;
import com.resky.yuaicodemother.config.AiCostControlProperties;
import com.resky.yuaicodemother.constant.UserConstant;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.exception.ThrowUtils;
import com.resky.yuaicodemother.model.dto.aicost.AiUsageQueryRequest;
import com.resky.yuaicodemother.model.entity.AiModelUsage;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.model.vo.AiQuotaVO;
import com.resky.yuaicodemother.model.vo.AiUsageDailyVO;
import com.resky.yuaicodemother.model.vo.AiUsageSummaryVO;
import com.resky.yuaicodemother.service.AiCostControlService;
import com.resky.yuaicodemother.service.AiModelUsageService;
import com.resky.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/cost")
public class AiCostController {
    @Resource private UserService userService;
    @Resource private AiCostControlService costControlService;
    @Resource private AiModelUsageService usageService;
    @Resource private AiCostControlProperties properties;

    @GetMapping("/quota")
    public BaseResponse<AiQuotaVO> quota(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(costControlService.getQuota(user));
    }

    @GetMapping("/admin/summary")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AiUsageSummaryVO> summary(@RequestParam(defaultValue = "7") int days) {
        ThrowUtils.throwIf(days < 1 || days > 30, ErrorCode.PARAMS_ERROR, "days must be between 1 and 30");
        LocalDate today = LocalDate.now(ZoneId.of(properties.getTimezone()));
        LocalDate start = today.minusDays(days - 1L);
        List<AiModelUsage> records = usageService.list(QueryWrapper.create()
                .ge("createTime", start.atStartOfDay()).orderBy("createTime", true));
        Map<LocalDate, long[]> daily = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) daily.put(start.plusDays(i), new long[5]);
        long calls = 0, successes = 0, input = 0, output = 0, todayTokens = 0;
        for (AiModelUsage record : records) {
            calls++;
            if (AiUsageStatusEnum.SUCCESS.name().equals(record.getStatus())) successes++;
            long in = zero(record.getInputTokens()), out = zero(record.getOutputTokens()), total = zero(record.getTotalTokens());
            input += in; output += out;
            LocalDate date = record.getCreateTime().toLocalDate();
            if (date.equals(today)) todayTokens += total;
            long[] values = daily.get(date);
            if (values != null) {
                values[0]++; if (AiUsageStatusEnum.SUCCESS.name().equals(record.getStatus())) values[1]++;
                values[2] += in; values[3] += out; values[4] += total;
            }
        }
        List<AiUsageDailyVO> rows = new ArrayList<>();
        daily.forEach((date, v) -> rows.add(AiUsageDailyVO.builder().date(date).calls(v[0]).successCalls(v[1])
                .inputTokens(v[2]).outputTokens(v[3]).totalTokens(v[4]).build()));
        return ResultUtils.success(AiUsageSummaryVO.builder().globalDailyBudget(properties.getGlobalTokensPerDay())
                .todayTokens(todayTokens).budgetUsageRate(rate(todayTokens, properties.getGlobalTokensPerDay()))
                .totalCalls(calls).successCalls(successes).successRate(rate(successes, calls))
                .inputTokens(input).outputTokens(output).daily(rows).build());
    }

    @PostMapping("/admin/records/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiModelUsage>> records(@RequestBody AiUsageQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getPageSize() > 100, ErrorCode.PARAMS_ERROR);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", request.getUserId()).eq("appId", request.getAppId())
                .eq("callType", request.getCallType()).eq("status", request.getStatus())
                .orderBy("createTime", false);
        return ResultUtils.success(usageService.page(Page.of(request.getPageNum(), request.getPageSize()), wrapper));
    }

    private static long zero(Long value) { return value == null ? 0 : value; }
    private static double rate(long value, long total) { return total == 0 ? 0 : Math.round(value * 10000.0 / total) / 100.0; }
}
