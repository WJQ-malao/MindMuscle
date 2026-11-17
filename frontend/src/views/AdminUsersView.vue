<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <span>👥 用户管理</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small"
                    :type="r === 'ADMIN' ? 'danger' : 'info'" style="margin-right: 4px">
              {{ r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" :disabled="row.id === myId" @click="openEdit(row)">
              调整角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="调整角色" width="420px">
      <p class="tip">为用户 <b>{{ current?.username }}</b> 分配角色：</p>
      <el-checkbox-group v-model="editRoles">
        <el-checkbox value="USER" label="USER">普通用户（健身功能）</el-checkbox>
        <el-checkbox value="ADMIN" label="ADMIN">系统管理员（平台管理）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api'

interface AdminUser {
  id: number
  username: string
  nickname: string
  roles: string[]
  createTime: string
}

const rows = ref<AdminUser[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const current = ref<AdminUser>()
const editRoles = ref<string[]>([])
const myId = ref(Number(localStorage.getItem('userId')) || 0)

async function load() {
  loading.value = true
  try {
    const res: any = await adminApi.users()
    rows.value = res.data
  } finally {
    loading.value = false
  }
}

function openEdit(row: AdminUser) {
  current.value = row
  editRoles.value = [...row.roles]
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await adminApi.updateRoles(current.value!.id, editRoles.value)
    ElMessage.success('角色已更新')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.tip { margin: 0 0 12px; color: #666; }
</style>
