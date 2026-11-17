import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 60000 })

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
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.msg || error.message || '请求失败')
    return Promise.reject(error)
  },
)

export default request
