<template>
  <div class="login-page">
    <!-- 左侧品牌面板（借鉴模板版式） -->
    <aside class="brand-panel">
      <div class="brand-logo">🏋️ MindMuscle</div>
      <div class="brand-slogan">
        <h1>智能健身，科学训练</h1>
        <p>RAG 知识库 + AI 教练 + 数据看板，你的个性化健身伙伴</p>
      </div>
      <ul class="brand-features">
        <li>🤖 AI 教练 —— 基于知识库的流式对话与自动计划生成</li>
        <li>📊 数据看板 —— 体重趋势、肌群分布、打卡热力图</li>
        <li>🏋️ 训练计划 —— 按周管理，逐组打卡</li>
      </ul>
      <div class="brand-circles"><i /><i /><i /></div>
    </aside>

    <!-- 右侧表单面板 -->
    <main class="form-panel">
      <el-card class="card" shadow="never">
        <h2 class="card-title">{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
        <p class="card-sub">{{ isRegister ? '两步完成注册，开始你的健身之旅' : '登录以继续使用 MindMuscle' }}</p>
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
          <el-button class="submit-btn" type="primary" style="width: 100%" :loading="loading" @click="onSubmit">
            {{ submitText }}
          </el-button>
          <div class="switch" @click="toggleMode">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </div>
        </el-form>
      </el-card>
      <div class="footer-tip">MindMuscle · 基于 SpringBoot 与 RAG 的个性化健身系统</div>
    </main>
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
  if (!isRegister.value) return '登 录'
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
  if (data.avatar) localStorage.setItem('avatar', data.avatar)
}

function isMobile() {
  return window.innerWidth < 768 || /Android|iPhone|Mobile/i.test(navigator.userAgent)
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f5f6fa;
}

/* ===== 左侧品牌面板 ===== */
.brand-panel {
  flex: 1.1;
  position: relative;
  overflow: hidden;
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  color: #fff;
  background: linear-gradient(150deg, #4f7cff 0%, #6a5bf0 55%, #9456e8 100%);
}
.brand-logo { font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.brand-slogan { margin-top: 15vh; }
.brand-slogan h1 { font-size: 30px; margin: 0 0 12px; letter-spacing: 1px; }
.brand-slogan p { font-size: 14px; opacity: 0.85; margin: 0; }
.brand-features { margin: 40px 0 0; padding: 0; list-style: none; }
.brand-features li {
  font-size: 14px; opacity: 0.92; margin-bottom: 14px;
  padding-left: 2px;
}
.brand-circles i {
  position: absolute; border-radius: 50%; background: rgba(255, 255, 255, 0.08);
}
.brand-circles i:nth-child(1) { width: 260px; height: 260px; right: -60px; top: -60px; }
.brand-circles i:nth-child(2) { width: 160px; height: 160px; right: 120px; bottom: 40px; }
.brand-circles i:nth-child(3) { width: 90px; height: 90px; left: 40%; bottom: -30px; }

/* ===== 右侧表单面板 ===== */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.card {
  width: 400px;
  max-width: 100%;
  border: none;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(31, 45, 61, 0.10);
  padding: 6px 10px;
}
.card-title { text-align: center; margin: 6px 0 4px; font-size: 22px; color: #1f2329; }
.card-sub { text-align: center; color: #8a8f99; font-size: 13px; margin: 0 0 22px; }
.steps { margin-bottom: 22px; }
.submit-btn {
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
  background: linear-gradient(90deg, #4f7cff, #7a5af8);
  border: none;
}
.submit-btn:hover { opacity: 0.92; }
.switch { text-align: center; margin-top: 14px; color: #4f7cff; cursor: pointer; font-size: 13px; }
.switch:hover { text-decoration: underline; }
.footer-tip { margin-top: 22px; color: #b0b5bf; font-size: 12px; }

/* 窄屏：隐藏左侧面板 */
@media (max-width: 900px) {
  .brand-panel { display: none; }
  .login-page { background: linear-gradient(150deg, #4f7cff, #9456e8); }
  .card { box-shadow: 0 8px 30px rgba(0, 0, 0, 0.18); }
}
</style>
