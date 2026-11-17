import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue') },
    {
      path: '/',
      component: () => import('../layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '数据看板' } },
        { path: 'chat', component: () => import('../views/ChatView.vue'), meta: { title: 'AI 教练' } },
        { path: 'metrics', component: () => import('../views/BodyMetricView.vue'), meta: { title: '身体数据' } },
        { path: 'workout', component: () => import('../views/WorkoutView.vue'), meta: { title: '训练计划' } },
        { path: 'knowledge', component: () => import('../views/KnowledgeView.vue'), meta: { title: '知识库管理' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') return '/login'
  if (token && to.path === '/login') return '/'
})

export default router
