<template>
  <div>
    <h3 class="m-title">👤 我的</h3>

    <el-card shadow="never" class="m-card">
      <template #header>
        <div class="card-header">
          <span>📈 最近记录</span>
          <el-button type="primary" size="small" round @click="openRecord">记录今日状态</el-button>
        </div>
      </template>
      <div v-if="rows.length" class="record-list">
        <div v-for="r in rows.slice(-7).reverse()" :key="r.id" class="record">
          <span>{{ r.recordDate }}</span>
          <span>{{ r.weightKg }} kg<span v-if="r.bodyFat"> · {{ r.bodyFat }}%</span></span>
        </div>
      </div>
      <el-empty v-else :image-size="50" description="暂无记录，点右上角记录第一条吧" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="记录今日状态" width="88%" style="max-width: 400px">
      <div class="record-form">
        <div class="field">
          <span>体重 (kg)</span>
          <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" size="large" />
        </div>
        <div class="field">
          <span>体脂率 (%)</span>
          <el-input-number v-model="form.bodyFat" :min="1" :max="60" :precision="1" size="large" placeholder="选填" />
        </div>
        <div class="field">
          <span>日期</span>
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD"
                          size="large" style="width: 180px" />
        </div>
      </div>
      <template #footer>
        <el-button size="large" type="primary" style="width: 100%" :loading="saving" @click="save">
          保存
        </el-button>
      </template>
    </el-dialog>

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
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive<{ weightKg: number; bodyFat?: number; recordDate?: string }>({
  weightKg: 65,
  recordDate: new Date().toISOString().slice(0, 10),
})

async function load() {
  const res: any = await metricApi.list(30)
  rows.value = res.data
}

function openRecord() {
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await metricApi.add(form)
    ElMessage.success('已保存')
    dialogVisible.value = false
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
.card-header { display: flex; justify-content: space-between; align-items: center; }
.record-list { display: flex; flex-direction: column; gap: 8px; }
.record { display: flex; justify-content: space-between; padding: 10px 12px;
  background: #f7f8fa; border-radius: 8px; color: #555; }
.record-form { display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; justify-content: space-between; align-items: center; }
.field > span { color: #666; }
</style>
