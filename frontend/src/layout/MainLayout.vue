<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">🏋️ 智能健身系统</div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
               active-text-color="#409eff">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon>数据看板</el-menu-item>
        <el-menu-item index="/chat"><el-icon><ChatDotRound /></el-icon>AI 教练</el-menu-item>
        <el-menu-item index="/metrics"><el-icon><Odometer /></el-icon>身体数据</el-menu-item>
        <el-menu-item index="/workout"><el-icon><Calendar /></el-icon>训练计划</el-menu-item>
        <el-menu-item index="/knowledge" v-if="role === 'ADMIN'"><el-icon><FolderOpened /></el-icon>知识库管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ $route.meta.title }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">{{ nickname }} <el-icon><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const role = ref(localStorage.getItem('role') || 'USER')

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    localStorage.clear()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1f2d3d; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  color: #fff; font-size: 17px; font-weight: 600;
}
.header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #eee; background: #fff;
}
.title { font-size: 16px; font-weight: 600; }
.user { cursor: pointer; display: flex; align-items: center; gap: 4px; }
.main { background: #f0f2f5; }
</style>
