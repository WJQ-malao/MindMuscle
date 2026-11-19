import request from './request'

export interface BodyMetric {
  id: number
  userId: number
  weightKg: number
  bodyFat: number | null
  recordDate: string
}

export interface WorkoutPlan {
  id: number
  title: string
  goal: string
  status: string
  source: string
  /** 计划所属周的周一日期（YYYY-MM-DD），一天一计划 */
  weekStart?: string
  createTime: string
}

export interface WorkoutItem {
  id: number
  planId: number
  dayOfWeek: number
  exerciseName: string
  muscleGroup: string
  sets: number
  reps: number
  weightKg?: number | null
}

export interface KnowledgeFile {
  id: number
  fileName: string
  chunkCount: number
  createTime: string
}

export const authApi = {
  login: (data: { username: string; password: string }) => request.post('/auth/login', data),
  register: (data: { username: string; password: string; nickname?: string; heightCm?: number }) =>
    request.post('/auth/register', data),
}

export const dashboardApi = {
  overview: () => request.get('/dashboard/overview'),
}

export const adminApi = {
  overview: () => request.get('/admin/overview'),
  users: () => request.get('/admin/users'),
  updateRoles: (userId: number, roles: string[]) =>
    request.put(`/admin/users/${userId}/roles`, { roles }),
  /** 用户资料详情（基本信息+统计+最近体测） */
  userProfile: (userId: number) => request.get(`/admin/users/${userId}`),
  resetPassword: (userId: number, newPassword: string) =>
    request.put(`/admin/users/${userId}/reset-password`, { newPassword }),
  deleteUser: (userId: number) => request.delete(`/admin/users/${userId}`),
  /** AI 教练开关：管理员在知识库管理页控制，关闭后用户发消息只收到提示 */
  aiCoachStatus: () => request.get('/admin/config/ai-coach'),
  setAiCoach: (enabled: boolean) => request.put('/admin/config/ai-coach', { enabled }),
}

export const metricApi = {
  list: (days?: number) => request.get('/metrics', { params: { days } }),
  add: (data: { weightKg: number; bodyFat?: number; recordDate?: string }) => request.post('/metrics', data),
  remove: (id: number) => request.delete(`/metrics/${id}`),
}

export interface ItemPayload {
  exerciseName: string
  muscleGroup: string
  dayOfWeek: number
  sets: number
  reps: number
  weightKg?: number | null
}

/** 创建计划时的动作项（训练日由后端按每周天数自动分摊） */
export interface PlanItemPayload {
  exerciseName: string
  muscleGroup: string
  sets: number
  reps: number
  weightKg?: number | null
  /** 指定训练日（1-7），空=轮流分配；从存档复用时带上原训练日 */
  dayOfWeek?: number
}

export interface PlanArchive {
  id: number
  name: string
  goal: string
  createTime: string
}

export interface PlanArchiveItem {
  id: number
  archiveId: number
  dayOfWeek: number
  exerciseName: string
  muscleGroup: string
  sets: number
  reps: number
  weightKg?: number | null
}

/** 存档内容项（创建/替换存档时的载荷） */
export interface ArchiveItemPayload {
  dayOfWeek: number
  exerciseName: string
  muscleGroup: string
  sets: number
  reps: number
  weightKg?: number | null
}

export const workoutApi = {
  plans: () => request.get('/plans'),
  createPlan: (data: {
    title: string
    goal: string
    daysPerWeek: number
    items: PlanItemPayload[]
  }) => request.post('/plans', data),
  items: (planId: number) => request.get(`/plans/${planId}/items`),
  addItem: (planId: number, data: ItemPayload) => request.post(`/plans/${planId}/items`, data),
  updateItem: (itemId: number, data: ItemPayload) => request.put(`/items/${itemId}`, data),
  removeItem: (itemId: number) => request.delete(`/items/${itemId}`),
  checkin: (data: { itemId: number; setNo: number; repsDone: number; weightUsed?: number }) =>
    request.post('/checkins', data),
  recent: () => request.get('/checkins/recent'),
  /** 已打卡: {动作ID: [已打组号]}，默认今天，可传 date=YYYY-MM-DD 查历史日期 */
  todayChecked: (date?: string): Promise<any> =>
    request.get('/checkins/today', { params: date ? { date } : {} }),
  /** 撤销某动作某一组的打卡，可传 date 撤销历史记录 */
  undoSetCheckin: (itemId: number, setNo: number, date?: string) =>
    request.delete(`/checkins/today/${itemId}/${setNo}`, { params: date ? { date } : {} }),
}

/** 计划存档（模板）：保存/管理/复用训练计划 */
export const archiveApi = {
  list: () => request.get('/plan-archives'),
  items: (archiveId: number) => request.get(`/plan-archives/${archiveId}/items`),
  /** 把训练计划快照保存为存档 */
  create: (data: { planId: number; name: string }) => request.post('/plan-archives', data),
  /** 从动作清单直接创建存档（选择器里"存为存档"） */
  createFromItems: (data: { name: string; goal?: string; items: ArchiveItemPayload[] }) =>
    request.post('/plan-archives/from-items', data),
  /** 整体替换存档内容（存档编辑） */
  replaceItems: (archiveId: number, data: { name?: string; goal?: string; items: ArchiveItemPayload[] }) =>
    request.put(`/plan-archives/${archiveId}/items`, data),
  rename: (archiveId: number, name: string) =>
    request.put(`/plan-archives/${archiveId}`, { name }),
  remove: (archiveId: number) => request.delete(`/plan-archives/${archiveId}`),
}

export interface Exercise {
  id: number
  name: string
  muscleGroup: string
  equipment?: string
  tips?: string
  imageUrl?: string
  sort: number
}

export const exerciseApi = {
  list: (): Promise<any> => request.get('/exercises'),
  create: (data: Omit<Exercise, 'id'>) => request.post('/exercises', data),
  update: (id: number, data: Omit<Exercise, 'id'>) => request.put(`/exercises/${id}`, data),
  remove: (id: number) => request.delete(`/exercises/${id}`),
  uploadImage: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/exercises/upload-image', form)
  },
}

export const knowledgeApi = {
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/knowledge/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  files: () => request.get('/knowledge/files'),
  preview: (id: number) => request.get(`/knowledge/files/${id}/preview`),
  /** 下载原始文件：axios 携带 token 拿 blob，浏览器触发保存 */
  download: async (id: number, fileName: string) => {
    const resp = await fetch(`/api/knowledge/files/${id}/download`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    if (!resp.ok) throw new Error('下载失败')
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  },
  remove: (id: number) => request.delete(`/knowledge/files/${id}`),
}

export const chatApi = {
  /** 流式对话：fetch + ReadableStream 读取 SSE */
  stream: async (
    message: string,
    conversationId: string,
    onChunk: (text: string) => void,
  ): Promise<void> => {
    const resp = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ message, conversationId }),
    })
    if (!resp.ok || !resp.body) {
      throw new Error(`请求失败: ${resp.status}`)
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 格式：以空行分隔的事件块，data: 开头为内容行
      const events = buffer.split('\n\n')
      buffer = events.pop() ?? ''
      for (const event of events) {
        for (const line of event.split('\n')) {
          if (line.startsWith('data:')) {
            onChunk(line.slice(5).trim())
          }
        }
      }
    }
  },
}
