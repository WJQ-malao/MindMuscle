<template>
  <div>
    <h3 class="m-title">🛠️ 平台管理</h3>
    <div class="admin-list">
      <div class="admin-card" @click="$router.push('/m/admin/knowledge')">
        <span class="icon">📚</span>
        <div class="info">
          <div class="name">知识库管理</div>
          <div class="desc">上传健身资料，维护 RAG 知识库</div>
        </div>
        <span class="arrow">›</span>
      </div>
      <div class="admin-card" @click="$router.push('/m/admin/users')">
        <span class="icon">👥</span>
        <div class="info">
          <div class="name">用户管理</div>
          <div class="desc">查看用户列表，调整角色权限</div>
        </div>
        <span class="arrow">›</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.m-title { margin: 4px 4px 14px; }
.admin-list { display: flex; flex-direction: column; gap: 12px; }
.admin-card {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 12px; padding: 16px;
  cursor: pointer; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.admin-card:active { background: #f5f7fa; }
.icon { font-size: 28px; }
.info { flex: 1; min-width: 0; }
.name { font-weight: 600; font-size: 16px; }
.desc { color: #999; font-size: 12px; margin-top: 4px; }
.arrow { color: #c0c4cc; font-size: 22px; }
</style>
