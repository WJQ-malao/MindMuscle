import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 60000 })

/** 清空本地会话（登录/退出/过期共用） */
function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  localStorage.removeItem('roles')
  localStorage.removeItem('avatar')
}

/** 防止 401 时重复跳转 */
let redirectingToLogin = false

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    // 后端统一返回 {code, msg, data}：HTTP 恒为 200，业务错误看 code
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
      ElMessage.warning(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      const expired = !!localStorage.getItem('token')
      clearSession()
      if (!redirectingToLogin && router.currentRoute.value.path !== '/login') {
        redirectingToLogin = true
        // 会话过期给用户明确提示，避免"莫名跳回登录页"
        ElMessage.warning(expired ? '登录已过期，请重新登录' : '请先登录')
        router.push('/login').finally(() => {
          redirectingToLogin = false
        })
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '请求失败')
    return Promise.reject(error)
  },
)

export default request
