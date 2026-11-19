<template>
  <div class="login-page">
    <el-card class="card">
      <h2>🏋️ MindMuscle</h2>
      <el-steps v-if="isRegister" :active="step - 1" align-center finish-status="success" class="steps">
        <el-step title="账号信息" />
        <el-step title="身体数据" />
      </el-steps>
      <el-form :model="form" size="large" @keyup.enter="onSubmit">
        <template v-if="!isRegister || step === 1">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password />
          </el-form-item>
          <template v-if="isRegister">
            <el-form-item>
              <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.nickname" placeholder="昵称（可选）" />
            </el-form-item>
          </template>
        </template>
        <template v-else>
          <el-form-item>
            <el-input-number v-model="form.heightCm" :min="100" :max="230" placeholder="身高 cm" style="width: 100%" />
          </el-form-item>
          <el-form-item>
            <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" placeholder="体重 kg" style="width: 100%" />
          </el-form-item>
          <el-form-item>
            <el-input-number v-model="form.bodyFat" :min="1" :max="60" :precision="1" placeholder="体脂率 %（可选）" style="width: 100%" />
          </el-form-item>
        </template>
        <el-button v-if="isRegister && step === 2" style="width: 100%; margin-bottom: 10px" @click="step = 1">
          上一步
        </el-button>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="onSubmit">
          {{ submitText }}
        </el-button>
        <div class="switch" @click="toggleMode">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, metricApi } from '../api'

const router = useRouter()
const isRegister = ref(false)
const step = ref(1)
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  heightCm: 170 as number | undefined,
  weightKg: undefined as number | undefined,
  bodyFat: undefined as number | undefined,
})

const submitText = computed(() => {
  if (!isRegister.value) return '登录'
  return step.value === 1 ? '下一步' : '完成注册'
})

function toggleMode() {
  isRegister.value = !isRegister.value
  step.value = 1
}

async function onSubmit() {
  if (!isRegister.value) {
    if (!form.username || !form.password) {
      ElMessage.warning('请输入用户名和密码')
      return
    }
    await doLogin()
    return
  }
  if (step.value === 1) {
    if (!form.username || !form.password) {
      ElMessage.warning('请输入用户名和密码')
      return
    }
    if (form.confirmPassword !== form.password) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }
    step.value = 2
    return
  }
  if (!form.weightKg) {
    ElMessage.warning('请填写体重')
    return
  }
  await doRegister()
}

async function doLogin() {
  loading.value = true
  try {
    const res: any = await authApi.login({ username: form.username, password: form.password })
    saveSession(res.data)
    router.push(isMobile() ? '/m/home' : '/')
  } catch {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  loading.value = true
  try {
    const res: any = await authApi.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      heightCm: form.heightCm,
    })
    saveSession(res.data)
    try {
      await metricApi.add({
        weightKg: form.weightKg!,
        bodyFat: form.bodyFat,
        recordDate: new Date().toISOString().slice(0, 10),
      })
    } catch {
      // 体重记录失败不阻塞注册流程
    }
    router.push(isMobile() ? '/m/home' : '/')
  } catch {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

function saveSession(data: any) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('nickname', data.nickname)
  localStorage.setItem('username', data.username)
  localStorage.setItem('role', data.role)
  localStorage.setItem('roles', JSON.stringify(data.roles ?? []))
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
.steps { margin-bottom: 24px; }
.switch { text-align: center; margin-top: 12px; color: #409eff; cursor: pointer; font-size: 13px; }
</style>
