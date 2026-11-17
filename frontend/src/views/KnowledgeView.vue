<template>
  <div>
    <el-card shadow="hover">
      <template #header>上传健身知识文档（入库后 AI 教练可检索引用）</template>
      <el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.txt,.docx,.md"
                 :on-change="(f: any) => (file = f.raw)" :on-remove="() => (file = null)">
        <el-icon size="40" color="#409eff"><UploadFilled /></el-icon>
        <div>拖拽文件到此处，或点击选择（PDF / TXT / DOCX / MD，≤20MB）</div>
      </el-upload>
      <el-button type="primary" style="margin-top: 12px" :disabled="!file" :loading="uploading"
                 @click="upload">
        {{ uploading ? '解析入库中…' : '开始入库' }}
      </el-button>
    </el-card>

    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>已入库文档</template>
      <el-table :data="files" v-loading="loading" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="chunkCount" label="切片数" width="100" />
        <el-table-column prop="createTime" label="入库时间" width="180" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && files.length === 0" description="知识库为空，请先上传健身知识文档" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi, type KnowledgeFile } from '../api'

const file = ref<File | null>(null)
const files = ref<KnowledgeFile[]>([])
const loading = ref(false)
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await knowledgeApi.files()
    files.value = res.data
  } finally {
    loading.value = false
  }
}

async function upload() {
  if (!file.value) return
  uploading.value = true
  try {
    const res: any = await knowledgeApi.upload(file.value)
    ElMessage.success(`入库成功：${res.data.chunkCount} 个切片`)
    file.value = null
    load()
  } finally {
    uploading.value = false
  }
}

async function remove(row: KnowledgeFile) {
  await ElMessageBox.confirm(`确定从知识库删除「${row.fileName}」吗？`, '提示', { type: 'warning' })
  await knowledgeApi.remove(row.fileName)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
