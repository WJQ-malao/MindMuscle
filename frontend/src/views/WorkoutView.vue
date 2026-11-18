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
          <!-- 有计划时只显示编辑；无计划时才显示新建（新建会使旧计划归档，避免误触） -->
          <template v-if="plans.length">
            <el-button :type="editMode ? 'warning' : 'primary'" plain @click="editMode = !editMode">
              {{ editMode ? '完成编辑' : '编辑计划' }}
            </el-button>
          </template>
          <el-button v-else type="primary" @click="openNewPlanPicker">创建计划</el-button>
        </div>
      </div>

      <el-tabs v-if="items.length || editMode" v-model="day">
        <el-tab-pane v-for="d in days" :key="d.value" :label="d.label" :name="String(d.value)">
          <el-table :data="itemsByDay(d.value)" stripe>
            <el-table-column prop="exerciseName" label="动作" min-width="160" />
            <el-table-column prop="muscleGroup" label="肌群" width="90" />
            <el-table-column label="安排" width="150">
              <template #default="{ row }">{{ row.sets }} 组 x {{ row.reps }}{{ row.muscleGroup === '有氧' ? ' 分钟' : ' 次' }}</template>
            </el-table-column>
            <el-table-column label="操作" :width="editMode ? 180 : 110">
              <template #default="{ row }">
                <template v-if="editMode">
                  <el-button size="small" @click="openItemEdit(row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="removeItem(row)">删除</el-button>
                </template>
                <el-button v-else type="primary" size="small" @click="openCheckin(row)">打卡</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="editMode" class="day-actions">
            <el-button type="primary" plain round size="small" @click="openAddPicker">＋ 添加动作</el-button>
            <span v-if="!itemsByDay(d.value).length" class="rest-tip">这一天还没有安排动作</span>
          </div>
          <el-empty v-if="!editMode && !itemsByDay(d.value).length" :image-size="60" description="这天是休息日" />
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

    <!-- 添加/编辑动作 -->
    <el-dialog v-model="itemVisible" :title="editingItem ? '编辑动作' : '添加动作'" width="440px">
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
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 动作选择器：新建计划篮子 / 加入当前计划 双模式 -->
    <ExercisePicker :visible="pickerVisible" :mode="pickerMode" :default-day="Number(day)"
                    @close="pickerVisible = false" @finish="onPickerFinish" />

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { exerciseApi, workoutApi, type Exercise, type WorkoutItem, type WorkoutPlan } from '../api'
import ExercisePicker, { type BasketItem } from './ExercisePicker.vue'

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const muscleOptions = ['胸', '背', '腿', '肩', '手臂', '核心', '有氧']
const day = ref(String(new Date().getDay() || 7))

const plans = ref<WorkoutPlan[]>([])
const items = ref<WorkoutItem[]>([])
const exercises = ref<Exercise[]>([])
const loading = ref(false)
const currentPlanId = ref<number>()
const editMode = ref(false)

const itemVisible = ref(false)
const itemSaving = ref(false)
const editingItem = ref<WorkoutItem>()
const itemForm = reactive({ exerciseName: '', muscleGroup: '胸', dayOfWeek: 1, sets: 4, reps: 10 })

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
/** 当前肌群下的动作库选项 */
const exercisesByGroup = computed(() =>
  exercises.value.filter((e) => e.muscleGroup === itemForm.muscleGroup))

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

// ---- 动作添加/编辑 ----

function onGroupChange() {
  itemForm.exerciseName = ''
}

function openItemEdit(row: WorkoutItem) {
  editingItem.value = row
  itemForm.exerciseName = row.exerciseName
  itemForm.muscleGroup = row.muscleGroup
  itemForm.dayOfWeek = row.dayOfWeek
  itemForm.sets = row.sets
  itemForm.reps = row.reps
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
      await workoutApi.addItem(currentPlanId.value!, { ...itemForm })
      ElMessage.success('已添加')
    }
    itemVisible.value = false
    loadItems()
  } finally {
    itemSaving.value = false
  }
}

async function removeItem(row: WorkoutItem) {
  await ElMessageBox.confirm(`确定删除「${row.exerciseName}」吗？`, '提示', { type: 'warning' })
  await workoutApi.removeItem(row.id)
  ElMessage.success('已删除')
  loadItems()
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
  if (pickerMode.value === 'new-plan') {
    basket.value = selected
    pickerVisible.value = false
    newPlanForm.title = ''
    newPlanVisible.value = true
    return
  }
  // 加入当前计划：逐个入库
  if (!currentPlanId.value) return
  let ok = 0
  for (const b of selected) {
    try {
      await workoutApi.addItem(currentPlanId.value, {
        exerciseName: b.name,
        muscleGroup: b.muscleGroup,
        dayOfWeek: b.dayOfWeek ?? Number(day.value),
        sets: b.sets,
        reps: b.reps,
      })
      ok++
    } catch { /* 拦截器已提示 */ }
  }
  ElMessage.success(`已添加 ${ok} 个动作`)
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

onMounted(async () => {
  load()
  const res: any = await exerciseApi.list()
  exercises.value = res.data
})
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
