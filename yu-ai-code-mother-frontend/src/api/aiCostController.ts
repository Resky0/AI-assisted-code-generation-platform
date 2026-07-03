import request from '@/request'

export interface AiQuota {
  serviceAvailable: boolean
  admin: boolean
  initialRemaining: number
  editRemaining: number
  tokenRemaining: number
  tokenLimit: number
  globalTokenRemaining: number
}

export interface AiUsageRecord {
  id: string
  traceId: string
  userId: string
  appId?: string
  callType: string
  modelName?: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  toolRounds: number
  status: string
  usageSource: string
  errorMessage?: string
  createTime: string
}

export interface AiUsageSummary {
  globalDailyBudget: number
  todayTokens: number
  budgetUsageRate: number
  totalCalls: number
  successCalls: number
  successRate: number
  inputTokens: number
  outputTokens: number
  daily: Array<{
    date: string
    calls: number
    successCalls: number
    inputTokens: number
    outputTokens: number
    totalTokens: number
  }>
}

export const getAiQuota = () => request<{ code: number; data: AiQuota }>('/ai/cost/quota')

export const getAiUsageSummary = (days = 7) =>
  request<{ code: number; data: AiUsageSummary }>('/ai/cost/admin/summary', { params: { days } })

export const pageAiUsageRecords = (data: Record<string, unknown>) =>
  request<{ code: number; data: { records: AiUsageRecord[]; totalRow: number } }>(
    '/ai/cost/admin/records/page',
    { method: 'POST', data },
  )
