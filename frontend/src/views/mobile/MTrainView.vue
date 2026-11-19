<template>
  <div>
    <div class="m-header">
      <h3 class="m-title">🏋️ 训练计划</h3>
      <div class="m-actions">
        <el-date-picker v-model="viewDate" type="date" :clearable="false" :editable="false"
                        size="small" style="width: 130px" placeholder="选择日期" @change="onViewDateChange" />
        <!-- 有计划：头部按钮直接打开动作选择器编辑当天（与 PC 端一致，无编辑模式） -->
        <el-button v-if="!activePlan" size="small" @click="openNewPlanPicker">创建计划</el-button>
        <el-button v-else size="small" plain type="primary" @click="openDayPicker">
          {{ currentDayHasItems ? '编辑计划' : '添加动作' }}
        </el-button>
      </div>
    </div>

    <el-card v-if="activePlan" shadow="never" class="m-card">
      <div class="plan-title">{{ activePlan.title }}</div>
      <el-tabs v-model="day" @tab-change="onTabChange">
        <el-tab-pane v-for="(d, idx) in days" :key="d.value" :name="String(d.value)">
          <template #label>
            <span :class="{ 'tab-today': isTabToday(idx) }">{{ d.label }} {{ fmtMD(weekDates[idx]) }}</span>
          </template>
          <div v-if="itemsByDay(d.value).length" class="item-list">
            <div v-for="it in itemsByDay(d.value)" :key="it.id" class="train-item">
              <div>
                <div class="exercise">{{ it.exerciseName }}</div>
                <div class="detail">{{ it.muscleGroup }} · {{ it.weightKg ? it.weightKg + 'kg · ' : '' }}{{ it.sets }}组x{{ it.reps }}{{ it.muscleGroup === '有氧' ? '分钟' : '次' }}</div>
              </div>
              <!-- 所有动作统一按组打卡格子；查看历史日期时只读 -->
              <div class="set-chips">
                <span v-for="i in it.sets" :key="i"
                      class="set-chip" :class="{ done: isSetDone(it.id, i), readonly: !isViewingToday }"
                      @click="isViewingToday && onSetChip(it, i)">
                  {{ isSetDone(it.id, i) ? '✓' : i }}
                </span>
              </div>
            </div>
          </div>
          <el-empty v-else :image-size="50" description="这天休息日" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-card v-else shadow="never" class="m-card">
      <el-empty description="还没有训练计划">
        <el-button type="primary" round @click="openNewPlanPicker">创建计划</el-button>
        <el-button type="primary" round plain @click="$router.push('/m/chat')">让 AI 帮你生成</el-button>
      </el-empty>
    </el-card>

    <!-- 动作选择器：新建计划 / 按天编辑 双模式（与 PC 端一致，头部按钮直接打开） -->
    <ExercisePicker :visible="pickerVisible" :mode="pickerMode" :plan-id="activePlan?.id"
                    :day-of-week="Number(day)"
                    @close="pickerVisible = false" @finish="onBasketFinish" @apply="onPickerApply" />

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

    <el-dialog v-model="checkinVisible" title="组打卡" width="90%" style="max-width: 400px">
      <div class="checkin-form">
        <div class="checkin-exercise">{{ currentItem?.exerciseName }} · 第 {{ form.setNo }} 组</div>
        <div class="field">
          <span>{{ currentItem?.muscleGroup === '有氧' ? '完成时长(分钟)' : '完成次数' }}</span>
          <el-input-number v-model="form.repsDone" :min="1" :max="100" size="large" />
        </div>
        <div v-if="currentItem?.muscleGroup !== '有氧'" class="field">
          <span>重量(kg)</span>
          <el-input-number v-model="form.weightUsed" :min="0" :max="500" :precision="1" size="large" />
        </div>
      </div>
      <template #footer>
        <el-button size="large" style="width: 100%" type="success" :loading="saving" @click="doCheckin">
          确认打卡
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workoutApi, type WorkoutItem, type WorkoutPlan } from '../../api'
import ExercisePicker, { type ApplyDiff, type BasketItem } from '../ExercisePicker.vue'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const day = ref(String(new Date().getDay() || 7))

/** 查看日期：默认今天；日期选择器切换后，页签显示该日期所在周的 周几+月/日 */
const viewDate = ref(new Date())
const pad = (n: number) => String(n).padStart(2, '0')
const fmtISO = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const fmtMD = (d: Date) => `${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
const todayISO = fmtISO(new Date())
const weekDates = computed(() => {
  const base = new Date(viewDate.value)
  const mon = new Date(base)
  mon.setDate(base.getDate() - ((base.getDay() + 6) % 7))
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(mon)
    d.setDate(mon.getDate() + i)
    return d
  })
})
const currentTabDate = computed(() => weekDates.value[Number(day.value) - 1])
/** 是否正在查看今天（只有今天可以打卡/撤销） */
const isViewingToday = computed(() => fmtISO(currentTabDate.value) === todayISO)
const isTabToday = (idx: number) => fmtISO(weekDates.value[idx]) === todayISO

function onViewDateChange() {
  day.value = String(viewDate.value.getDay() || 7)
  loadChecked()
}

/** 切换训练日：刷新该日期的打卡状态 */
function onTabChange() {
  loadChecked()
}

const activePlan = ref<WorkoutPlan>()
const items = ref<WorkoutItem[]>([])

const pickerVisible = ref(false)
const pickerMode = ref<'new-plan' | 'edit-day'>('new-plan')
const basket = ref<BasketItem[]>([])
const newPlanVisible = ref(false)
const planSaving = ref(false)
const newPlanForm = reactive({ title: '', goal: '增肌', daysPerWeek: 4 })

const checkinVisible = ref(false)
const saving = ref(false)
const currentItem = ref<WorkoutItem>()
const form = reactive({ setNo: 1, repsDone: 10, weightUsed: 0 })

const itemsByDay = (d: number) => items.value.filter((i) => i.dayOfWeek === d)
/** 当前查看那天是否有训练动作（决定头部按钮显示 编辑计划 还是 添加动作） */
const currentDayHasItems = computed(() => itemsByDay(Number(day.value)).length > 0)

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

// ---- 新建计划（Keep式：先选动作进篮子，再创建计划） ----

function openNewPlanPicker() {
  pickerMode.value = 'new-plan'
  pickerVisible.value = true
}

/** 按天添加/编辑：打开与 PC 端一致的动作选择器，编辑当前选中那一天 */
function openDayPicker() {
  if (!activePlan.value) return
  pickerMode.value = 'edit-day'
  pickerVisible.value = true
}

/** 编辑计划：按差异一次性提交 新增/修改/删除（与 PC 端同一套逻辑） */
async function onPickerApply(diff: ApplyDiff) {
  if (!activePlan.value) return
  for (const r of diff.removed) {
    try { await workoutApi.removeItem(r.itemId) } catch { /* 拦截器已提示 */ }
  }
  for (const b of diff.updated) {
    try {
      await workoutApi.updateItem(b.itemId!, {
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? 1, sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
      })
    } catch { /* 拦截器已提示 */ }
  }
  for (const b of diff.added) {
    try {
      await workoutApi.addItem(activePlan.value.id, {
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? Number(day.value), sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
      })
    } catch { /* 拦截器已提示 */ }
  }
  ElMessage.success('已保存')
  pickerVisible.value = false
  const it: any = await workoutApi.items(activePlan.value.id)
  items.value = it.data
}

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

// ---- 打卡（按组） ----

const checkedToday = ref<Record<number, number[]>>({})

function isSetDone(itemId: number, setNo: number) {
  return (checkedToday.value[itemId] ?? []).includes(setNo)
}

async function loadChecked() {
  const res: any = await workoutApi.todayChecked(fmtISO(currentTabDate.value))
  checkedToday.value = res.data ?? {}
}

function onSetChip(item: WorkoutItem, setNo: number) {
  if (isSetDone(item.id, setNo)) {
    undoSet(item, setNo)
  } else {
    openCheckin(item, setNo)
  }
}

function openCheckin(item: WorkoutItem, setNo: number) {
  currentItem.value = item
  form.setNo = setNo
  form.repsDone = item.reps
  form.weightUsed = item.weightKg ?? 0
  checkinVisible.value = true
}

async function doCheckin() {
  saving.value = true
  try {
    await workoutApi.checkin({ itemId: currentItem.value!.id, ...form })
    ElMessage.success(`第 ${form.setNo} 组打卡成功`)
    checkinVisible.value = false
    await loadChecked()
  } finally {
    saving.value = false
  }
}

async function undoSet(item: WorkoutItem, setNo: number) {
  await ElMessageBox.confirm(
    `撤销「${item.exerciseName}」第 ${setNo} 组的打卡记录？`,
    '撤销组打卡',
    { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' },
  )
  await workoutApi.undoSetCheckin(item.id, setNo, fmtISO(currentTabDate.value))
  ElMessage.success('已撤销该组')
  await loadChecked()
}

onMounted(async () => {
  load()
  loadChecked()
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
  padding: 12px; background: #f7f8fa; border-radius: 10px; gap: 8px; flex-wrap: wrap; }
.exercise { font-weight: 600; font-size: 16px; }
.detail { color: #999; font-size: 12px; margin-top: 2px; }
.checkin-form { display: flex; flex-direction: column; gap: 14px; }
.checkin-exercise { font-weight: 700; font-size: 18px; text-align: center; }
.field { display: flex; justify-content: space-between; align-items: center; }
.field > span { color: #666; }
</style>

<style scoped>
.set-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.set-chip {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 8px;
  border: 1px solid #dcdfe6; color: #909399; font-size: 13px;
}
.set-chip.done { background: #67c23a; border-color: #67c23a; color: #fff; }
/* 查看历史日期：打卡格子只读 */
.set-chip.readonly { cursor: default; opacity: 0.85; }
.tab-today { color: #409eff; font-weight: 600; }
</style>
