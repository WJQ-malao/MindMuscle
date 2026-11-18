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
          <!-- 按天维度: 当天有内容→编辑; 当天为空→创建(往现有计划补这一天); 无计划→创建计划容器 -->
          <el-button v-if="!plans.length" type="primary" @click="openNewPlanPicker">创建计划</el-button>
          <el-button v-else-if="currentDayItems.length" type="primary" plain @click="openDayPicker">编辑计划</el-button>
          <el-button v-else type="primary" @click="openDayPicker">创建计划</el-button>
        </div>
      </div>

      <el-tabs v-if="items.length" v-model="day">
        <el-tab-pane v-for="d in days" :key="d.value" :name="String(d.value)">
          <template #label>
            <span class="tab-label">
              {{ d.label }}
              <span v-if="!itemsByDay(d.value).length" class="day-dot" title="未安排"></span>
            </span>
          </template>
          <el-table :data="itemsByDay(d.value)" stripe>
            <el-table-column prop="exerciseName" label="动作" min-width="160" />
            <el-table-column prop="muscleGroup" label="肌群" width="90" />
            <el-table-column label="安排" width="190">
              <template #default="{ row }">
                {{ row.weightKg ? row.weightKg + 'kg · ' : '' }}{{ row.sets }} 组 x {{ row.reps }}{{ row.muscleGroup === '有氧' ? ' 分钟' : ' 次' }}
              </template>
            </el-table-column>
            <el-table-column label="打卡" width="230">
              <template #default="{ row }">
                <!-- 力量动作：按组打卡，组号格子逐个点亮 -->
                <template v-if="row.muscleGroup !== '有氧'">
                  <span v-for="i in row.sets" :key="i"
                        class="set-chip" :class="{ done: isSetDone(row.id, i) }"
                        :title="isSetDone(row.id, i) ? `第${i}组已完成，点击撤销` : `打卡第${i}组`"
                        @click="onSetChip(row, i)">
                    {{ isSetDone(row.id, i) ? '✓' : i }}
                  </span>
                  <span class="set-progress">{{ doneCount(row.id) }}/{{ row.sets }}</span>
                </template>
                <!-- 有氧动作：整项一次打卡 -->
                <template v-else>
                  <el-button v-if="isSetDone(row.id, 1)" type="danger" plain size="small"
                             @click="undoSet(row, 1)">已打卡</el-button>
                  <el-button v-else type="success" size="small" @click="openCheckin(row, 1)">打卡</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!itemsByDay(d.value).length" :image-size="60" description="这天是休息日" />
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else-if="!loading && !plans.length" description="还没有训练计划">
        <el-button type="primary" @click="openNewPlanPicker">创建计划</el-button>
        <el-button @click="$router.push('/chat')">让 AI 帮你生成</el-button>
      </el-empty>
      <el-empty v-else-if="!loading" description="这个计划还没有动作，编辑添加或重新创建">
        <el-button type="primary" plain @click="openDayPicker">编辑计划</el-button>
        <el-button type="primary" @click="openNewPlanPicker">创建计划</el-button>
      </el-empty>
    </el-card>

    <!-- 动作选择器：创建计划(周维度) / 按天编辑 双模式 -->
    <ExercisePicker :visible="pickerVisible" :mode="pickerMode" :plan-id="currentPlanId"
                    :day-of-week="Number(day)"
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

    <!-- 打卡（某一组） -->
    <el-dialog v-model="checkinVisible" title="组打卡" width="420px">
      <el-form label-width="90px">
        <el-form-item label="动作">
          <span>{{ currentItem?.exerciseName }} · 第 {{ checkinForm.setNo }} 组</span>
        </el-form-item>
        <el-form-item label="完成次数">
          <el-input-number v-model="checkinForm.repsDone" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="重量 (kg)">
          <el-input-number v-model="checkinForm.weightUsed" :min="0" :max="500" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="success" :loading="checkinSaving" @click="doCheckin">确认打卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const pickerMode = ref<'new-plan' | 'edit-day'>('new-plan')
const basket = ref<BasketItem[]>([])
const newPlanForm = reactive({ title: '', goal: '增肌', daysPerWeek: 4 })

const checkinVisible = ref(false)
const checkinSaving = ref(false)
const currentItem = ref<WorkoutItem>()
const checkinForm = reactive({ setNo: 1, repsDone: 10, weightUsed: 0 })

const currentPlan = computed(() => plans.value.find((p) => p.id === currentPlanId.value))
const itemsByDay = (d: number) => items.value.filter((i) => i.dayOfWeek === d)
/** 当前选中那一天的训练项（编辑/创建按天维度） */
const currentDayItems = computed(() => itemsByDay(Number(day.value)))

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
  await loadCheckedToday()
}

/** 今天已打卡的"动作ID→已打组号列表"（按组打卡状态） */
const checkedToday = ref<Record<number, number[]>>({})

function isSetDone(itemId: number, setNo: number) {
  return (checkedToday.value[itemId] ?? []).includes(setNo)
}

function doneCount(itemId: number) {
  return (checkedToday.value[itemId] ?? []).length
}

async function loadCheckedToday() {
  const res: any = await workoutApi.todayChecked()
  checkedToday.value = res.data ?? {}
}

/** 组格子点击：已完成→确认撤销；未完成→打卡弹窗 */
function onSetChip(row: WorkoutItem, setNo: number) {
  if (isSetDone(row.id, setNo)) {
    undoSet(row, setNo)
  } else {
    openCheckin(row, setNo)
  }
}

// ---- 动作选择器：两种模式统一收口 ----

function openNewPlanPicker() {
  pickerMode.value = 'new-plan'
  pickerVisible.value = true
}

/** 按天编辑/创建：编辑的是当前选中那一天的内容 */
function openDayPicker() {
  pickerMode.value = 'edit-day'
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
        dayOfWeek: b.dayOfWeek ?? 1, sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
      })
      updateOk++
    } catch { /* 拦截器已提示 */ }
  }
  for (const b of diff.added) {
    try {
      await workoutApi.addItem(currentPlanId.value, {
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? Number(day.value), sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
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
        exerciseName: b.name, muscleGroup: b.muscleGroup,
        sets: b.sets, reps: b.reps, weightKg: b.weightKg ?? null,
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

// ---- 打卡（按组） ----

function openCheckin(item: WorkoutItem, setNo: number) {
  currentItem.value = item
  checkinForm.setNo = setNo
  checkinForm.repsDone = item.reps
  checkinForm.weightUsed = item.weightKg ?? 0
  checkinVisible.value = true
}

async function doCheckin() {
  checkinSaving.value = true
  try {
    await workoutApi.checkin({ itemId: currentItem.value!.id, ...checkinForm })
    ElMessage.success(`第 ${checkinForm.setNo} 组打卡成功`)
    checkinVisible.value = false
    await loadCheckedToday()
  } finally {
    checkinSaving.value = false
  }
}

/** 撤销某一组的打卡 */
async function undoSet(row: WorkoutItem, setNo: number) {
  await ElMessageBox.confirm(
    `撤销「${row.exerciseName}」第 ${setNo} 组的打卡记录？`,
    '撤销组打卡',
    { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' },
  )
  await workoutApi.undoSetCheckin(row.id, setNo)
  ElMessage.success('已撤销该组')
  await loadCheckedToday()
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
.tab-label { display: inline-flex; align-items: center; gap: 5px; }
.day-badge {
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px;
  background: #409eff; color: #fff; font-size: 11px; line-height: 18px;
}
.day-dot { width: 6px; height: 6px; border-radius: 50%; background: #dcdfe6; display: inline-block; }
/* 按组打卡格子 */
.set-chip {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; margin-right: 6px; border-radius: 6px;
  border: 1px solid #dcdfe6; color: #909399; font-size: 12px; cursor: pointer;
  user-select: none;
}
.set-chip:hover { border-color: #67c23a; color: #67c23a; }
.set-chip.done { background: #67c23a; border-color: #67c23a; color: #fff; }
.set-chip.done:hover { background: #f56c6c; border-color: #f56c6c; }
.set-progress { color: #c0c4cc; font-size: 12px; margin-left: 4px; }
</style>
