<template>
  <div>
    <el-card shadow="never" class="archive-panel">
      <template #header>
        <div class="card-header">
          <span>📁 计划存档</span>
        </div>
      </template>
      <div v-if="rows.length" class="archive-list">
        <div v-for="a in rows" :key="a.id" class="archive-row">
          <div class="archive-info">
            <div class="archive-name">{{ a.name }}</div>
            <div class="archive-sub">{{ a.goal || '未设目标' }} · {{ (a.createTime || '').slice(0, 10) }}</div>
          </div>
          <div class="archive-ops">
            <el-button size="small" text type="primary" @click="openRename(a)">重命名</el-button>
            <el-button size="small" text type="danger" @click="remove(a)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else :image-size="60" description="还没有存档，可在训练计划页把当前计划存为模板" />
    </el-card>

    <el-dialog v-model="renameVisible" title="重命名存档" width="88%" style="max-width: 420px">
      <el-input v-model="renameValue" placeholder="存档名称" maxlength="100" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRename">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { archiveApi, type PlanArchive } from '../api'

const rows = ref<PlanArchive[]>([])
const renameVisible = ref(false)
const renameValue = ref('')
const renameTarget = ref<PlanArchive>()
const saving = ref(false)

async function load() {
  const res: any = await archiveApi.list()
  rows.value = res.data ?? []
}

function openRename(a: PlanArchive) {
  renameTarget.value = a
  renameValue.value = a.name
  renameVisible.value = true
}

async function doRename() {
  if (!renameValue.value.trim()) {
    ElMessage.warning('请填写存档名称')
    return
  }
  saving.value = true
  try {
    await archiveApi.rename(renameTarget.value!.id, renameValue.value.trim())
    ElMessage.success('已重命名')
    renameVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(a: PlanArchive) {
  await ElMessageBox.confirm(`删除存档「${a.name}」？删除后不可恢复。`, '删除存档', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  await archiveApi.remove(a.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.archive-panel { border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.archive-list { display: flex; flex-direction: column; gap: 8px; }
.archive-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; background: #f7f8fa; border-radius: 8px; gap: 8px;
}
.archive-info { min-width: 0; }
.archive-name { font-weight: 600; }
.archive-sub { color: #999; font-size: 12px; margin-top: 2px; }
.archive-ops { flex-shrink: 0; }
</style>
