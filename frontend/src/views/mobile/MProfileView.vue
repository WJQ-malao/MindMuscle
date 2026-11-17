<template>
  <div>
    <h3 class="m-title">👤 我的</h3>

    <el-card shadow="never" class="m-card">
      <template #header>⚖️ 记录今日体重</template>
      <div class="metric-form">
        <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" size="large" style="flex:1" />
        <el-input-number v-model="form.bodyFat" :min="1" :max="60" :precision="1" size="large" style="flex:1" placeholder="体脂%" />
      </div>
      <el-button type="primary" size="large" round style="width: 100%; margin-top: 12px"
                 :loading="saving" @click="save">
        保存
      </el-button>
    </el-card>

    <el-card shadow="never" class="m-card">
      <template #header>📈 最近记录</template>
      <div v-if="rows.length" class="record-list">
        <div v-for="r in rows.slice(-7).reverse()" :key="r.id" class="record">
          <span>{{ r.recordDate }}</span>
          <span>{{ r.weightKg }} kg<span v-if="r.bodyFat"> · {{ r.bodyFat }}%</span></span>
        </div>
      </div>
      <el-empty v-else :image-size="50" description="暂无记录" />
    </el-card>

    <el-button size="large" round style="width: 100%; margin-top: 16px" @click="logout">
      退出登录
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { metricApi, type BodyMetric } from '../../api'

const router = useRouter()
const rows = ref<BodyMetric[]>([])
const saving = ref(false)
const form = reactive<{ weightKg: number; bodyFat?: number }>({ weightKg: 65 })

async function load() {
  const res: any = await metricApi.list(30)
  rows.value = res.data
}

async function save() {
  saving.value = true
  try {
    await metricApi.add(form)
    ElMessage.success('已保存')
    load()
  } finally {
    saving.value = false
  }
}

function logout() {
  localStorage.clear()
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.m-title { margin: 4px 4px 14px; }
.m-card { border-radius: 12px; }
.metric-form { display: flex; gap: 10px; }
.record-list { display: flex; flex-direction: column; gap: 8px; }
.record { display: flex; justify-content: space-between; padding: 10px 12px;
  background: #f7f8fa; border-radius: 8px; color: #555; }
</style>
