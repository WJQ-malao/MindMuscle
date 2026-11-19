<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">🏋️ MindMuscle</div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
               active-text-color="#409eff">
        <!-- ===== 管理员：平台管理菜单 ===== -->
        <template v-if="role === 'ADMIN'">
          <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon>平台概览</el-menu-item>
          <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
          <el-menu-item index="/admin/exercises"><el-icon><Aim /></el-icon>动作库管理</el-menu-item>
          <el-menu-item index="/knowledge"><el-icon><FolderOpened /></el-icon>知识库管理</el-menu-item>
        </template>
        <!-- ===== 普通用户：个人健身菜单 ===== -->
        <template v-else>
          <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon>数据看板</el-menu-item>
          <el-menu-item index="/chat"><el-icon><ChatDotRound /></el-icon>AI 教练</el-menu-item>
          <el-menu-item index="/workout"><el-icon><Calendar /></el-icon>训练计划</el-menu-item>
          <el-menu-item index="/metrics"><el-icon><Odometer /></el-icon>我的</el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ pageTitle }}</span>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const role = ref(localStorage.getItem('role') || 'USER')
const pageTitle = computed(() =>
  role.value === 'ADMIN' && route.path === '/dashboard' ? '平台概览' : (route.meta.title as string),
)

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
  color: #fff; font-size: 19px; font-weight: 700; letter-spacing: 0.5px;
}
.header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #eee; background: #fff;
}
.title { font-size: 16px; font-weight: 600; }
.user { cursor: pointer; display: flex; align-items: center; gap: 4px; }
.main { background: #f0f2f5; }
</style>
