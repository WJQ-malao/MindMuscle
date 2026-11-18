<template>
  <el-dialog :model-value="visible" :title="mode === 'new-plan' ? '选择动作（新建计划）' : '选择动作（加入当前计划）'"
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
        <div class="basket-title">已选 {{ basket.length }} 个</div>
        <div class="basket-list">
          <div v-for="(b, i) in basket" :key="i" class="basket-item">
            <div class="basket-head">
              <span class="basket-name">{{ b.name }}</span>
              <el-button size="small" text type="danger" @click="basket.splice(i, 1)">移除</el-button>
            </div>
            <div class="basket-fields">
              <span>组数</span>
              <el-input-number v-model="b.sets" :min="1" :max="30" size="small" style="width: 100px" />
              <span>{{ b.muscleGroup === '有氧' ? '分钟' : '次数' }}</span>
              <el-input-number v-model="b.reps" :min="1" :max="100" size="small" style="width: 100px" />
            </div>
            <div v-if="mode === 'add-to-plan'" class="basket-fields">
              <span>训练日</span>
              <el-select v-model="b.dayOfWeek" size="small" style="width: 100px">
                <el-option v-for="d in days" :key="d.value" :value="d.value" :label="d.label" />
              </el-select>
            </div>
          </div>
          <el-empty v-if="!basket.length" description="点左侧 ＋ 选择动作" :image-size="50" />
        </div>
        <el-button type="primary" size="large" style="width: 100%" :disabled="!basket.length" @click="finish">
          {{ mode === 'new-plan' ? '完成，去创建计划' : `加入计划（${basket.length}）` }}
        </el-button>
      </aside>
    </div>

    <!-- 点+后的组次填写弹窗 -->
    <el-dialog v-model="configVisible" :title="`添加「${configEx?.name}」`" width="360px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="组数">
          <el-input-number v-model="configForm.sets" :min="1" :max="30" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="configEx?.muscleGroup === '有氧' ? '时长(分钟)' : '每组次数'">
          <el-input-number v-model="configForm.reps" :min="1" :max="100" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="mode === 'add-to-plan'" label="训练日">
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
import { ElMessage } from 'element-plus'
import { exerciseApi, type Exercise } from '../api'

export interface BasketItem {
  name: string
  muscleGroup: string
  sets: number
  reps: number
  dayOfWeek?: number
}

const props = defineProps<{
  visible: boolean
  /** new-plan: 新建计划篮子；add-to-plan: 加入当前计划 */
  mode: 'new-plan' | 'add-to-plan'
  defaultDay?: number
}>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'finish', basket: BasketItem[]): void
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
const expandedTips = ref<Set<number>>(new Set())
const customName = ref('')

const configVisible = ref(false)
const configEx = ref<Exercise>()
const configForm = ref({ sets: 4, reps: 10, dayOfWeek: 1 })

const filtered = computed(() =>
  exercises.value.filter((e) =>
    (currentGroup.value === '全部' || e.muscleGroup === currentGroup.value) &&
    (currentEquip.value === '全部' || e.equipment === currentEquip.value) &&
    (!keyword.value || e.name.includes(keyword.value))))

async function onOpen() {
  basket.value = []
  expandedTips.value = new Set()
  configForm.value.dayOfWeek = props.defaultDay ?? 1
  if (!exercises.value.length) {
    loading.value = true
    try {
      const res: any = await exerciseApi.list()
      exercises.value = res.data
    } finally {
      loading.value = false
    }
  }
}

function openConfig(ex: Exercise) {
  configEx.value = ex
  configForm.value = { sets: 4, reps: 10, dayOfWeek: props.defaultDay ?? 1 }
  configVisible.value = true
}

function confirmConfig() {
  const ex = configEx.value!
  basket.value = [...basket.value, {
    name: ex.name,
    muscleGroup: ex.muscleGroup,
    sets: configForm.value.sets,
    reps: configForm.value.reps,
    dayOfWeek: props.mode === 'add-to-plan' ? configForm.value.dayOfWeek : undefined,
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
    name, muscleGroup: group, sets: 4, reps: 10,
    dayOfWeek: props.mode === 'add-to-plan' ? (props.defaultDay ?? 1) : undefined,
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

function finish() {
  emit('finish', [...basket.value])
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
</style>
