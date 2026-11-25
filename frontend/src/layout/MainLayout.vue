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
          <span class="user">
            <img v-if="avatar" :src="avatar" class="avatar-img" alt="头像" />
            <span v-else class="avatar-fallback">{{ nickname.charAt(0).toUpperCase() }}</span>
            {{ nickname }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="avatar">📷 更换头像</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <AvatarUpload ref="avatarUploadRef" @updated="onAvatarUpdated" />
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="mm-page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AvatarUpload from '../views/AvatarUpload.vue'

const route = useRoute()
const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const role = ref(localStorage.getItem('role') || 'USER')
const avatar = ref(localStorage.getItem('avatar') || '')
const avatarUploadRef = ref<InstanceType<typeof AvatarUpload>>()
const pageTitle = computed(() => {
  if (role.value === 'ADMIN' && route.path === '/dashboard') return '平台概览'
  // 「我的」页标题展示当前用户名字
  if (route.path === '/metrics') return `你好，${nickname.value}`
  return route.meta.title as string
})

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    localStorage.clear()
    router.push('/login')
  } else if (cmd === 'avatar') {
    avatarUploadRef.value?.open()
  }
}

/** 头像上传成功后更新顶栏显示 */
function onAvatarUpdated(url: string) {
  avatar.value = url
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside {
  background: linear-gradient(180deg, #1a2332 0%, #1f2d3d 100%);
  overflow: hidden;
}
.aside :deep(.el-menu) { border-right: none; background: transparent; }
/* 菜单项：圆角 pill，激活项高亮 */
.aside :deep(.el-menu-item) {
  border-radius: 10px;
  margin: 3px 10px;
  height: 46px;
  line-height: 46px;
}
.aside :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.06); }
.aside :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.25), rgba(122, 90, 248, 0.18));
  font-weight: 600;
}
.logo {
  height: 60px; line-height: 60px; text-align: center;
  color: #fff; font-size: 19px; font-weight: 700; letter-spacing: 0.5px;
}
.header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #f0f1f4;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  box-shadow: 0 1px 4px rgba(16, 24, 40, 0.04);
}
.title { font-size: 17px; font-weight: 700; color: #1f2329; }
.user { cursor: pointer; display: flex; align-items: center; gap: 6px; }
.avatar-img {
  width: 30px; height: 30px; border-radius: 50%; object-fit: cover;
  border: 1px solid #eee;
}
.avatar-fallback {
  width: 30px; height: 30px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #7a5af8);
  color: #fff; font-size: 14px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
}
.main { background: #f5f6fa; padding: 20px; animation: mmPageIn 0.5s ease both; }
@keyframes mmPageIn {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
