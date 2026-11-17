<template>
  <div>
    <el-card shadow="hover">
      <template #header>我的训练计划</template>
      <el-table :data="plans" v-loading="loading" stripe>
        <el-table-column prop="title" label="计划名称" min-width="200" />
        <el-table-column prop="goal" label="目标" width="100" />
        <el-table-column prop="source" label="来源" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'AI' ? 'success' : 'info'">{{ row.source }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'primary' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" @click="showItems(row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && plans.length === 0" description="还没有训练计划，去 AI 教练页让 AI 帮你生成一个吧" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentPlan?.title" width="720px">
      <el-table :data="items" stripe max-height="480">
        <el-table-column prop="dayOfWeek" label="训练日" width="90">
          <template #default="{ row }">{{ dayNames[row.dayOfWeek] }}</template>
        </el-table-column>
        <el-table-column prop="exerciseName" label="动作" />
        <el-table-column prop="muscleGroup" label="肌群" width="90" />
        <el-table-column label="安排" width="130">
          <template #default="{ row }">{{ row.sets }} 组 x {{ row.reps }}{{ row.muscleGroup === '有氧' ? ' 分钟' : ' 次' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openCheckin(row)">打卡</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="checkinVisible" title="训练打卡" width="420px">
      <el-form label-width="90px">
        <el-form-item label="动作">
          <span>{{ currentItem?.exerciseName }}</span>
        </el-form-item>
        <el-form-item label="完成组数">
          <el-input-number v-model="checkinForm.setsDone" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="每组次数">
          <el-input-number v-model="checkinForm.repsDone" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="重量 (kg)">
          <el-input-number v-model="checkinForm.weightUsed" :min="0" :max="500" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkinSaving" @click="doCheckin">确认打卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workoutApi, type WorkoutItem, type WorkoutPlan } from '../api'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const plans = ref<WorkoutPlan[]>([])
const items = ref<WorkoutItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const checkinVisible = ref(false)
const checkinSaving = ref(false)
const currentPlan = ref<WorkoutPlan>()
const currentItem = ref<WorkoutItem>()
const checkinForm = reactive({ setsDone: 4, repsDone: 10, weightUsed: 0 })

async function load() {
  loading.value = true
  try {
    const res: any = await workoutApi.plans()
    plans.value = res.data
  } finally {
    loading.value = false
  }
}

async function showItems(plan: WorkoutPlan) {
  currentPlan.value = plan
  const res: any = await workoutApi.items(plan.id)
  items.value = res.data
  dialogVisible.value = true
}

function openCheckin(item: WorkoutItem) {
  currentItem.value = item
  checkinForm.setsDone = item.sets
  checkinForm.repsDone = item.reps
  checkinVisible.value = true
}

async function doCheckin() {
  checkinSaving.value = true
  try {
    await workoutApi.checkin({ itemId: currentItem.value!.id, ...checkinForm })
    ElMessage.success('打卡成功')
    checkinVisible.value = false
  } finally {
    checkinSaving.value = false
  }
}

onMounted(load)
</script>
