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
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openProfile(row)">查看资料</el-button>
            <el-button size="small" text :disabled="row.id === myId" @click="openEdit(row)">调整角色</el-button>
            <el-button size="small" text type="warning" @click="openReset(row)">重置密码</el-button>
            <el-button size="small" text type="danger" :disabled="row.id === myId" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户资料详情 -->
    <el-dialog v-model="profileVisible" title="用户资料" width="92%" style="max-width: 560px">
      <div v-loading="profileLoading">
        <el-descriptions v-if="profile" :column="2" border size="small">
          <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ profile.nickname }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag v-for="r in profile.roles" :key="r" size="small"
                    :type="r === 'ADMIN' ? 'danger' : 'info'" style="margin-right: 4px">{{ r }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="身高">{{ profile.heightCm ? profile.heightCm + ' cm' : '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ profile.createTime }}</el-descriptions-item>
          <el-descriptions-item label="训练计划">{{ profile.planCount }} 个</el-descriptions-item>
          <el-descriptions-item label="打卡记录">{{ profile.checkinCount }} 条</el-descriptions-item>
          <el-descriptions-item label="体测记录">{{ profile.metricCount }} 条</el-descriptions-item>
          <el-descriptions-item label="计划存档">{{ profile.archiveCount }} 个</el-descriptions-item>
        </el-descriptions>
        <el-card v-if="profile?.recentMetrics?.length" shadow="never" style="margin-top: 12px">
          <template #header><span style="font-weight: 600">📈 最近体测</span></template>
          <el-table :data="profile.recentMetrics" size="small">
            <el-table-column prop="recordDate" label="日期" width="120" />
            <el-table-column prop="weightKg" label="体重 (kg)" />
            <el-table-column prop="bodyFat" label="体脂率 (%)" />
          </el-table>
        </el-card>
        <el-empty v-else-if="profile && !profileLoading" description="暂无体测记录" :image-size="60" />
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="调整角色" width="88%" style="max-width: 420px">
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

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="88%" style="max-width: 420px">
      <p class="tip">为用户 <b>{{ current?.username }}</b> 设置新密码（至少 6 位）：</p>
      <el-input v-model="newPassword" type="password" show-password placeholder="新密码" maxlength="64" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="warning" :loading="resetSaving" @click="doReset">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

const profileVisible = ref(false)
const profileLoading = ref(false)
const profile = ref<any>()

const resetVisible = ref(false)
const resetSaving = ref(false)
const newPassword = ref('')

async function load() {
  loading.value = true
  try {
    const res: any = await adminApi.users()
    rows.value = res.data
  } finally {
    loading.value = false
  }
}

async function openProfile(row: AdminUser) {
  current.value = row
  profileVisible.value = true
  profileLoading.value = true
  try {
    const res: any = await adminApi.userProfile(row.id)
    profile.value = res.data
  } finally {
    profileLoading.value = false
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

function openReset(row: AdminUser) {
  current.value = row
  newPassword.value = ''
  resetVisible.value = true
}

async function doReset() {
  if (newPassword.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetSaving.value = true
  try {
    await adminApi.resetPassword(current.value!.id, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    resetSaving.value = false
  }
}

async function remove(row: AdminUser) {
  await ElMessageBox.confirm(
    `删除用户「${row.username}」？将同时删除其训练计划、打卡、体测、存档等全部个人数据，不可恢复。`,
    '删除用户',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  await adminApi.deleteUser(row.id)
  ElMessage.success('用户已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.tip { margin: 0 0 12px; color: #666; }
</style>
