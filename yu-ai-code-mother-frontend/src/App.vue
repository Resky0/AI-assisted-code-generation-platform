<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <BasicLayout />
</template>

<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

html {
  overflow-x: hidden;
}
:root {
  --console-bg: #070b13;
  --console-bg-soft: #0b1220;
  --console-panel: rgba(11, 18, 32, 0.9);
  --console-panel-strong: rgba(15, 23, 42, 0.96);
  --console-line: rgba(34, 211, 238, 0.22);
  --console-line-soft: rgba(148, 163, 184, 0.16);
  --console-text: #e5eefb;
  --console-text-soft: #9fb2ca;
  --console-muted: #64748b;
  --console-cyan: #22d3ee;
  --console-blue: #38bdf8;
  --console-green: #34d399;
  --console-amber: #f59e0b;
  --console-danger: #fb7185;
  --console-shadow: 0 18px 46px rgba(0, 0, 0, 0.34);
}

html,
body {
  color: var(--console-text);
  background:
    linear-gradient(180deg, rgba(8, 13, 24, 0.98) 0%, rgba(4, 9, 17, 1) 100%),
    repeating-linear-gradient(
      90deg,
      rgba(34, 211, 238, 0.07) 0,
      rgba(34, 211, 238, 0.07) 1px,
      transparent 1px,
      transparent 96px
    ),
    repeating-linear-gradient(
      0deg,
      rgba(148, 163, 184, 0.05) 0,
      rgba(148, 163, 184, 0.05) 1px,
      transparent 1px,
      transparent 96px
    );
}

a {
  color: var(--console-cyan);
}

.ant-layout {
  color: var(--console-text);
}

.ant-btn {
  border-radius: 6px;
  font-weight: 600;
}

.ant-btn-primary {
  border-color: rgba(34, 211, 238, 0.85);
  background: linear-gradient(135deg, #0891b2 0%, #2563eb 100%);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
}

.ant-btn-primary:not(:disabled):hover {
  border-color: var(--console-cyan);
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
}

.ant-btn-default {
  color: var(--console-text);
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.82);
}

.ant-btn-default:not(:disabled):hover {
  color: var(--console-cyan);
  border-color: rgba(34, 211, 238, 0.55);
  background: rgba(8, 47, 73, 0.45);
}

.ant-input,
.ant-input-affix-wrapper,
.ant-input-number,
.ant-select-selector,
.ant-picker,
.ant-input textarea {
  color: var(--console-text) !important;
  border-color: rgba(148, 163, 184, 0.22) !important;
  background: rgba(2, 6, 23, 0.72) !important;
}

.ant-input::placeholder,
.ant-input textarea::placeholder {
  color: rgba(148, 163, 184, 0.72) !important;
}

.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: rgba(34, 211, 238, 0.74) !important;
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.12) !important;
}

.ant-card,
.ant-modal-content,
.ant-table-wrapper .ant-table {
  color: var(--console-text);
  border: 1px solid var(--console-line-soft);
  background: var(--console-panel-strong);
}

.ant-card-head,
.ant-modal-header,
.ant-table-wrapper .ant-table-thead > tr > th {
  color: var(--console-text) !important;
  border-color: var(--console-line-soft) !important;
  background: rgba(15, 23, 42, 0.98) !important;
}

.ant-modal-title,
.ant-card-head-title,
.ant-form-item-label > label,
.ant-descriptions-title {
  color: var(--console-text) !important;
}

.ant-table-wrapper .ant-table-tbody > tr > td {
  color: var(--console-text-soft);
  border-color: rgba(148, 163, 184, 0.12);
  background: rgba(11, 18, 32, 0.78);
}

.ant-table-wrapper .ant-table-tbody > tr:hover > td {
  background: rgba(14, 116, 144, 0.18) !important;
}

.ant-table-wrapper .ant-table-tbody > tr > td {
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease !important;
}

.ant-table-wrapper .ant-table-tbody > tr > td.ant-table-cell-row-hover,
.ant-table-wrapper .ant-table-tbody > tr.ant-table-row:hover > td {
  background: rgba(14, 116, 144, 0.18) !important;
}

.ant-pagination,
.ant-pagination-total-text,
.ant-pagination-item a,
.ant-pagination-prev button,
.ant-pagination-next button {
  color: var(--console-text-soft) !important;
}

.ant-pagination-item {
  border-color: rgba(148, 163, 184, 0.22) !important;
  background: rgba(15, 23, 42, 0.82) !important;
}

.ant-pagination-item-active {
  border-color: rgba(34, 211, 238, 0.78) !important;
  background: rgba(8, 145, 178, 0.18) !important;
}

.ant-avatar {
  color: #082f49;
  border: 1px solid rgba(186, 230, 253, 0.72);
  background: linear-gradient(135deg, #e0f2fe 0%, #a7f3d0 100%);
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.18);
}
</style>
