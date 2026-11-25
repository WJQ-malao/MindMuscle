<template>
  <div class="welcome-banner">
    <div>
      <div class="wb-title">Welcome to MindMuscle</div>
      <div class="wb-sub">{{ greeting }}，{{ nickname }}，{{ isAdmin ? '平台运行一切正常' : '今天也要加油训练哦' }}</div>
    </div>
    <div class="wb-date">{{ today }}</div>
  </div>

  <!-- ===== 管理员：平台运营视角 ===== -->
  <div v-if="isAdmin">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#eef4ff">👥</span><div>
          <div class="label">注册用户</div>
          <div class="value">{{ adminData.totalUsers ?? '-' }}</div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#e9faf0">✅</span><div>
          <div class="label">训练打卡</div>
          <div class="value">{{ adminData.totalCheckins ?? '-' }}</div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#f0ebff">📋</span><div>
          <div class="label">生成计划</div>
          <div class="value">{{ adminData.totalPlans ?? '-' }}</div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#fff6e8">📚</span><div>
          <div class="label">知识文档</div>
          <div class="value">{{ adminData.knowledgeFiles ?? '-' }}</div></div></div></el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <div class="admin-header">
          <span>🕘 最近注册用户</span>
          <router-link to="/knowledge" class="knowledge-link">📚 维护知识库 →</router-link>
        </div>
      </template>
      <el-table :data="adminData.recentUsers ?? []" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
      </el-table>
    </el-card>
  </div>

  <!-- ===== 普通用户：个人数据视角 ===== -->
  <div v-else>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#eef4ff">⚖️</span><div>
          <div class="label">当前体重</div>
          <div class="value">{{ latest.weightKg ?? '-' }} <span class="unit">kg</span></div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#f0ebff">📊</span><div>
          <div class="label">体脂率</div>
          <div class="value">{{ latest.bodyFat ?? '-' }} <span class="unit">%</span></div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#e9faf0">🏅</span><div>
          <div class="label">累计打卡</div>
          <div class="value">{{ totalDays }} <span class="unit">天</span></div></div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat"><span class="stat-icon" style="background:#fff6e8">🔥</span><div>
          <div class="label">近 7 天打卡</div>
          <div class="value">{{ recent7Days }} <span class="unit">天</span></div></div></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>📈 体重 / 体脂趋势（近 90 天）</template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>🎯 肌群容量分布</template>
          <div ref="muscleRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>🗓️ 训练打卡热力图（近 6 个月）</template>
      <div ref="heatmapRef" class="chart heatmap"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import { adminApi, dashboardApi } from '../api'

const isAdmin = localStorage.getItem('role') === 'ADMIN'
const nickname = localStorage.getItem('nickname') || '朋友'
const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const greeting = (() => {
  const h = now.getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})()
const today = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 星期${weekDays[now.getDay()]}`
const adminData = ref<any>({})
const trendRef = ref<HTMLElement>()
const muscleRef = ref<HTMLElement>()
const heatmapRef = ref<HTMLElement>()
const latest = ref<any>({})
const totalDays = ref(0)
const recent7Days = ref(0)
let charts: echarts.ECharts[] = []

function initTrend(el: HTMLElement, rows: any[]) {
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重(kg)', '体脂率(%)'] },
    xAxis: { type: 'category', data: rows.map((r) => r.recordDate) },
    yAxis: [{ type: 'value', name: 'kg' }, { type: 'value', name: '%', max: 50 }],
    series: [
      { name: '体重(kg)', type: 'line', smooth: true, data: rows.map((r) => r.weightKg), areaStyle: { opacity: 0.15 } },
      { name: '体脂率(%)', type: 'line', smooth: true, yAxisIndex: 1, data: rows.map((r) => r.bodyFat) },
    ],
  })
}

function initMuscle(el: HTMLElement, rows: any[]) {
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: {},
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: rows.map((r) => ({ name: r.muscle, value: Number(r.volume) || 0 })),
      label: { formatter: '{b}: {d}%' },
    }],
  })
}

function initHeatmap(el: HTMLElement, rows: any[]) {
  const chart = echarts.init(el)
  charts.push(chart)
  const dataMap = new Map(rows.map((r) => [String(r.date), Number(r.count)]))
  // 生成近 6 个月日期序列
  const days: string[] = []
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 6)
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    days.push(d.toISOString().slice(0, 10))
  }
  const max = Math.max(1, ...rows.map((r) => Number(r.count)))
  chart.setOption({
    tooltip: { formatter: (p: any) => `${p.data[0]}：训练 ${p.data[1]} 个动作` },
    visualMap: { min: 0, max, type: 'piecewise', orient: 'horizontal', left: 'center',
      pieces: [{ max: 0, color: '#ebedf0', label: '0' },
               { min: 1, max: 1, color: '#9be9a8', label: '1 个' },
               { min: 2, max: 3, color: '#40c463', label: '2-3 个' },
               { min: 4, color: '#30a14e', label: '≥4 个' }] },
    calendar: { range: [days[0], days[days.length - 1]], cellSize: ['auto', 14], top: 60 },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: days.map((d) => [d, dataMap.get(d) ?? 0]),
    }],
  })
}

function resize() {
  charts.forEach((c) => c.resize())
}

onMounted(async () => {
  if (isAdmin) {
    const res: any = await adminApi.overview()
    adminData.value = res.data
    return
  }
  const res: any = await dashboardApi.overview()
  const data = res.data
  latest.value = data.latestMetric ?? {}
  totalDays.value = data.totalCheckinDays ?? 0
  recent7Days.value = data.recent7Days ?? 0
  initTrend(trendRef.value!, data.weightTrend ?? [])
  initMuscle(muscleRef.value!, data.muscleCapacity ?? [])
  initHeatmap(heatmapRef.value!, data.checkinHeatmap ?? [])
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(120deg, #5b8cff 0%, #7a5af8 60%, #a55cf0 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 14px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8px 24px rgba(102, 108, 240, 0.35);
}
.wb-title { font-size: 19px; font-weight: 700; letter-spacing: 0.5px; }
.wb-sub { font-size: 13px; opacity: 0.88; margin-top: 6px; }
.wb-date { font-size: 13px; opacity: 0.88; white-space: nowrap; }

.stat { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 46px; height: 46px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; flex-shrink: 0;
}
.stat .label { color: #999; font-size: 13px; }
.stat .value { font-size: 26px; font-weight: 700; margin-top: 4px; color: #1f2329; }
.stat .unit { font-size: 13px; color: #999; font-weight: 400; }
.admin-header { display: flex; justify-content: space-between; align-items: center; }
.knowledge-link { color: #409eff; text-decoration: none; font-size: 13px; }
.chart { height: 300px; }
.chart.heatmap { height: 220px; }
</style>
