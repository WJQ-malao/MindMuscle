<template>
  <div class="mobile-layout">
    <div class="page-content">
      <router-view />
    </div>
    <nav class="tab-bar">
      <router-link v-for="t in tabs" :key="t.path" :to="t.path" class="tab"
                   :class="{ active: t.match ? $route.path.startsWith(t.path) : $route.path === t.path }">
        <span class="icon">{{ t.icon }}</span>
        <span class="label">{{ t.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const role = localStorage.getItem('role') || 'USER'

interface TabItem { path: string; icon: string; label: string; match?: boolean }

const userTabs: TabItem[] = [
  { path: '/m/home', icon: '📊', label: '数据看板' },
  { path: '/m/chat', icon: '🤖', label: 'AI教练' },
  { path: '/m/train', icon: '🏋️', label: '训练计划' },
  { path: '/m/profile', icon: '👤', label: '我的' },
]

const adminTabs: TabItem[] = [
  { path: '/m/admin', icon: '🛠️', label: '管理', match: true },
  { path: '/m/profile', icon: '👤', label: '我的' },
]

const tabs = computed(() => (role === 'ADMIN' ? adminTabs : userTabs))
</script>

<style scoped>
.mobile-layout {
  max-width: 520px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
  animation: mmPageIn 0.5s ease both;
}
@keyframes mmPageIn {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
.page-content {
  flex: 1;
  padding: 12px 12px 16px;
  overflow-y: auto;
}
/* 底部导航：sticky 而非 fixed。微信/Android WebView 对 fixed+transform 有合成层bug，
   滚动后底栏会悬空；sticky 在文档流内贴底，配合 mobile-layout 的 min-height:100vh 表现稳定 */
.tab-bar {
  position: sticky;
  bottom: 0;
  z-index: 100;
  width: 100%;
  max-width: 520px;
  margin: 0 auto;
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
