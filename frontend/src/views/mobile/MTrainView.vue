<template>
  <div>
    <div class="m-header">
      <h3 class="m-title">🏋️ 训练计划</h3>
      <div class="m-actions">
        <el-button size="small" @click="pickerVisible = true">创建计划</el-button>
        <el-button v-if="activePlan" size="small" plain
                   :type="editMode ? 'warning' : 'primary'" @click="editMode = !editMode">
          {{ editMode ? '完成' : '编辑' }}
        </el-button>
      </div>
    </div>

    <el-card v-if="activePlan" shadow="never" class="m-card">
      <div class="plan-title">{{ activePlan.title }}</div>
      <el-tabs v-model="day">
        <el-tab-pane v-for="d in days" :key="d.value" :label="d.label" :name="String(d.value)">
          <div v-if="itemsByDay(d.value).length" class="item-list">
            <div v-for="it in itemsByDay(d.value)" :key="it.id" class="train-item">
              <div>
                <div class="exercise">{{ it.exerciseName }}</div>
                <div class="detail">{{ it.muscleGroup }} · {{ it.weightKg ? it.weightKg + 'kg · ' : '' }}{{ it.sets }}组x{{ it.reps }}{{ it.muscleGroup === '有氧' ? '分钟' : '次' }}</div>
              </div>
              <template v-if="editMode">
                <div class="row-actions">
                  <el-button size="small" @click="openItemEdit(it)">编辑</el-button>
                  <el-button type="danger" size="small" @click="removeItem(it)">删除</el-button>
                </div>
              </template>
              <el-button v-if="checkedToday.has(it.id)" type="danger" plain round size="small"
                         @click="undoCheckin(it)">已打卡</el-button>
              <el-button v-else type="success" round size="small" @click="openCheckin(it)">打卡</el-button>
            </div>
          </div>
          <el-empty v-else :image-size="50" description="这天休息日" />
          <div v-if="editMode" class="day-actions">
            <el-button type="primary" plain round size="small" @click="openItemAdd(d.value)">＋ 添加动作</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-card v-else shadow="never" class="m-card">
      <el-empty description="还没有训练计划">
        <el-button type="primary" round @click="pickerVisible = true">创建计划</el-button>
        <el-button type="primary" round plain @click="$router.push('/m/chat')">让 AI 帮你生成</el-button>
      </el-empty>
    </el-card>

    <!-- 添加/编辑动作（与 PC 端同一套字段） -->
    <el-dialog v-model="itemVisible" :title="editingItem ? '编辑动作' : '添加动作'" width="90%" style="max-width: 420px">
      <el-form label-width="90px">
        <el-form-item label="肌群">
          <el-select v-model="itemForm.muscleGroup" style="width: 100%" @change="onGroupChange">
            <el-option v-for="m in muscleOptions" :key="m" :value="m" :label="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="itemForm.exerciseName" filterable style="width: 100%"
                     placeholder="选择或输入动作名称" allow-create>
            <el-option v-for="e in exercisesByGroup" :key="e.id" :value="e.name" :label="e.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练日">
          <el-select v-model="itemForm.dayOfWeek" style="width: 100%">
            <el-option v-for="d in days" :key="d.value" :value="d.value" :label="d.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="组数">
          <el-input-number v-model="itemForm.sets" :min="1" :max="30" />
        </el-form-item>
        <el-form-item :label="itemForm.muscleGroup === '有氧' ? '时长(分钟)' : '每组次数'">
          <el-input-number v-model="itemForm.reps" :min="1" :max="100" />
        </el-form-item>
        <el-form-item v-if="itemForm.muscleGroup !== '有氧'" label="重量(kg)">
          <el-input-number v-model="itemForm.weightKg" :min="0" :max="500" :precision="1" placeholder="自重" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建计划：先挑动作进篮子（复用 PC 端挑选器） -->
    <ExercisePicker :visible="pickerVisible" mode="new-plan" @close="pickerVisible = false" @finish="onBasketFinish" />

    <!-- 新建计划：填信息创建 -->
    <el-dialog v-model="newPlanVisible" title="创建训练计划" width="90%" style="max-width: 420px">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exerciseApi, workoutApi, type Exercise, type WorkoutItem, type WorkoutPlan } from '../../api'
import ExercisePicker, { type BasketItem } from '../ExercisePicker.vue'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const muscleOptions = ['胸', '背', '腿', '肩', '手臂', '核心', '有氧']
const day = ref(String(new Date().getDay() || 7))

const activePlan = ref<WorkoutPlan>()
const items = ref<WorkoutItem[]>([])
const exercises = ref<Exercise[]>([])
const editMode = ref(false)

const itemVisible = ref(false)
const itemSaving = ref(false)
const editingItem = ref<WorkoutItem>()
const itemForm = reactive({ exerciseName: '', muscleGroup: '胸', dayOfWeek: 1, sets: 4, reps: 10, weightKg: undefined as number | undefined })

const pickerVisible = ref(false)
const basket = ref<BasketItem[]>([])
const newPlanVisible = ref(false)
const planSaving = ref(false)
const newPlanForm = reactive({ title: '', goal: '增肌', daysPerWeek: 4 })

const checkinVisible = ref(false)
const saving = ref(false)
const currentItem = ref<WorkoutItem>()
const form = reactive({ setsDone: 4, repsDone: 10, weightUsed: 0 })

const itemsByDay = (d: number) => items.value.filter((i) => i.dayOfWeek === d)
/** 当前肌群下的动作库选项 */
const exercisesByGroup = computed(() =>
  exercises.value.filter((e) => e.muscleGroup === itemForm.muscleGroup))

async function load() {
  const p: any = await workoutApi.plans()
  activePlan.value = p.data.find((x: WorkoutPlan) => x.status === 'ACTIVE')
  if (activePlan.value) {
    const it: any = await workoutApi.items(activePlan.value.id)
    items.value = it.data
  } else {
    items.value = []
  }
}

// ---- 动作添加/编辑（与 PC 端同一套接口与字段） ----

function onGroupChange() {
  itemForm.exerciseName = ''
}

function openItemAdd(dayOfWeek: number) {
  editingItem.value = undefined
  itemForm.exerciseName = ''
  itemForm.muscleGroup = '胸'
  itemForm.dayOfWeek = dayOfWeek
  itemForm.sets = 4
  itemForm.reps = 10
  itemVisible.value = true
}

function openItemEdit(row: WorkoutItem) {
  editingItem.value = row
  itemForm.exerciseName = row.exerciseName
  itemForm.muscleGroup = row.muscleGroup
  itemForm.dayOfWeek = row.dayOfWeek
  itemForm.sets = row.sets
  itemForm.reps = row.reps
  itemForm.weightKg = row.weightKg ?? undefined
  itemVisible.value = true
}

async function saveItem() {
  if (!itemForm.exerciseName.trim()) {
    ElMessage.warning('请填写动作名称')
    return
  }
  itemSaving.value = true
  try {
    if (editingItem.value) {
      await workoutApi.updateItem(editingItem.value.id, { ...itemForm })
      ElMessage.success('已保存')
    } else {
      await workoutApi.addItem(activePlan.value!.id, { ...itemForm })
      ElMessage.success('已添加')
    }
    itemVisible.value = false
    const it: any = await workoutApi.items(activePlan.value!.id)
    items.value = it.data
  } finally {
    itemSaving.value = false
  }
}

async function removeItem(row: WorkoutItem) {
  await ElMessageBox.confirm(`确定删除「${row.exerciseName}」吗？`, '提示', { type: 'warning' })
  await workoutApi.removeItem(row.id)
  ElMessage.success('已删除')
  const it: any = await workoutApi.items(activePlan.value!.id)
  items.value = it.data
}

// ---- 新建计划（Keep式：先选动作进篮子，再创建计划） ----

function onBasketFinish(selected: BasketItem[]) {
  basket.value = selected
  pickerVisible.value = false
  newPlanForm.title = ''
  newPlanVisible.value = true
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
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
      })),
    })
    ElMessage.success('计划已创建，动作已分配到各训练日，点「编辑」可微调')
    newPlanVisible.value = false
    await load()
    const it: any = await workoutApi.items(res.data.id)
    activePlan.value = res.data
    items.value = it.data
  } finally {
    planSaving.value = false
  }
}

// ---- 打卡（支持已打卡/未打卡状态切换） ----

const checkedToday = ref<Set<number>>(new Set())

async function loadCheckedToday() {
  const res: any = await workoutApi.todayChecked()
  checkedToday.value = new Set(res.data ?? [])
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
    await loadCheckedToday()
  } finally {
    saving.value = false
  }
}

async function undoCheckin(item: WorkoutItem) {
  await ElMessageBox.confirm(
    `「${item.exerciseName}」今天已打卡，要切换为未打卡吗？`,
    '切换打卡状态',
    { type: 'warning', confirmButtonText: '切换为未打卡', cancelButtonText: '取消' },
  )
  await workoutApi.undoCheckin(item.id)
  ElMessage.success('已切换为未打卡')
  await loadCheckedToday()
}

onMounted(async () => {
  load()
  loadCheckedToday()
  const res: any = await exerciseApi.list()
  exercises.value = res.data
})
</script>

<style scoped>
.m-header { display: flex; justify-content: space-between; align-items: center; margin: 4px 4px 14px; }
.m-title { margin: 0; }
.m-actions { display: flex; gap: 8px; }
.m-card { border-radius: 12px; }
.plan-title { font-weight: 700; margin-bottom: 8px; }
.item-list { display: flex; flex-direction: column; gap: 10px; }
.train-item { display: flex; justify-content: space-between; align-items: center;
  padding: 12px; background: #f7f8fa; border-radius: 10px; }
.exercise { font-weight: 600; font-size: 16px; }
.detail { color: #999; font-size: 12px; margin-top: 2px; }
.row-actions { display: flex; gap: 8px; flex-shrink: 0; }
.day-actions { margin-top: 10px; }
.checkin-form { display: flex; flex-direction: column; gap: 14px; }
.checkin-exercise { font-weight: 700; font-size: 18px; text-align: center; }
.field { display: flex; justify-content: space-between; align-items: center; }
.field > span { color: #666; }
</style>
