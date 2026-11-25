<template>
  <div class="login-page" :class="{ 'splash-on': splash.show }">
    <!-- 漂浮装饰 -->
    <div class="shape s1" /><div class="shape s2" /><div class="shape s3" />
    <div class="float-card fc1" /><div class="float-card fc2" />

    <!-- 左侧品牌视觉（splash 出现时隐藏，避免过渡页淡出时透出登录表单） -->
    <aside v-show="!splash.show" class="brand">
      <div class="brand-head">
        <div class="app-icon">🏋️</div>
        <span class="brand-name">MindMuscle</span>
      </div>
      <div class="brand-body">
        <h1>智能健身<br />科学训练</h1>
        <p>RAG 知识库 + AI 教练 + 数据看板<br />你的个性化健身伙伴</p>
        <ul class="brand-points">
          <li><b>AI 教练</b>基于知识库的流式对话，一句话生成训练计划</li>
          <li><b>数据看板</b>体重趋势、肌群分布、打卡热力图一目了然</li>
          <li><b>训练计划</b>按周管理，逐组打卡，容量自动统计</li>
        </ul>
      </div>
      <div class="brand-foot">MindMuscle · 基于 SpringBoot 与 RAG 的个性化健身系统</div>
    </aside>

    <!-- 右侧登录卡片（splash 出现时隐藏） -->
    <main v-show="!splash.show" class="panel">
      <div class="card">
        <h2 class="card-title">欢迎来到 MindMuscle</h2>
        <p class="card-sub">{{ isRegister ? '创建账号，开始你的健身之旅' : '登录以继续使用' }}</p>

        <div class="tabs">
          <button class="tab" :class="{ active: !isRegister }" @click="isRegister = false; step = 1">登录</button>
          <button class="tab" :class="{ active: isRegister }" @click="isRegister = true; step = 1">注册</button>
          <span class="tab-ink" :style="isRegister ? 'transform: translateX(100%)' : 'transform: translateX(0)'" />
        </div>

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
          <el-button class="submit" type="primary" style="width: 100%" :loading="loading" @click="onSubmit">
            {{ submitText }}
          </el-button>
        </el-form>
      </div>
      <div class="panel-foot">欢迎页过渡 · 登录成功后将进入系统</div>
    </main>

    <!-- 登录成功欢迎过渡页（停留 ~2 秒后渐变进入系统） -->
    <transition name="splash">
      <div v-if="splash.show" class="splash" :class="{ leaving: splash.leaving }">
        <div class="splash-inner">
          <div class="splash-logo">🏋️</div>
          <div class="splash-title">{{ splashText }}</div>
          <div class="splash-name">{{ splash.nickname }}</div>
          <div class="splash-bar"><i /></div>
          <div class="splash-tip">正在进入系统…</div>
        </div>
      </div>
    </transition>
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

/** 欢迎过渡页状态 */
const splash = reactive({ show: false, leaving: false, nickname: '' })
const splashText = computed(() => (isRegister.value ? '注册成功' : '登录成功'))

const submitText = computed(() => {
  if (!isRegister.value) return '登 录'
  return step.value === 1 ? '下一步' : '完成注册'
})

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
    enterSplash(res.data.nickname)
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
    enterSplash(res.data.nickname)
  } catch {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

/** 欢迎过渡页：停留约 1.6s → 渐变淡出 0.4s → 进入系统 */
function enterSplash(nickname: string) {
  splash.nickname = nickname || form.username
  splash.show = true
  splash.leaving = false
  setTimeout(() => {
    splash.leaving = true
  }, 1600)
  setTimeout(() => {
    router.push(isMobile() ? '/m/home' : '/')
  }, 2050)
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
/* 欢迎过渡期间隐藏底层一切内容，防止淡出时透出登录页 */
.login-page.splash-on .shape,
.login-page.splash-on .float-card {
  visibility: hidden;
}

/* ===== 页面基底：浅色高级感 + 漂浮几何 ===== */
.login-page {
  min-height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  background: #f4f6fb;
}
.shape {
  position: absolute; border-radius: 50%; filter: blur(70px); opacity: 0.55; pointer-events: none;
}
.s1 { width: 420px; height: 420px; background: #cfdcff; top: -120px; left: -120px; animation: drift 14s ease-in-out infinite; }
.s2 { width: 320px; height: 320px; background: #e4d6ff; bottom: -100px; left: 38%; animation: drift 11s ease-in-out infinite reverse; }
.s3 { width: 260px; height: 260px; background: #d2f0ff; top: 10%; right: -80px; animation: drift 16s ease-in-out infinite; }
.float-card {
  position: absolute; border-radius: 22px; pointer-events: none;
  background: linear-gradient(135deg, #4f7cff, #a55cf0); opacity: 0.10;
}
.fc1 { width: 110px; height: 110px; left: 34%; top: 14%; transform: rotate(18deg); animation: bob 9s ease-in-out infinite; }
.fc2 { width: 70px; height: 70px; left: 6%; bottom: 12%; transform: rotate(-12deg); animation: bob 12s ease-in-out infinite reverse; }
@keyframes drift {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -24px); }
}
@keyframes bob {
  0%, 100% { transform: rotate(18deg) translateY(0); }
  50% { transform: rotate(18deg) translateY(-16px); }
}

/* ===== 左侧品牌视觉 ===== */
.brand {
  flex: 1.15;
  position: relative; z-index: 1;
  padding: 44px 40px 28px 56px;
  display: flex; flex-direction: column;
}
.brand-head { display: flex; align-items: center; gap: 12px; }
.app-icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, #4f7cff, #9456e8);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; box-shadow: 0 8px 20px rgba(94, 108, 240, 0.35);
}
.brand-name { font-size: 20px; font-weight: 700; color: #1f2329; letter-spacing: 0.4px; }
.brand-body { margin-top: 12vh; }
.brand-body h1 {
  font-size: 40px; line-height: 1.25; margin: 0 0 14px;
  color: #1f2329; letter-spacing: 2px; font-weight: 800;
}
.brand-body p { font-size: 14px; color: #6b7280; line-height: 1.8; margin: 0 0 34px; }
.brand-points { list-style: none; margin: 0; padding: 0; }
.brand-points li {
  font-size: 13px; color: #6b7280; margin-bottom: 14px;
  padding: 10px 16px; background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 12px; backdrop-filter: blur(6px);
  max-width: 380px;
}
.brand-points b { color: #1f2329; margin-right: 10px; }
.brand-foot { margin-top: auto; font-size: 12px; color: #a0a6b3; }

/* ===== 右侧卡片 ===== */
.panel {
  flex: 1; position: relative; z-index: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 24px;
}
.card {
  width: 400px; max-width: 100%;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(31, 45, 61, 0.10);
  padding: 34px 34px 28px;
}
.card-title { text-align: center; margin: 0 0 6px; font-size: 22px; color: #1f2329; }
.card-sub { text-align: center; color: #8a8f99; font-size: 13px; margin: 0 0 22px; }

/* 分段选项卡 */
.tabs {
  position: relative; display: flex;
  background: #f2f4f9; border-radius: 12px; padding: 4px;
  margin-bottom: 22px;
}
.tab {
  flex: 1; position: relative; z-index: 1;
  border: none; background: transparent; cursor: pointer;
  padding: 9px 0; font-size: 14px; color: #6b7280; border-radius: 9px;
  transition: color 0.25s;
}
.tab.active { color: #1f2329; font-weight: 600; }
.tab-ink {
  position: absolute; top: 4px; bottom: 4px; left: 4px; width: calc(50% - 4px);
  background: #fff; border-radius: 9px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.08);
  transition: transform 0.28s ease;
}
.steps { margin-bottom: 20px; }

/* 填充式输入框（高级感） */
.card :deep(.el-input__wrapper) {
  background: #f5f6fa;
  box-shadow: none;
  border-radius: 10px;
  padding: 2px 14px;
}
.card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(79, 124, 255, 0.18);
  background: #fff;
}
.submit {
  height: 44px; font-size: 15px; letter-spacing: 6px;
  border: none; border-radius: 10px; margin-top: 4px;
  background: linear-gradient(90deg, #4f7cff, #9456e8);
  box-shadow: 0 8px 20px rgba(94, 108, 240, 0.32);
  transition: transform 0.15s ease, opacity 0.2s;
}
.submit:hover { opacity: 0.93; transform: translateY(-1px); }
.panel-foot { margin-top: 18px; font-size: 12px; color: #b0b5bf; }

/* ===== 欢迎过渡页 ===== */
.splash {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(150deg, #4f7cff 0%, #6a5bf0 55%, #9456e8 100%);
  transition: opacity 0.4s ease;
}
.splash.leaving { opacity: 0; }
.splash-inner { text-align: center; color: #fff; animation: pop 0.45s ease both; }
.splash-logo {
  width: 84px; height: 84px; margin: 0 auto 18px;
  border-radius: 22px; background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex; align-items: center; justify-content: center; font-size: 44px;
  backdrop-filter: blur(4px);
  animation: pulse 1.6s ease-in-out infinite;
}
.splash-title { font-size: 20px; font-weight: 700; letter-spacing: 2px; }
.splash-name { font-size: 14px; opacity: 0.85; margin-top: 8px; }
.splash-bar {
  width: 200px; height: 4px; margin: 26px auto 12px;
  background: rgba(255, 255, 255, 0.25); border-radius: 4px; overflow: hidden;
}
.splash-bar i {
  display: block; height: 100%; width: 0;
  background: #fff; border-radius: 4px;
  animation: fill 1.55s linear forwards;
}
.splash-tip { font-size: 12px; opacity: 0.7; }
@keyframes fill { to { width: 100%; } }
@keyframes pop {
  from { opacity: 0; transform: translateY(14px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 窄屏：隐藏左侧，保留浅色底与漂浮装饰 */
@media (max-width: 900px) {
  .brand { display: none; }
  .card { width: 92vw; }
  .panel-foot { display: none; }
}
</style>
