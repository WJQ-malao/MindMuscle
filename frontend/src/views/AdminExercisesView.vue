<template>
  <div>
    <el-card shadow="hover">
      <div class="toolbar">
        <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width: 140px" @change="onFilterChange">
          <el-option value="无氧" label="无氧（力量）" />
          <el-option value="有氧" label="有氧" />
        </el-select>
        <el-select v-if="filterCategory !== '有氧'" v-model="filterGroup" placeholder="全部部位" clearable
                   style="width: 140px" @change="load">
          <el-option v-for="g in muscleOptions" :key="g" :value="g" :label="g" />
        </el-select>
        <span class="count-tip">共 {{ list.length }} 个动作</span>
        <el-button type="primary" class="add-btn" @click="openEdit()">＋ 新增动作</el-button>
      </div>

      <!-- 按分类+部位分组展示 -->
      <div v-for="sec in sections" :key="sec.title" class="group-section">
        <div class="group-title">
          <span class="group-badge" :class="sec.title.startsWith('有氧') ? 'aerobic' : 'anaerobic'">
            {{ sec.title.startsWith('有氧') ? '有氧' : '无氧' }}
          </span>
          {{ sec.title }} <span class="group-count">{{ sec.items.length }} 个</span>
        </div>
        <el-table :data="sec.items" v-loading="loading">
          <el-table-column label="插图" width="90">
            <template #default="{ row }">
              <img v-if="row.imageUrl" :src="row.imageUrl" class="ex-img" :alt="row.name" />
              <span v-else class="ex-icon" :style="{ background: groupColor[row.muscleGroup] || '#888' }">
                {{ row.muscleGroup }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="动作名称" min-width="140" />
          <el-table-column prop="muscleGroup" label="部位" width="90" />
          <el-table-column label="器械" width="100">
            <template #default="{ row }">{{ row.equipment || '—' }}</template>
          </el-table-column>
          <el-table-column label="动作要点" min-width="240">
            <template #default="{ row }">
              <span class="tips-cell">{{ row.tips || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="70" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!sections.length && !loading" description="没有符合条件的动作" />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑动作' : '新增动作'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="类别">
          <el-radio-group v-model="form.category" @change="onCategoryChange">
            <el-radio-button value="无氧">无氧（力量）</el-radio-button>
            <el-radio-button value="有氧">有氧</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.category === '无氧'" label="训练部位">
          <el-select v-model="form.muscleGroup" style="width: 100%">
            <el-option v-for="g in muscleOptions" :key="g" :value="g" :label="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作名称">
          <el-input v-model="form.name" placeholder="如：杠铃卧推" maxlength="100" />
        </el-form-item>
        <el-form-item label="器械">
          <el-select v-model="form.equipment" style="width: 100%" clearable placeholder="可空">
            <el-option v-for="e in equipmentOptions" :key="e" :value="e" :label="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作要点">
          <el-input v-model="form.tips" type="textarea" :rows="3" maxlength="2000"
                    placeholder="选填，动作标准与注意事项" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exerciseApi, type Exercise } from '../api'

const muscleOptions = ['胸', '背', '腿', '肩', '手臂', '核心']
const equipmentOptions = ['杠铃', '哑铃', '器械', '自重', '有氧']
const groupColor: Record<string, string> = {
  胸: '#f56c6c', 背: '#e6a23c', 腿: '#909399',
  肩: '#409eff', 手臂: '#b88230', 核心: '#26a69a', 有氧: '#67c23a',
}

const list = ref<Exercise[]>([])
const loading = ref(false)
const filterCategory = ref('')
const filterGroup = ref('')

const editVisible = ref(false)
const saving = ref(false)
const editing = ref<Exercise>()
const form = reactive({ category: '无氧', name: '', muscleGroup: '胸', equipment: '', tips: '', sort: 0 })

/** 分组展示：有氧一组；无氧按部位各一组 */
const sections = computed(() => {
  const result: { title: string; items: Exercise[] }[] = []
  const aerobic = list.value.filter((e) => e.muscleGroup === '有氧')
  if (aerobic.length) result.push({ title: '有氧', items: aerobic })
  for (const g of muscleOptions) {
    const items = list.value.filter((e) => e.muscleGroup === g)
    if (items.length) result.push({ title: `无氧 · ${g}`, items })
  }
  return result
})

async function load() {
  loading.value = true
  try {
    // 前端分组展示，一次取全量再按筛选过滤
    const res: any = await exerciseApi.list()
    let rows: Exercise[] = res.data
    if (filterCategory.value === '有氧') rows = rows.filter((e) => e.muscleGroup === '有氧')
    if (filterCategory.value === '无氧') rows = rows.filter((e) => e.muscleGroup !== '有氧')
    if (filterGroup.value && filterCategory.value !== '有氧') {
      rows = rows.filter((e) => e.muscleGroup === filterGroup.value)
    }
    list.value = rows
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  filterGroup.value = ''
  load()
}

function onCategoryChange() {
  if (form.category === '有氧') form.equipment = '有氧'
  else if (form.equipment === '有氧') form.equipment = ''
}

function openEdit(row?: Exercise) {
  editing.value = row
  form.category = row ? (row.muscleGroup === '有氧' ? '有氧' : '无氧') : '无氧'
  form.name = row?.name ?? ''
  form.muscleGroup = row && row.muscleGroup !== '有氧' ? row.muscleGroup : '胸'
  form.equipment = row?.equipment ?? ''
  form.tips = row?.tips ?? ''
  form.sort = row?.sort ?? 0
  editVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写动作名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      muscleGroup: form.category === '有氧' ? '有氧' : form.muscleGroup,
      equipment: form.equipment,
      tips: form.tips,
      sort: form.sort,
    }
    if (editing.value) {
      await exerciseApi.update(editing.value.id, payload)
      ElMessage.success('已保存')
    } else {
      await exerciseApi.create(payload)
      ElMessage.success('已新增（无插图，选择器中显示为肌群色块）')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Exercise) {
  await ElMessageBox.confirm(
    `确定删除「${row.name}」吗？已加入用户训练计划的历史动作不受影响。`,
    '提示', { type: 'warning' },
  )
  await exerciseApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
.count-tip { color: #999; font-size: 13px; }
.add-btn { margin-left: auto; }
.group-section { margin-bottom: 18px; }
.group-title {
  display: flex; align-items: center; gap: 8px;
  font-weight: 600; font-size: 15px; margin-bottom: 8px;
}
.group-badge {
  padding: 2px 10px; border-radius: 12px; font-size: 12px; color: #fff;
}
.group-badge.aerobic { background: #67c23a; }
.group-badge.anaerobic { background: #e6a23c; }
.group-count { color: #999; font-size: 12px; font-weight: 400; }
.ex-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; border: 1px solid #eee; }
.ex-icon {
  width: 48px; height: 48px; border-radius: 50%; color: #fff; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
}
.tips-cell { color: #888; font-size: 13px; }
</style>
