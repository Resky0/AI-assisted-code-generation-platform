# AI 成本控制上线

1. 备份 `app`、`chat_history` 数据并执行 `sql/migration/20260702_ai_cost_control.sql`。
2. 首次发布设置环境变量 `AI_COST_ENFORCEMENT_ENABLED=false`。此时仍记录 Redis token 与 MySQL 审计，但不拦截次数和 token 超限；并发保护仍生效。
3. 检查 `ai_model_usage` 中 `usageSource=PROVIDER` 的比例、累计 token 是否与供应商后台一致，并确认 Redis 当日 key 持续增长。
4. 验证无误后设置 `AI_COST_ENFORCEMENT_ENABLED=true` 并重启服务。
5. 分别验证普通用户每日 1 次首次生成、3 次修改，管理员个人额度绕过，以及全站预算与并发熔断。

Redis 不可用时，新 AI 任务和注册请求会被拒绝；应用浏览、案例访问等非 AI 接口不受影响。MySQL 审计写入失败只记录告警，不会绕过 Redis 限额。

重点监控：`FAILED/BUDGET_EXCEEDED` 比例、`usageSource=UNAVAILABLE` 比例、Redis 异常、过期并发许可和全站当日 token 使用率。
