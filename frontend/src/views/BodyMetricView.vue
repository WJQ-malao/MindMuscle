<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>⚖️ 身体数据记录</span>
          <el-button type="primary" @click="openRecord">记录今日状态</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="recordDate" label="日期" width="160" />
        <el-table-column prop="weightKg" label="体重 (kg)" />
        <el-table-column prop="bodyFat" label="体脂率 (%)" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无记录，点右上角记录第一条吧" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="记录今日状态" width="440px">
      <el-form label-width="90px">
        <el-form-item label="体重 (kg)">
          <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体脂率 (%)">
          <el-input-number v-model="form.bodyFat" :min="1" :max="60" :precision="1" size="large" style="width: 100%" placeholder="选填" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD"
                          size="large" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { metricApi, type BodyMetric } from '../api'

const rows = ref<BodyMetric[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive<{ weightKg: number; bodyFat?: number; recordDate?: string }>({
  weightKg: 65,
  recordDate: new Date().toISOString().slice(0, 10),
})

async function load() {
  loading.value = true
  try {
    const res: any = await metricApi.list()
    rows.value = res.data
  } finally {
    loading.value = false
  }
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

async function remove(row: BodyMetric) {
  await ElMessageBox.confirm('确定删除这条记录吗？', '提示', { type: 'warning' })
  await metricApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
