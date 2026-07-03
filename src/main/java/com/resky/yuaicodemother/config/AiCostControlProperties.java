package com.resky.yuaicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.cost-control")
public class AiCostControlProperties {
    private boolean enabled = true;
    private boolean enforcementEnabled = true;
    private String timezone = "Asia/Shanghai";
    private int initialCallsPerDay = 1;
    private int editCallsPerDay = 3;
    private long userTokensPerDay = 80_000;
    private long globalTokensPerDay = 300_000;
    private long initialTraceTokens = 40_000;
    private long editTraceTokens = 15_000;
    private int initialToolRounds = 10;
    private int editToolRounds = 5;
    private int globalConcurrency = 2;
    private int userConcurrency = 1;
    private Duration leaseTime = Duration.ofMinutes(12);
    private Duration maxExecutionTime = Duration.ofMinutes(10);
    private Duration keyTtl = Duration.ofHours(48);
    private int initialPromptMaxChars = 1_000;
    private int editPromptMaxChars = 4_000;
    private int maxGeneratedFiles = 12;
    private int registrationPerHour = 3;
    private int registrationPerDay = 5;
    private int aiRequestsPerIpPerDay = 10;
}
