package com.resky.yuaicodemother.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("ai_model_usage")
public class AiModelUsage {
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    @Column("traceId")
    private String traceId;
    @Column("userId")
    private Long userId;
    @Column("appId")
    private Long appId;
    @Column("callType")
    private String callType;
    @Column("modelName")
    private String modelName;
    @Column("inputTokens")
    private Long inputTokens;
    @Column("outputTokens")
    private Long outputTokens;
    @Column("totalTokens")
    private Long totalTokens;
    @Column("toolRounds")
    private Integer toolRounds;
    private String status;
    @Column("usageSource")
    private String usageSource;
    @Column("errorMessage")
    private String errorMessage;
    @Column("startedTime")
    private LocalDateTime startedTime;
    @Column("finishedTime")
    private LocalDateTime finishedTime;
    @Column("latencyMs")
    private Long latencyMs;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
}
