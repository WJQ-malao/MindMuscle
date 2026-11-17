<template>
  <div>
    <h3 class="m-title">🏋️ 训练计划</h3>

    <el-card v-if="activePlan" shadow="never" class="m-card">
      <div class="plan-title">{{ activePlan.title }}</div>
      <el-tabs v-model="day">
        <el-tab-pane v-for="d in days" :key="d.value" :label="d.label" :name="String(d.value)">
          <div v-if="itemsByDay(d.value).length" class="item-list">
            <div v-for="it in itemsByDay(d.value)" :key="it.id" class="train-item">
              <div>
                <div class="exercise">{{ it.exerciseName }}</div>
                <div class="detail">{{ it.muscleGroup }} · {{ it.sets }}组x{{ it.reps }}{{ it.muscleGroup === '有氧' ? '分钟' : '次' }}</div>
              </div>
              <el-button type="primary" round size="small" @click="openCheckin(it)">打卡</el-button>
            </div>
          </div>
          <el-empty v-else :image-size="50" description="这天休息日" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-card v-else shadow="never" class="m-card">
      <el-empty description="还没有训练计划">
        <el-button type="primary" round @click="$router.push('/m/chat')">让 AI 帮你生成</el-button>
      </el-empty>
    </el-card>

    <el-dialog v-model="checkinVisible" title="训练打卡" width="90%" style="max-width: 400px">
      <div class="checkin-form">
        <div class="checkin-exercise">{{ currentItem?.exerciseName }}</div>
        <div class="field">
          <span>完成组数</span>
          <el-input-number v-model="form.setsDone" :min="1" :max="30" size="large" />
        </div>
        <div class="field">
          <span>每组次数</span>
          <el-input-number v-model="form.repsDone" :min="1" :max="100" size="large" />
        </div>
        <div class="field">
          <span>重量(kg)</span>
          <el-input-number v-model="form.weightUsed" :min="0" :max="500" :precision="1" size="large" />
        </div>
      </div>
      <template #footer>
        <el-button size="large" style="width: 100%" type="primary" :loading="saving" @click="doCheckin">
          确认打卡
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workoutApi, type WorkoutItem, type WorkoutPlan } from '../../api'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const day = ref(String(new Date().getDay() || 7))

const activePlan = ref<WorkoutPlan>()
const items = ref<WorkoutItem[]>([])
const checkinVisible = ref(false)
const saving = ref(false)
const currentItem = ref<WorkoutItem>()
const form = reactive({ setsDone: 4, repsDone: 10, weightUsed: 0 })

const itemsByDay = (d: number) => items.value.filter((i) => i.dayOfWeek === d)

async function load() {
  const p: any = await workoutApi.plans()
  activePlan.value = p.data.find((x: WorkoutPlan) => x.status === 'ACTIVE')
  if (activePlan.value) {
    const it: any = await workoutApi.items(activePlan.value.id)
    items.value = it.data
  }
}

function openCheckin(item: WorkoutItem) {
  currentItem.value = item
  form.setsDone = item.sets
  form.repsDone = item.reps
  checkinVisible.value = true
}

async function doCheckin() {
  saving.value = true
  try {
    await workoutApi.checkin({ itemId: currentItem.value!.id, ...form })
    ElMessage.success('打卡成功')
    checkinVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.m-title { margin: 4px 4px 14px; }
.m-card { border-radius: 12px; }
.plan-title { font-weight: 700; margin-bottom: 8px; }
.item-list { display: flex; flex-direction: column; gap: 10px; }
.train-item { display: flex; justify-content: space-between; align-items: center;
  padding: 12px; background: #f7f8fa; border-radius: 10px; }
.exercise { font-weight: 600; font-size: 16px; }
.detail { color: #999; font-size: 12px; margin-top: 2px; }
.checkin-form { display: flex; flex-direction: column; gap: 14px; }
.checkin-exercise { font-weight: 700; font-size: 18px; text-align: center; }
.field { display: flex; justify-content: space-between; align-items: center; }
.field > span { color: #666; }
</style>
