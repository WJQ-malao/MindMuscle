<template>
  <div>
    <el-card shadow="hover">
      <div class="toolbar">
        <el-select v-model="currentPlanId" placeholder="选择训练计划" style="width: 320px" @change="loadItems">
          <el-option v-for="p in plans" :key="p.id" :value="p.id"
                     :label="`${p.title}（${p.source === 'AI' ? 'AI生成' : '手动创建'} · ${p.status === 'ACTIVE' ? '进行中' : '已归档'}）`" />
        </el-select>
        <span v-if="currentPlan" class="goal-chip" :class="{ archived: currentPlan.status !== 'ACTIVE' }">
          {{ currentPlan.goal }}
        </span>
        <div class="toolbar-right">
          <!-- 有计划→编辑（打开动作选择器）；无计划→创建（同样先进选择器挑动作） -->
          <template v-if="plans.length">
            <el-button type="primary" plain @click="openAddPicker">编辑计划</el-button>
          </template>
          <el-button v-else type="primary" @click="openNewPlanPicker">创建计划</el-button>
        </div>
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
          <div class="day-actions">
            <el-button type="primary" plain round size="small" @click="openAddPicker">＋ 添加动作</el-button>
            <span v-if="!itemsByDay(d.value).length" class="rest-tip">这一天还没有安排动作</span>
          </div>
          <el-empty v-if="!itemsByDay(d.value).length" :image-size="60" description="这天是休息日" />
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else-if="!loading && !plans.length" description="还没有训练计划">
        <el-button type="primary" @click="openNewPlanPicker">创建计划</el-button>
        <el-button @click="$router.push('/chat')">让 AI 帮你生成</el-button>
      </el-empty>
      <el-empty v-else-if="!loading" description="该计划还没有训练项">
        <el-button type="primary" @click="openAddPicker">添加动作</el-button>
      </el-empty>
    </el-card>

    <!-- 动作选择器：创建计划篮子 / 编辑当前计划 双模式 -->
    <ExercisePicker :visible="pickerVisible" :mode="pickerMode" :plan-id="currentPlanId"
                    :default-day="Number(day)"
                    @close="pickerVisible = false" @finish="onPickerFinish" @apply="onPickerApply" />

    <!-- 新建计划：填信息创建 -->
    <el-dialog v-model="newPlanVisible" title="创建训练计划" width="440px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
                :title="`已选 ${basket.length} 个动作，将按顺序轮流分配到每周各训练日，创建后可微调`" />
      <el-form label-width="110px">
        <el-form-item label="计划名称">
          <el-input v-model="newPlanForm.title" placeholder="如：上肢力量强化" maxlength="200" />
        </el-form-item>
        <el-form-item label="目标">
          <el-select v-model="newPlanForm.goal" style="width: 100%">
            <el-option v-for="g in ['增肌', '减脂', '塑形', '力量', '耐力', '保持']" :key="g" :value="g" :label="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="每周训练天数">
          <el-input-number v-model="newPlanForm.daysPerWeek" :min="1" :max="7" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newPlanVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSaving" :disabled="!basket.length" @click="savePlan">创建计划</el-button>
      </template>
    </el-dialog>

    <!-- 打卡 -->
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
import ExercisePicker, { type ApplyDiff, type BasketItem } from './ExercisePicker.vue'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const day = ref(String(new Date().getDay() || 7))

const plans = ref<WorkoutPlan[]>([])
const items = ref<WorkoutItem[]>([])
const loading = ref(false)
const currentPlanId = ref<number>()

const newPlanVisible = ref(false)
const planSaving = ref(false)
const pickerVisible = ref(false)
const pickerMode = ref<'new-plan' | 'add-to-plan'>('new-plan')
const basket = ref<BasketItem[]>([])
const newPlanForm = reactive({ title: '', goal: '增肌', daysPerWeek: 4 })

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

// ---- 动作选择器：两种模式统一收口 ----

function openNewPlanPicker() {
  pickerMode.value = 'new-plan'
  pickerVisible.value = true
}

function openAddPicker() {
  pickerMode.value = 'add-to-plan'
  pickerVisible.value = true
}

async function onPickerFinish(selected: BasketItem[]) {
  basket.value = selected
  pickerVisible.value = false
  newPlanForm.title = ''
  newPlanVisible.value = true
}

/** 编辑计划：按差异一次性提交 新增/修改/删除 */
async function onPickerApply(diff: ApplyDiff) {
  if (!currentPlanId.value) return
  let addOk = 0
  let updateOk = 0
  let removeOk = 0
  for (const r of diff.removed) {
    try {
      await workoutApi.removeItem(r.itemId)
      removeOk++
    } catch { /* 拦截器已提示 */ }
  }
  for (const b of diff.updated) {
    try {
      await workoutApi.updateItem(b.itemId!, {
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? 1, sets: b.sets, reps: b.reps,
      })
      updateOk++
    } catch { /* 拦截器已提示 */ }
  }
  for (const b of diff.added) {
    try {
      await workoutApi.addItem(currentPlanId.value, {
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? Number(day.value), sets: b.sets, reps: b.reps,
      })
      addOk++
    } catch { /* 拦截器已提示 */ }
  }
  const parts: string[] = []
  if (addOk) parts.push(`新增${addOk}`)
  if (updateOk) parts.push(`修改${updateOk}`)
  if (removeOk) parts.push(`删除${removeOk}`)
  ElMessage.success(parts.length ? `已保存：${parts.join('，')}` : '没有需要保存的修改')
  pickerVisible.value = false
  loadItems()
}

async function savePlan() {
  if (!newPlanForm.title.trim()) {
    ElMessage.warning('请填写计划名称')
    return
  }
  planSaving.value = true
  try {
    const res: any = await workoutApi.createPlan({
      title: newPlanForm.title,
      goal: newPlanForm.goal,
      daysPerWeek: newPlanForm.daysPerWeek,
      items: basket.value.map((b) => ({
        exerciseName: b.name, muscleGroup: b.muscleGroup, sets: b.sets, reps: b.reps,
      })),
    })
    ElMessage.success('计划已创建，动作已分配到各训练日，可进入编辑模式微调')
    newPlanVisible.value = false
    await load()
    currentPlanId.value = res.data.id
    await loadItems()
  } finally {
    planSaving.value = false
  }
}

// ---- 打卡 ----

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
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; flex-wrap: wrap; }
.toolbar > * { flex-shrink: 0; }
.toolbar-right { display: flex; gap: 10px; }
.goal-chip {
  padding: 7px 14px; border-radius: 8px; background: #ecf5ff; color: #409eff;
  font-size: 14px; line-height: 1;
}
.goal-chip.archived { background: #f4f4f5; color: #909399; }
.day-actions { margin-top: 10px; display: flex; align-items: center; gap: 12px; }
.rest-tip { color: #999; font-size: 12px; }
</style>
