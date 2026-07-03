ALTER TABLE app
    ADD COLUMN generationStatus varchar(32) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/GENERATING/READY/FAILED' AFTER codeGenType;

UPDATE app a SET a.generationStatus = 'READY'
WHERE a.deployedTime IS NOT NULL
   OR EXISTS (SELECT 1 FROM chat_history h WHERE h.appId = a.id AND h.messageType = 'ai');

CREATE TABLE IF NOT EXISTS ai_model_usage
(
    id bigint auto_increment primary key,
    traceId varchar(64) NOT NULL,
    userId bigint NOT NULL,
    appId bigint NULL,
    callType varchar(32) NOT NULL,
    modelName varchar(128) NULL,
    inputTokens bigint DEFAULT 0 NOT NULL,
    outputTokens bigint DEFAULT 0 NOT NULL,
    totalTokens bigint DEFAULT 0 NOT NULL,
    toolRounds int DEFAULT 0 NOT NULL,
    status varchar(32) NOT NULL,
    usageSource varchar(32) DEFAULT 'UNAVAILABLE' NOT NULL,
    errorMessage varchar(1000) NULL,
    startedTime datetime NOT NULL,
    finishedTime datetime NULL,
    latencyMs bigint NULL,
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_traceId (traceId),
    INDEX idx_userId_createTime (userId, createTime),
    INDEX idx_appId_createTime (appId, createTime),
    INDEX idx_callType_status_createTime (callType, status, createTime)
) COMMENT 'AI model workflow usage' COLLATE = utf8mb4_unicode_ci;
