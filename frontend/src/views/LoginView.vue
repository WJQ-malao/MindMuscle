<template>
  <div class="login-page">
    <el-card class="card">
      <h2>🏋️ 智能健身管理系统</h2>
      <el-form :model="form" size="large" @keyup.enter="onSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item v-if="isRegister">
          <el-input v-model="form.nickname" placeholder="昵称（可选）" />
        </el-form-item>
        <el-form-item v-if="isRegister">
          <el-input-number v-model="form.heightCm" :min="100" :max="230" placeholder="身高 cm" style="width: 100%" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="onSubmit">
          {{ isRegister ? '注册并登录' : '登录' }}
        </el-button>
        <div class="switch" @click="isRegister = !isRegister">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const isRegister = ref(false)
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '', heightCm: 170 as number | undefined })

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = isRegister.value
      ? await authApi.register({ ...form })
      : await authApi.login({ username: form.username, password: form.password })
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('nickname', res.data.nickname)
    localStorage.setItem('role', res.data.role)
    router.push(isMobile() ? '/m/home' : '/')
  } catch {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

function isMobile() {
  return window.innerWidth < 768 || /Android|iPhone|Mobile/i.test(navigator.userAgent)
}
</script>

<style scoped>
.login-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f2d3d, #409eff);
}
.card { width: 380px; }
.card h2 { text-align: center; margin-bottom: 24px; }
.switch { text-align: center; margin-top: 12px; color: #409eff; cursor: pointer; font-size: 13px; }
</style>
