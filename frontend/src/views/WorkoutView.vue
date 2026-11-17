<template>
  <div>
    <el-card shadow="hover">
      <div class="toolbar">
        <el-select v-model="currentPlanId" placeholder="选择训练计划" style="width: 320px" @change="loadItems">
          <el-option v-for="p in plans" :key="p.id" :value="p.id"
                     :label="`${p.title}（${p.source === 'AI' ? 'AI生成' : '手动创建'} · ${p.status === 'ACTIVE' ? '进行中' : '已归档'}）`" />
        </el-select>
        <el-tag v-if="currentPlan" :type="currentPlan.status === 'ACTIVE' ? 'primary' : 'info'" size="large">
          {{ currentPlan.goal }}
        </el-tag>
      </div>

      <el-tabs v-if="items.length" v-model="day">
        <el-tab-pane v-for="d in days" :key="d.value" :label="d.label" :name="String(d.value)">
          <el-table :data="itemsByDay(d.value)" stripe>
            <el-table-column prop="exerciseName" label="动作" min-width="160" />
            <el-table-column prop="muscleGroup" label="肌群" width="90" />
            <el-table-column label="安排" width="150">
              <template #default="{ row }">{{ row.sets }} 组 x {{ row.reps }}{{ row.muscleGroup === '有氧' ? ' 分钟' : ' 次' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="openCheckin(row)">打卡</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!itemsByDay(d.value).length" :image-size="60" description="这天是休息日" />
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else-if="!loading" description="该计划还没有训练项">
        <el-button type="primary" @click="$router.push('/chat')">去 AI 教练生成一个计划</el-button>
      </el-empty>
    </el-card>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workoutApi, type WorkoutItem, type WorkoutPlan } from '../api'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const day = ref(String(new Date().getDay() || 7))

const plans = ref<WorkoutPlan[]>([])
const items = ref<WorkoutItem[]>([])
const loading = ref(false)
const currentPlanId = ref<number>()
const checkinVisible = ref(false)
const checkinSaving = ref(false)
const currentItem = ref<WorkoutItem>()
const checkinForm = reactive({ setsDone: 4, repsDone: 10, weightUsed: 0 })

const currentPlan = computed(() => plans.value.find((p) => p.id === currentPlanId.value))
const itemsByDay = (d: number) => items.value.filter((i) => i.dayOfWeek === d)

async function load() {
  loading.value = true
  try {
    const res: any = await workoutApi.plans()
    plans.value = res.data
    // 默认选中进行中的计划，和手机端"今天练什么"的体验一致
    const active = res.data.find((p: WorkoutPlan) => p.status === 'ACTIVE') ?? res.data[0]
    currentPlanId.value = active?.id
    if (active) await loadItems()
  } finally {
    loading.value = false
  }
}

async function loadItems() {
  if (!currentPlanId.value) {
    items.value = []
    return
  }
  const res: any = await workoutApi.items(currentPlanId.value)
  items.value = res.data
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

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
</style>
