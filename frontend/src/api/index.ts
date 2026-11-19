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
  /** 今天已打卡: {动作ID: [已打组号]} */
  todayChecked: (): Promise<any> => request.get('/checkins/today'),
  /** 撤销今天某动作某一组的打卡 */
  undoSetCheckin: (itemId: number, setNo: number) => request.delete(`/checkins/today/${itemId}/${setNo}`),
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
  create: (data: Omit<Exercise, 'id' | 'imageUrl'>) => request.post('/exercises', data),
  update: (id: number, data: Omit<Exercise, 'id' | 'imageUrl'>) => request.put(`/exercises/${id}`, data),
  remove: (id: number) => request.delete(`/exercises/${id}`),
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
