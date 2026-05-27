<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app.deployKey" type="default" @click="handleViewWork">查看作品</a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="40">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <p class="app-author">
          {{ app.user?.userName || (featured ? '官方' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition:
    transform 0.3s,
    box-shadow 0.3s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.25);
}

.app-preview {
  height: 180px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-placeholder {
  font-size: 48px;
  color: #d9d9d9;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  font-size: 14px;
  color: #666;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-card {
  position: relative;
  border-radius: 8px;
  border: 1px solid rgba(34, 211, 238, 0.18);
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.92), rgba(2, 6, 23, 0.9)),
    linear-gradient(135deg, rgba(34, 211, 238, 0.08), transparent 42%);
  box-shadow: var(--console-shadow), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.app-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.22), transparent 28%, transparent 72%, rgba(52, 211, 153, 0.14));
  opacity: 0;
  transition: opacity 0.3s;
}

.app-card:hover {
  transform: translateY(-6px);
  border-color: rgba(34, 211, 238, 0.42);
  box-shadow: 0 22px 54px rgba(0, 0, 0, 0.42), 0 0 28px rgba(34, 211, 238, 0.12);
}

.app-card:hover::before {
  opacity: 1;
}

.app-card--featured {
  border-color: rgba(245, 158, 11, 0.34);
}

.app-preview {
  height: 184px;
  background:
    linear-gradient(135deg, rgba(8, 47, 73, 0.52), rgba(2, 6, 23, 0.86)),
    repeating-linear-gradient(
      90deg,
      rgba(34, 211, 238, 0.08) 0,
      rgba(34, 211, 238, 0.08) 1px,
      transparent 1px,
      transparent 20px
    );
}

.app-preview::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, transparent 0%, rgba(2, 6, 23, 0.18) 70%, rgba(2, 6, 23, 0.64) 100%);
}

.app-placeholder {
  color: rgba(34, 211, 238, 0.68);
  text-shadow: 0 0 26px rgba(34, 211, 238, 0.28);
}

.app-overlay {
  background: rgba(2, 6, 23, 0.72);
  backdrop-filter: blur(4px);
}

.app-info {
  position: relative;
  padding: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.app-title {
  color: var(--console-text);
}

.app-author {
  color: var(--console-text-soft);
}
</style>
