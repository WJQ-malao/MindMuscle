<template>
  <div>
    <el-card shadow="hover">
      <template #header>记录身体数据</template>
      <el-form inline>
        <el-form-item label="体重 (kg)">
          <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" />
        </el-form-item>
        <el-form-item label="体脂率 (%)">
          <el-input-number v-model="form.bodyFat" :min="1" :max="60" :precision="1" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>历史记录</template>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { metricApi, type BodyMetric } from '../api'

const rows = ref<BodyMetric[]>([])
const loading = ref(false)
const saving = ref(false)
const form = reactive<{ weightKg: number; bodyFat?: number; recordDate?: string }>({ weightKg: 65 })

async function load() {
  loading.value = true
  try {
    const res: any = await metricApi.list()
    rows.value = res.data
  } finally {
    loading.value = false
  }
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

async function remove(row: BodyMetric) {
  await ElMessageBox.confirm('确定删除这条记录吗？', '提示', { type: 'warning' })
  await metricApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
