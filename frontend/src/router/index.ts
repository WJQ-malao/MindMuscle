import { createRouter, createWebHistory } from 'vue-router'

/** 简单判定移动端：屏幕宽度或 UA */
function isMobile(): boolean {
  return window.innerWidth < 768 || /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue') },
    {
      // ===== PC 端 =====
      path: '/',
      component: () => import('../layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '数据看板' } },
        { path: 'chat', component: () => import('../views/ChatView.vue'), meta: { title: 'AI 教练' } },
        { path: 'metrics', component: () => import('../views/BodyMetricView.vue'), meta: { title: '我的' } },
        { path: 'workout', component: () => import('../views/WorkoutView.vue'), meta: { title: '训练计划' } },
        { path: 'knowledge', component: () => import('../views/KnowledgeView.vue'), meta: { title: '知识库管理', admin: true } },
        { path: 'admin/users', component: () => import('../views/AdminUsersView.vue'), meta: { title: '用户管理', admin: true } },
        { path: 'admin/exercises', component: () => import('../views/AdminExercisesView.vue'), meta: { title: '动作库管理', admin: true } },
      ],
    },
    {
      // ===== 移动端 =====
      path: '/m',
      component: () => import('../layout/MobileLayout.vue'),
      redirect: '/m/home',
      children: [
        { path: 'home', component: () => import('../views/mobile/MHomeView.vue'), meta: { title: '首页', tab: true } },
        { path: 'chat', component: () => import('../views/mobile/MChatView.vue'), meta: { title: 'AI 教练', tab: true } },
        { path: 'train', component: () => import('../views/mobile/MTrainView.vue'), meta: { title: '训练', tab: true } },
        { path: 'profile', component: () => import('../views/mobile/MProfileView.vue'), meta: { title: '我的', tab: true } },
        // 管理员：复用 PC 端页面（表格类组件在窄屏下可横向滚动）
        { path: 'admin', component: () => import('../views/mobile/MAdminView.vue'), meta: { title: '平台管理', admin: true } },
        { path: 'admin/knowledge', component: () => import('../views/KnowledgeView.vue'), meta: { title: '知识库管理', admin: true } },
        { path: 'admin/users', component: () => import('../views/AdminUsersView.vue'), meta: { title: '用户管理', admin: true } },
        { path: 'admin/exercises', component: () => import('../views/AdminExercisesView.vue'), meta: { title: '动作库管理', admin: true } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') return '/login'
  if (token && to.path === '/login') {
    // PC 端：管理员落到平台概览；移动端：管理员落到平台管理，普通用户落到首页
    if (isMobile()) return localStorage.getItem('role') === 'ADMIN' ? '/m/admin' : '/m/home'
    return '/dashboard'
  }

  if (token) {
    // 自动分流：手机访问PC路由 → 移动版；PC显式访问/m/路由 → 放行（便于开发调试与演示）
    if (isMobile() && !to.path.startsWith('/m')) return '/m/home'
    // 管理员专属页面
    if (to.meta.admin && localStorage.getItem('role') !== 'ADMIN') return '/dashboard'
  }
})

export default router
