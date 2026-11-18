<template>
  <el-dialog :model-value="visible" title="选择动作" width="92%" top="4vh"
             @update:model-value="$emit('close')" @open="onOpen">
    <div class="picker-body">
      <!-- 左侧：部位导航（自由切换，不限定部位） -->
      <aside class="group-nav">
        <div v-for="g in groups" :key="g" class="group-item"
             :class="{ active: currentGroup === g }" @click="currentGroup = g">
          {{ g }}
        </div>
      </aside>

      <!-- 右侧：搜索 + 器械筛选 + 动作卡片 -->
      <div class="picker-main">
        <div class="picker-toolbar">
          <el-input v-model="keyword" placeholder="输入动作名字搜索" clearable size="large" class="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

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
              <button class="ex-add" :disabled="selectedIds.has(ex.id)" @click="toggle(ex)">
                {{ selectedIds.has(ex.id) ? '✓' : '＋' }}
              </button>
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
    </div>

    <template #footer>
      <span class="added-count">已选 {{ basket.length }} 个动作</span>
      <el-button size="large" @click="$emit('close')">继续逛逛</el-button>
      <el-button type="primary" size="large" :disabled="!basket.length" @click="finish">
        完成，去创建计划
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exerciseApi, type Exercise } from '../api'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'finish', basket: Exercise[]): void
}>()

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
/** 已选篮子：先自由挑选，完成后统一创建计划 */
const basket = ref<Exercise[]>([])
const expandedTips = ref<Set<number>>(new Set())
const customName = ref('')

const selectedIds = computed(() => new Set(basket.value.map((e) => e.id)))
const filtered = computed(() =>
  exercises.value.filter((e) =>
    (currentGroup.value === '全部' || e.muscleGroup === currentGroup.value) &&
    (currentEquip.value === '全部' || e.equipment === currentEquip.value) &&
    (!keyword.value || e.name.includes(keyword.value))))

async function onOpen() {
  basket.value = []
  expandedTips.value = new Set()
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

function toggle(ex: Exercise) {
  if (selectedIds.value.has(ex.id)) {
    basket.value = basket.value.filter((e) => e.id !== ex.id)
  } else {
    basket.value = [...basket.value, ex]
  }
}

function addCustom() {
  const name = customName.value.trim()
  if (!name) return
  if (basket.value.some((e) => e.name === name)) {
    ElMessage.warning('该动作已在已选列表中')
    return
  }
  const group = currentGroup.value === '全部' ? '核心' : currentGroup.value
  basket.value = [...basket.value, {
    id: -(basket.value.length + 1), name, muscleGroup: group,
    equipment: '自定义', tips: '', imageUrl: '', sort: 0,
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
.picker-body { display: flex; gap: 16px; min-height: 55vh; }
.group-nav { width: 110px; flex-shrink: 0; border-right: 1px solid #f0f0f0; }
.group-item {
  padding: 12px 14px; cursor: pointer; color: #666; font-size: 15px;
  border-radius: 8px; margin-right: 10px;
}
.group-item.active { background: #ecf5ff; color: #409eff; font-weight: 600; }
.picker-main { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.picker-toolbar { display: flex; gap: 10px; }
.search { flex: 1; }
.equip-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  padding: 5px 14px; border-radius: 16px; background: #f4f4f5; color: #666;
  font-size: 13px; cursor: pointer;
}
.chip.active { background: #409eff; color: #fff; }
.exercise-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px; overflow-y: auto; max-height: 48vh; padding: 2px;
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
.ex-add:disabled { background: #a0cfff; cursor: default; }
.ex-tips {
  margin-top: 10px; padding: 8px 10px; background: #f7f8fa; border-radius: 8px;
  color: #888; font-size: 12px; cursor: pointer; line-height: 1.6;
}
.manual-add { display: flex; gap: 10px; align-items: center; }
.added-count { color: #999; font-size: 13px; margin-right: auto; }
</style>
