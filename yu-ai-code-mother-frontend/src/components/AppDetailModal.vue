<template>
  <a-modal v-model:open="visible" title="应用详情" :footer="null" width="500px">
    <div class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="app-basic-info">
        <div class="info-item">
          <span class="info-label">创建者：</span>
          <UserInfo :user="app?.user" size="small" />
        </div>
        <div class="info-item">
          <span class="info-label">创建时间：</span>
          <span>{{ formatTime(app?.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">生成类型：</span>
          <a-tag v-if="app?.codeGenType" color="blue">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else>未知类型</span>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button type="primary" @click="handleEdit">
            <template #icon>
              <EditOutlined />
            </template>
            修改
          </a-button>
          <a-popconfirm
            title="确定要删除这个应用吗？"
            @confirm="handleDelete"
            ok-text="确定"
            cancel-text="取消"
          >
            <a-button danger>
              <template #icon>
                <DeleteOutlined />
              </template>
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import { formatTime } from '@/utils/time'
import { formatCodeGenType } from '@/utils/codeGenTypes.ts'

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.app-basic-info {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.info-label {
  width: 80px;
  color: #666;
  font-size: 14px;
  flex-shrink: 0;
}

.app-actions {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.app-detail-content {
  color: var(--console-text);
}

.info-label {
  color: var(--console-text-soft);
}

.info-item {
  color: var(--console-text);
}

.app-actions {
  border-top-color: rgba(148, 163, 184, 0.16);
}

:deep(.ant-modal-content) {
  overflow: hidden;
  border: 1px solid rgba(34, 211, 238, 0.22);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(2, 6, 23, 0.96)),
    linear-gradient(135deg, rgba(34, 211, 238, 0.1), transparent 46%);
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.48), 0 0 34px rgba(34, 211, 238, 0.12);
}

:deep(.ant-modal-header) {
  padding: 18px 22px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.18);
  background: rgba(8, 47, 73, 0.22);
}

:deep(.ant-modal-title) {
  color: #f8fafc;
  font-weight: 700;
  letter-spacing: 0;
}

:deep(.ant-modal-close) {
  color: var(--console-text-soft);
}

:deep(.ant-modal-close:hover) {
  color: var(--console-cyan);
  background: rgba(34, 211, 238, 0.1);
}

.app-detail-content {
  padding: 6px 0 0;
}

.app-basic-info {
  display: grid;
  gap: 12px;
  margin-bottom: 22px;
}

.info-item {
  min-height: 44px;
  margin-bottom: 0;
  padding: 12px 14px;
  border: 1px solid rgba(125, 211, 252, 0.16);
  border-radius: 8px;
  color: #e0f2fe;
  background:
    linear-gradient(90deg, rgba(8, 47, 73, 0.42), rgba(15, 23, 42, 0.72)),
    rgba(2, 6, 23, 0.56);
}

.info-label {
  width: 92px;
  color: #67e8f9;
  font-weight: 700;
}

.info-item > span:last-child {
  color: #f8fafc;
  font-weight: 600;
}

.info-item :deep(.user-info .user-name) {
  color: #f8fafc;
  font-weight: 700;
}

.info-item :deep(.ant-tag) {
  margin: 0;
  color: #dbeafe;
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(37, 99, 235, 0.28);
}

.app-actions {
  padding-top: 18px;
  border-top: 1px solid rgba(125, 211, 252, 0.16);
}
</style>
