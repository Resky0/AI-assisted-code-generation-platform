<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { healthCheck } from '@/api/healthController.ts'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

healthCheck().then((res) => {
  console.log(res)
})
</script>

<template>
  <a-config-provider :locale="zhCN">
  <BasicLayout />
  </a-config-provider>
</template>

<style></style>
