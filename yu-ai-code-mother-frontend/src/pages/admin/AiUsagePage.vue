<template>
  <div class="usage-page">
    <a-space direction="vertical" :size="16" style="width: 100%">
      <a-row :gutter="16">
        <a-col :span="8"><a-card title="今日全站 Token"><a-statistic :value="summary?.todayTokens ?? 0" /><a-progress :percent="Math.min(summary?.budgetUsageRate ?? 0, 100)" /></a-card></a-col>
        <a-col :span="8"><a-card title="近 7 天调用"><a-statistic :value="summary?.totalCalls ?? 0" /></a-card></a-col>
        <a-col :span="8"><a-card title="成功率"><a-statistic :value="summary?.successRate ?? 0" suffix="%" /></a-card></a-col>
      </a-row>
      <a-card title="按日汇总">
        <a-table :data-source="summary?.daily ?? []" :columns="dailyColumns" row-key="date" :pagination="false" />
      </a-card>
      <a-card title="调用明细">
        <a-table :data-source="records" :columns="recordColumns" row-key="traceId"
                 :pagination="pagination" @change="onPageChange" :scroll="{ x: 1100 }" />
      </a-card>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getAiUsageSummary, pageAiUsageRecords, type AiUsageRecord, type AiUsageSummary } from '@/api/aiCostController'

const summary = ref<AiUsageSummary>()
const records = ref<AiUsageRecord[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const dailyColumns = [
  { title: '日期', dataIndex: 'date' }, { title: '调用数', dataIndex: 'calls' },
  { title: '成功数', dataIndex: 'successCalls' }, { title: '输入 Token', dataIndex: 'inputTokens' },
  { title: '输出 Token', dataIndex: 'outputTokens' }, { title: '总 Token', dataIndex: 'totalTokens' },
]
const recordColumns = [
  { title: '时间', dataIndex: 'createTime', width: 180 }, { title: '用户', dataIndex: 'userId', width: 160 },
  { title: '应用', dataIndex: 'appId', width: 160 }, { title: '类型', dataIndex: 'callType', width: 180 },
  { title: '状态', dataIndex: 'status', width: 150 }, { title: '输入', dataIndex: 'inputTokens' },
  { title: '输出', dataIndex: 'outputTokens' }, { title: '总量', dataIndex: 'totalTokens' },
  { title: '工具轮数', dataIndex: 'toolRounds' }, { title: '来源', dataIndex: 'usageSource' },
]

const load = async () => {
  const [summaryRes, recordsRes] = await Promise.all([
    getAiUsageSummary(7), pageAiUsageRecords({ pageNum: pagination.current, pageSize: pagination.pageSize }),
  ])
  if (summaryRes.data.code === 0) summary.value = summaryRes.data.data
  if (recordsRes.data.code === 0) {
    records.value = recordsRes.data.data.records
    pagination.total = recordsRes.data.data.totalRow
  }
}
const onPageChange = (page: { current: number; pageSize: number }) => {
  pagination.current = page.current
  pagination.pageSize = page.pageSize
  load()
}
onMounted(load)
</script>

<style scoped>
.usage-page { padding: 24px; color: var(--console-text); }
:deep(.ant-card) { background: rgba(15, 23, 42, .86); border-color: rgba(34, 211, 238, .18); }
:deep(.ant-card-head), :deep(.ant-card-body), :deep(.ant-statistic-content), :deep(.ant-table) { color: var(--console-text); }
</style>
