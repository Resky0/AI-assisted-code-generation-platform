<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  background: none;
}

.main-content {
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}

.basic-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at 16% 0%, rgba(34, 211, 238, 0.12), transparent 28%),
    radial-gradient(circle at 88% 12%, rgba(52, 211, 153, 0.08), transparent 30%),
    linear-gradient(180deg, #08111f 0%, #070b13 48%, #04070d 100%);
}

.main-content {
  min-height: calc(100vh - 64px);
}
</style>
