<template>
  <el-dialog :model-value="visible" :title="pickerTitle"
             width="94%" top="3vh" @update:model-value="$emit('close')" @open="onOpen">
    <div class="picker-body">
      <!-- 左侧：部位导航 -->
      <aside class="group-nav">
        <div v-for="g in groups" :key="g" class="group-item"
             :class="{ active: currentGroup === g }" @click="currentGroup = g">
          {{ g }}
        </div>
      </aside>

      <!-- 中间：搜索 + 器械筛选 + 动作卡片 -->
      <div class="picker-main">
        <el-input v-model="keyword" placeholder="输入动作名字搜索" clearable size="large">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <div class="equip-chips">
          <span v-for="e in equipmentOptions" :key="e" class="chip"
                :class="{ active: currentEquip === e }" @click="currentEquip = e">{{ e }}</span>
        </div>

        <div class="exercise-grid" v-loading="loading">
          <div v-for="ex in filtered" :key="ex.id" class="ex-card">
            <div class="ex-head">
              <img v-if="ex.imageUrl" :src="ex.imageUrl" class="ex-img" :alt="ex.name" />
              <span v-else class="ex-icon" :style="{ background: groupColor[ex.muscleGroup] || '#888' }">
                {{ ex.muscleGroup }}
              </span>
              <div class="ex-name">
                <div class="ex-title">{{ ex.name }}</div>
                <div class="ex-sub">{{ ex.muscleGroup }} · {{ ex.equipment || '自重' }}</div>
              </div>
              <button class="ex-add" @click="openConfig(ex)">＋</button>
            </div>
            <div v-if="ex.tips" class="ex-tips" @click="toggleTips(ex.id)">
              {{ expandedTips.has(ex.id) ? ex.tips : '查看动作要点' }}
            </div>
          </div>
          <el-empty v-if="!loading && !filtered.length" description="没有匹配的动作" :image-size="60" />
        </div>

        <div class="manual-add">
          <el-input v-model="customName" placeholder="库里没有？输入自定义动作名" size="small" style="width: 260px"
                    @keyup.enter="addCustom" />
          <el-button size="small" @click="addCustom">加入已选</el-button>
        </div>
      </div>

      <!-- 右侧：已选清单（可修改组次/训练日，可移除） -->
      <aside class="basket-panel">
        <div class="basket-title">
          {{ mode === 'new-plan' ? `已选 ${basket.length} 个` : mode === 'edit-archive' ? `存档共 ${basket.length} 个` : `${dayNames[props.dayOfWeek ?? 1]} 已安排（${basket.length}）` }}
        </div>
        <div class="basket-archive-actions">
          <el-button v-if="mode !== 'edit-archive'" size="small" plain @click="saveBasketAsArchive">
            💾 存为存档
          </el-button>
          <el-button v-if="mode !== 'edit-archive'" size="small" plain @click="openArchivePicker">
            📁 使用存档
          </el-button>
        </div>
        <div class="basket-list">
          <div v-for="(b, i) in sortedBasket" :key="b.itemId ?? 'new-' + i" class="basket-item">
            <div class="basket-head">
              <span class="basket-name">
                {{ b.name }}
                <el-tag v-if="b.isNew" size="small" type="success" effect="plain" style="margin-left: 4px">新</el-tag>
              </span>
              <el-button size="small" text type="danger" @click="removeFromBasket(b)">移除</el-button>
            </div>
            <div class="basket-fields">
              <span>组数</span>
              <el-input-number v-model="b.sets" :min="1" :max="30" size="small" style="width: 100px" />
              <span>{{ b.muscleGroup === '有氧' ? '分钟' : '次数' }}</span>
              <el-input-number v-model="b.reps" :min="1" :max="100" size="small" style="width: 100px" />
            </div>
            <div v-if="b.muscleGroup !== '有氧'" class="basket-fields">
              <span>重量(kg)</span>
              <el-input-number v-model="b.weightKg" :min="0" :max="500" :precision="1" size="small"
                               style="width: 110px" placeholder="自重" />
            </div>
            <div v-if="mode === 'edit-archive'" class="basket-fields">
              <span>训练日</span>
              <el-select v-model="b.dayOfWeek" size="small" style="width: 100px">
                <el-option v-for="d in days" :key="d.value" :value="d.value" :label="d.label" />
              </el-select>
            </div>
          </div>
          <el-empty v-if="!basket.length" description="点左侧 ＋ 选择动作" :image-size="50" />
        </div>
        <el-button type="primary" size="large" style="width: 100%" :disabled="!basket.length" @click="finish">
          {{ mode === 'new-plan' ? '完成，去创建计划' : '保存修改' }}
        </el-button>
      </aside>
    </div>

    <!-- 使用存档：载入已保存的模板内容 -->
  <el-dialog v-model="archiveVisible" title="使用存档" width="360px" append-to-body>
    <div v-loading="archiveLoading" class="archive-options">
      <el-radio-group v-if="archives.length" v-model="archiveSelected" class="archive-radio-group">
        <el-radio v-for="a in archives" :key="a.id" :value="a.id" class="archive-radio">
          <span class="archive-radio-name">{{ a.name }}</span>
          <span class="archive-radio-sub">{{ a.goal || '未设目标' }}</span>
        </el-radio>
      </el-radio-group>
      <el-empty v-if="!archiveLoading && !archives.length" :image-size="60"
                description="还没有存档，可先在训练计划页把计划存为模板" />
    </div>
    <template #footer>
      <el-button @click="archiveVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!archiveSelected" @click="applySelectedArchive">载入</el-button>
    </template>
  </el-dialog>

  <!-- 点+后的组次填写弹窗 -->
    <el-dialog v-model="configVisible" :title="`添加「${configEx?.name}」`" width="360px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="组数">
          <el-input-number v-model="configForm.sets" :min="1" :max="30" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="configEx?.muscleGroup === '有氧' ? '时长(分钟)' : '每组次数'">
          <el-input-number v-model="configForm.reps" :min="1" :max="100" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="configEx && configEx.muscleGroup !== '有氧'" label="重量(kg)">
          <el-input-number v-model="configForm.weightKg" :min="0" :max="500" :precision="1"
                           size="large" style="width: 100%" placeholder="自重可不填" />
        </el-form-item>
        <el-form-item v-if="configEx && mode === 'edit-archive'" label="训练日">
          <el-select v-model="configForm.dayOfWeek" size="large" style="width: 100%">
            <el-option v-for="d in days" :key="d.value" :value="d.value" :label="d.label" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmConfig">加入已选</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { archiveApi, exerciseApi, workoutApi, type ArchiveItemPayload, type Exercise, type PlanArchive, type PlanArchiveItem } from '../api'

export interface BasketItem {
  /** 已有动作对应 workout_item 主键；新增动作为空 */
  itemId?: number
  name: string
  muscleGroup: string
  sets: number
  reps: number
  /** 建议重量(kg)，空=自重 */
  weightKg?: number | null
  dayOfWeek?: number
  isNew: boolean
}

export interface ApplyDiff {
  added: BasketItem[]
  updated: BasketItem[]
  removed: { itemId: number; name: string }[]
}

const props = defineProps<{
  visible: boolean
  /** new-plan: 新建计划篮子；edit-day: 编辑/加入当前计划某一天；edit-archive: 编辑存档内容 */
  mode: 'new-plan' | 'edit-day' | 'edit-archive'
  planId?: number
  /** 按天编辑时锁定训练日（1-7） */
  dayOfWeek?: number
  /** edit-archive 模式：目标存档ID */
  archiveId?: number
}>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'finish', basket: BasketItem[]): void
  (e: 'apply', diff: ApplyDiff): void
  (e: 'save-items', items: ArchiveItemPayload[]): void
}>()

const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = [1, 2, 3, 4, 5, 6, 7].map((v) => ({ value: v, label: dayNames[v] }))
const groups = ['全部', '胸', '背', '腿', '肩', '手臂', '核心', '有氧']
const equipmentOptions = ['全部', '杠铃', '哑铃', '器械', '自重', '有氧']
const groupColor: Record<string, string> = {
  胸: '#e74c3c', 背: '#3498db', 腿: '#9b59b6', 肩: '#f39c12',
  手臂: '#e67e22', 核心: '#1abc9c', 有氧: '#2ecc71',
}

const exercises = ref<Exercise[]>([])
const loading = ref(false)
const keyword = ref('')
const currentGroup = ref('全部')
const currentEquip = ref('全部')
const basket = ref<BasketItem[]>([])
/** 载入时的原始动作快照，用于计算增删改差异 */
const original = ref<any[]>([])
const expandedTips = ref<Set<number>>(new Set())
const customName = ref('')

const configVisible = ref(false)
const configEx = ref<Exercise>()
const configForm = ref({ sets: 4, reps: 10, dayOfWeek: 1, weightKg: undefined as number | undefined })

const pickerTitle = computed(() =>
  props.mode === 'new-plan' ? '选择动作（创建计划）'
    : props.mode === 'edit-archive' ? '编辑存档内容'
    : `${dayNames[props.dayOfWeek ?? 1]} · 编辑训练内容`)

const filtered = computed(() =>
  exercises.value.filter((e) =>
    (currentGroup.value === '全部' || e.muscleGroup === currentGroup.value) &&
    (currentEquip.value === '全部' || e.equipment === currentEquip.value) &&
    (!keyword.value || e.name.includes(keyword.value))))

/** 右栏展示：编辑存档模式按训练日排序，其余保持挑选顺序 */
const sortedBasket = computed(() =>
  props.mode === 'edit-archive'
    ? [...basket.value].sort((a, b) => (a.dayOfWeek ?? 9) - (b.dayOfWeek ?? 9))
    : basket.value)

async function onOpen() {
  basket.value = []
  expandedTips.value = new Set()
  configForm.value.dayOfWeek = props.dayOfWeek ?? 1
  if (!exercises.value.length) {
    loading.value = true
    try {
      const res: any = await exerciseApi.list()
      exercises.value = res.data
    } finally {
      loading.value = false
    }
  }
  // 编辑模式：只载入"这一天"的已有动作，可改组次/重量/移除
  if (props.mode === 'edit-day' && props.planId) {
    loading.value = true
    try {
      const res: any = await workoutApi.items(props.planId)
      original.value = res.data.filter((it: any) => it.dayOfWeek === props.dayOfWeek)
      basket.value = original.value.map((it: any) => ({
        itemId: it.id,
        name: it.exerciseName,
        muscleGroup: it.muscleGroup,
        sets: it.sets,
        reps: it.reps,
        weightKg: it.weightKg ?? null,
        dayOfWeek: it.dayOfWeek,
        isNew: false,
      }))
    } finally {
      loading.value = false
    }
  }
  // 编辑存档模式：载入存档全部动作（跨天），可改组次/训练日/移除
  if (props.mode === 'edit-archive' && props.archiveId) {
    loading.value = true
    try {
      const res: any = await archiveApi.items(props.archiveId)
      original.value = res.data
      basket.value = res.data.map((it: any) => ({
        itemId: it.id,
        name: it.exerciseName,
        muscleGroup: it.muscleGroup,
        sets: it.sets,
        reps: it.reps,
        weightKg: it.weightKg ?? null,
        dayOfWeek: it.dayOfWeek,
        isNew: false,
      }))
    } finally {
      loading.value = false
    }
  }
}

function openConfig(ex: Exercise) {
  configEx.value = ex
  configForm.value = { sets: 4, reps: 10, dayOfWeek: props.dayOfWeek ?? 1, weightKg: undefined }
  configVisible.value = true
}

function confirmConfig() {
  const ex = configEx.value!
  basket.value = [...basket.value, {
    name: ex.name,
    muscleGroup: ex.muscleGroup,
    sets: configForm.value.sets,
    reps: configForm.value.reps,
    weightKg: configForm.value.weightKg == null ? null : configForm.value.weightKg,
    dayOfWeek: props.mode === 'new-plan' ? undefined : configForm.value.dayOfWeek,
    isNew: true,
  }]
  configVisible.value = false
}

function addCustom() {
  const name = customName.value.trim()
  if (!name) return
  if (basket.value.some((b) => b.name === name)) {
    ElMessage.warning('该动作已在已选列表中')
    return
  }
  const group = currentGroup.value === '全部' ? '核心' : currentGroup.value
  basket.value = [...basket.value, {
    name, muscleGroup: group, sets: 4, reps: 10, weightKg: null,
    dayOfWeek: props.mode === 'new-plan' ? undefined : (props.dayOfWeek ?? 1),
    isNew: true,
  }]
  customName.value = ''
}

function toggleTips(id: number) {
  const next = new Set(expandedTips.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedTips.value = next
}

function removeFromBasket(b: BasketItem) {
  basket.value = basket.value.filter((x) => x !== b)
}

// ---- 使用存档：载入已保存的模板内容 ----

const archiveVisible = ref(false)
const archiveLoading = ref(false)
const archives = ref<PlanArchive[]>([])
const archiveSelected = ref<number>()

async function openArchivePicker() {
  archiveVisible.value = true
  archiveSelected.value = undefined
  archiveLoading.value = true
  try {
    const res: any = await archiveApi.list()
    archives.value = res.data ?? []
  } finally {
    archiveLoading.value = false
  }
}

const toBasketItem = (it: PlanArchiveItem): BasketItem => ({
  name: it.exerciseName,
  muscleGroup: it.muscleGroup,
  sets: it.sets,
  reps: it.reps,
  weightKg: it.weightKg ?? null,
  dayOfWeek: it.dayOfWeek,
  isNew: true,
})

/** 应用存档：编辑当天→把存档全部动作归入当天（忽略原训练日，同名去重）；新建计划→载入整周并保留原训练日 */
async function applySelectedArchive() {
  const res: any = await archiveApi.items(archiveSelected.value!)
  const items: PlanArchiveItem[] = res.data ?? []
  if (props.mode === 'edit-day') {
    const seen = new Set<string>()
    basket.value = items
      .filter((it) => !seen.has(it.exerciseName) && (seen.add(it.exerciseName), true))
      .map((it) => ({ ...toBasketItem(it), dayOfWeek: props.dayOfWeek ?? 1 }))
  } else {
    basket.value = [...items]
      .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.id - b.id)
      .map(toBasketItem)
  }
  archiveVisible.value = false
  ElMessage.success('已载入存档内容，确认后保存')
}

/** 把当前已选内容直接存为新的存档（编辑当天→全部归当天；新建计划→保留已带训练日） */
async function saveBasketAsArchive() {
  if (!basket.value.length) {
    ElMessage.warning('请先选择动作')
    return
  }
  const { value } = await ElMessageBox.prompt('把当前已选内容存为新的存档模板', '存为存档', {
    inputPlaceholder: '存档名称，如：上肢力量日',
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  })
  const name = (value ?? '').trim()
  if (!name) {
    ElMessage.warning('请填写存档名称')
    return
  }
  const items: ArchiveItemPayload[] = basket.value.map((b) => ({
    dayOfWeek: props.mode === 'edit-day' ? (props.dayOfWeek ?? 1) : (b.dayOfWeek ?? 1),
    exerciseName: b.name,
    muscleGroup: b.muscleGroup,
    sets: b.sets,
    reps: b.reps,
    weightKg: b.weightKg ?? null,
  }))
  await archiveApi.createFromItems({ name, items })
  ElMessage.success('已存为存档，可在「我的 → 计划存档」查看和编辑')
}

function finish() {
  if (props.mode === 'edit-archive') {
    // 编辑存档：整体提交当前清单（增删改合并为一次替换）
    emit('save-items', basket.value.map((b) => ({
      dayOfWeek: b.dayOfWeek ?? 1,
      exerciseName: b.name,
      muscleGroup: b.muscleGroup,
      sets: b.sets,
      reps: b.reps,
      weightKg: b.weightKg ?? null,
    })))
    return
  }
  if (props.mode === 'new-plan') {
    emit('finish', [...basket.value])
    return
  }
  // 编辑模式：与载入快照对比，计算 新增/修改/删除 差异一次性提交
  const added = basket.value.filter((b) => b.isNew)
  const updated = basket.value.filter((b) => {
    if (b.isNew) return false
    const o = original.value.find((x: any) => x.id === b.itemId)
    return o && (o.sets !== b.sets || o.reps !== b.reps || o.dayOfWeek !== b.dayOfWeek || (o.weightKg ?? null) !== (b.weightKg ?? null))
  })
  const removed = original.value
    .filter((o: any) => !basket.value.some((b) => b.itemId === o.id))
    .map((o: any) => ({ itemId: o.id, name: o.exerciseName }))
  emit('apply', { added, updated, removed })
}
</script>

<style scoped>
.picker-body { display: flex; gap: 16px; min-height: 58vh; }
.group-nav { width: 100px; flex-shrink: 0; border-right: 1px solid #f0f0f0; }
.group-item {
  padding: 12px 14px; cursor: pointer; color: #666; font-size: 15px;
  border-radius: 8px; margin-right: 10px;
}
.group-item.active { background: #ecf5ff; color: #409eff; font-weight: 600; }
.picker-main { flex: 1; display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.equip-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  padding: 5px 14px; border-radius: 16px; background: #f4f4f5; color: #666;
  font-size: 13px; cursor: pointer;
}
.chip.active { background: #409eff; color: #fff; }
.exercise-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px; overflow-y: auto; max-height: 46vh; padding: 2px;
}
.ex-card { border: 1px solid #eee; border-radius: 10px; padding: 12px; }
.ex-head { display: flex; align-items: center; gap: 10px; }
.ex-icon {
  width: 42px; height: 42px; border-radius: 50%; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 13px;
  flex-shrink: 0;
}
.ex-img {
  width: 56px; height: 56px; border-radius: 10px; object-fit: cover;
  border: 1px solid #eee; flex-shrink: 0; background: #fff;
}
.ex-name { flex: 1; min-width: 0; }
.ex-title { font-weight: 600; font-size: 15px; }
.ex-sub { color: #999; font-size: 12px; margin-top: 2px; }
.ex-add {
  width: 34px; height: 34px; border-radius: 50%; border: none;
  background: #409eff; color: #fff; font-size: 18px; cursor: pointer; flex-shrink: 0;
}
.ex-tips {
  margin-top: 10px; padding: 8px 10px; background: #f7f8fa; border-radius: 8px;
  color: #888; font-size: 12px; cursor: pointer; line-height: 1.6;
}
.manual-add { display: flex; gap: 10px; align-items: center; }
.basket-archive-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.archive-options { min-height: 80px; }
.archive-radio-group { display: flex; flex-direction: column; gap: 4px; }
.archive-radio { margin-right: 0; height: auto; padding: 6px 0; }
.archive-radio-name { font-weight: 600; margin-right: 8px; }
.archive-radio-sub { color: #999; font-size: 12px; }

/* 右侧已选面板 */
.basket-panel {
  width: 280px; flex-shrink: 0; border-left: 1px solid #f0f0f0;
  padding-left: 14px; display: flex; flex-direction: column; gap: 10px;
}
.basket-title { font-weight: 700; }
.basket-list {
  flex: 1; overflow-y: auto; max-height: 50vh;
  display: flex; flex-direction: column; gap: 10px; padding-right: 4px;
}
.basket-item { border: 1px solid #eee; border-radius: 10px; padding: 10px; }
.basket-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.basket-name { font-weight: 600; font-size: 14px; }
.basket-fields { display: flex; align-items: center; gap: 6px; margin-top: 6px; color: #888; font-size: 12px; }

/* 移动端适配（手机微信/窄屏）：三栏纵向堆叠，导航横向滑动 */
@media (max-width: 768px) {
  :deep(.el-dialog__body) { max-height: 72vh; overflow-y: auto; }
  .picker-body { flex-direction: column; gap: 10px; min-height: auto; }
  .group-nav {
    width: 100%; display: flex; overflow-x: auto; flex-shrink: 0;
    border-right: none; border-bottom: 1px solid #f0f0f0; padding-bottom: 6px;
  }
  .group-item { white-space: nowrap; margin-right: 6px; padding: 8px 12px; font-size: 14px; flex-shrink: 0; }
  .exercise-grid { grid-template-columns: 1fr; max-height: 38vh; }
  .basket-panel {
    width: 100%; border-left: none; border-top: 1px solid #f0f0f0;
    padding-left: 0; padding-top: 10px;
  }
  .basket-list { max-height: 24vh; }
  .manual-add { flex-wrap: wrap; }
}
</style>
