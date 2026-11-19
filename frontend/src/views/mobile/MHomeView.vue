<template>
  <div>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="num">{{ latest.weightKg ?? '-' }}</div>
        <div class="lbl">体重 kg</div>
      </div>
      <div class="stat-card">
        <div class="num">{{ latest.bodyFat ?? '-' }}</div>
        <div class="lbl">体脂率 %</div>
      </div>
      <div class="stat-card">
        <div class="num">{{ totalDays }}</div>
        <div class="lbl">累计打卡</div>
      </div>
      <div class="stat-card">
        <div class="num">{{ recent7Days }}</div>
        <div class="lbl">近7天</div>
      </div>
    </div>

    <el-card shadow="never" class="m-card">
      <template #header>
        <div class="card-header">
          <span>📋 今日训练</span>
          <router-link to="/m/train" class="more">全部</router-link>
        </div>
      </template>
      <div v-if="todayItems.length" class="item-list">
        <div v-for="it in todayItems" :key="it.id" class="train-item">
          <div>
            <div class="exercise">{{ it.exerciseName }}</div>
            <div class="detail">{{ it.muscleGroup }} · {{ it.sets }}组x{{ it.reps }}{{ it.muscleGroup === '有氧' ? '分钟' : '次' }}</div>
          </div>
          <el-button type="primary" size="small" round @click="$router.push('/m/train')">打卡</el-button>
        </div>
      </div>
      <el-empty v-else :image-size="60" description="今天没有安排训练，或还没有计划" />
    </el-card>

    <el-card shadow="never" class="m-card">
      <template #header>⚖️ 体重趋势（近30天）</template>
      <div ref="chartRef" style="height: 180px"></div>
    </el-card>

    <el-button type="primary" size="large" round style="width: 100%; margin-top: 8px"
               @click="$router.push('/m/chat')">
      🤖 问问 AI 教练
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi, workoutApi, type WorkoutItem, type WorkoutPlan } from '../../api'

const latest = ref<any>({})
const totalDays = ref(0)
const recent7Days = ref(0)
const plans = ref<WorkoutPlan[]>([])
const items = ref<WorkoutItem[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const today = new Date().getDay() || 7 // 周日=7，与后端 dayOfWeek 对齐
const todayItems = computed<WorkoutItem[]>(() =>
  items.value.filter((i) => i.dayOfWeek === today))

async function load() {
  const overview: any = await dashboardApi.overview()
  latest.value = overview.data.latestMetric ?? {}
  totalDays.value = overview.data.totalCheckinDays ?? 0
  recent7Days.value = overview.data.recent7Days ?? 0

  const trend = overview.data.weightTrend ?? []
  chart = echarts.init(chartRef.value!)
  chart.setOption({
    grid: { left: 40, right: 16, top: 20, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map((r: any) => String(r.recordDate).slice(5)) },
    yAxis: { type: 'value', scale: true },
    series: [{ type: 'line', smooth: true, data: trend.map((r: any) => r.weightKg),
      areaStyle: { opacity: 0.15 }, symbolSize: 6 }],
  })

  const p: any = await workoutApi.plans()
  plans.value = p.data
  const active = p.data.find((x: WorkoutPlan) => x.status === 'ACTIVE')
  if (active) {
    const it: any = await workoutApi.items(active.id)
    items.value = it.data
  }
}

onMounted(load)
onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.m-title { margin: 4px 4px 14px; }
.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat-card { background: #fff; border-radius: 12px; padding: 14px; text-align: center; }
.stat-card .num { font-size: 24px; font-weight: 700; }
.stat-card .lbl { color: #999; font-size: 12px; margin-top: 4px; }
.m-card { margin-top: 12px; border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.more { color: #409eff; font-size: 13px; text-decoration: none; }
.item-list { display: flex; flex-direction: column; gap: 10px; }
.train-item { display: flex; justify-content: space-between; align-items: center;
  padding: 10px; background: #f7f8fa; border-radius: 10px; }
.exercise { font-weight: 600; }
.detail { color: #999; font-size: 12px; margin-top: 2px; }
</style>
