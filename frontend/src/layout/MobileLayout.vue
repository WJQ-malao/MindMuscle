<template>
  <div class="mobile-layout">
    <div class="page-content">
      <router-view />
    </div>
    <nav class="tab-bar">
      <router-link v-for="t in tabs" :key="t.path" :to="t.path" class="tab"
                   :class="{ active: $route.path === t.path }">
        <span class="icon">{{ t.icon }}</span>
        <span class="label">{{ t.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
const tabs = [
  { path: '/m/home', icon: '📊', label: '首页' },
  { path: '/m/chat', icon: '🤖', label: 'AI教练' },
  { path: '/m/train', icon: '🏋️', label: '训练' },
  { path: '/m/profile', icon: '👤', label: '我的' },
]
</script>

<style scoped>
.mobile-layout {
  max-width: 520px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
}
.page-content {
  flex: 1;
  padding: 12px 12px 70px;
  overflow-y: auto;
}
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 520px;
  display: flex;
  background: #fff;
  border-top: 1px solid #eee;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0 4px;
  text-decoration: none;
  color: #999;
  font-size: 12px;
}
.tab .icon { font-size: 22px; }
.tab.active { color: #409eff; }
</style>
