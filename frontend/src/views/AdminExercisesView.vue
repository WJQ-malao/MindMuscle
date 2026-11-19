<template>
  <div>
    <el-card shadow="hover">
      <div class="toolbar">
        <el-select v-model="filterGroup" placeholder="全部肌群" clearable style="width: 160px" @change="load">
          <el-option v-for="g in muscleOptions" :key="g" :value="g" :label="g" />
        </el-select>
        <span class="count-tip">共 {{ list.length }} 个动作</span>
        <el-button type="primary" class="add-btn" @click="openEdit()">＋ 新增动作</el-button>
      </div>

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column label="插图" width="90">
          <template #default="{ row }">
            <img v-if="row.imageUrl" :src="row.imageUrl" class="ex-img" :alt="row.name" />
            <span v-else class="ex-icon" :style="{ background: groupColor[row.muscleGroup] || '#888' }">
              {{ row.muscleGroup }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="动作名称" min-width="140" />
        <el-table-column prop="muscleGroup" label="肌群" width="90" />
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
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑动作' : '新增动作'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="动作名称">
          <el-input v-model="form.name" placeholder="如：杠铃卧推" maxlength="100" />
        </el-form-item>
        <el-form-item label="肌群">
          <el-select v-model="form.muscleGroup" style="width: 100%">
            <el-option v-for="g in muscleOptions" :key="g" :value="g" :label="g" />
          </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exerciseApi, type Exercise } from '../api'

const muscleOptions = ['胸', '背', '腿', '肩', '手臂', '核心', '有氧']
const equipmentOptions = ['杠铃', '哑铃', '器械', '自重', '有氧']
const groupColor: Record<string, string> = {
  胸: '#f56c6c', 背: '#e6a23c', 腿: '#909399',
  肩: '#409eff', 手臂: '#b88230', 核心: '#26a69a', 有氧: '#67c23a',
}

const list = ref<Exercise[]>([])
const loading = ref(false)
const filterGroup = ref('')

const editVisible = ref(false)
const saving = ref(false)
const editing = ref<Exercise>()
const form = reactive({ name: '', muscleGroup: '胸', equipment: '', tips: '', sort: 0 })

async function load() {
  loading.value = true
  try {
    const res: any = await exerciseApi.list(filterGroup.value || undefined)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openEdit(row?: Exercise) {
  editing.value = row
  form.name = row?.name ?? ''
  form.muscleGroup = row?.muscleGroup ?? '胸'
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
      muscleGroup: form.muscleGroup,
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
.ex-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; border: 1px solid #eee; }
.ex-icon {
  width: 48px; height: 48px; border-radius: 50%; color: #fff; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
}
.tips-cell { color: #888; font-size: 13px; }
</style>
