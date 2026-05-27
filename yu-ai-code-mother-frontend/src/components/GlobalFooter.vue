<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a
          href="https://www.resky.top"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          AI 全栈开发--Resky
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  text-align: center;
  padding: 20px;
  margin-top: 40px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.footer {
  margin-top: 0;
  color: var(--console-text-soft);
  border-top: 1px solid rgba(34, 211, 238, 0.14);
  background:
    linear-gradient(180deg, rgba(7, 11, 19, 0.72), rgba(4, 7, 13, 0.94)),
    linear-gradient(90deg, rgba(34, 211, 238, 0.08), transparent 50%, rgba(52, 211, 153, 0.06));
  backdrop-filter: blur(16px);
}

.copyright {
  color: var(--console-text-soft);
}

.author-link {
  color: var(--console-cyan);
}

.author-link:hover {
  color: var(--console-green);
}
</style>
