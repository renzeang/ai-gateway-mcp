import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1/admin',
  timeout: 30000
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('[API 拦截器] 收到响应:', response.config.url, response.status)

    // 204 No Content - 删除成功等情况
    if (response.status === 204) {
      return { code: 'SUCCESS' }
    }

    // 导出请求：返回完整响应（包含响应头）
    if (response.config.returnResponse === true) {
      console.log('[API 拦截器] 返回完整响应')
      return response
    }

    // 其他 blob 类型：直接返回 blob 数据
    if (response.config.responseType === 'blob') {
      console.log('[API 拦截器] 返回 blob 数据')
      return response.data
    }

    // 普通 JSON 响应
    const res = response.data
    if (res.code !== 'SUCCESS') {
      // 如果配置了跳过错误处理，则不显示错误消息
      if (!response.config.skipErrorHandle) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('[API 拦截器] 错误:', error)
    // 如果配置了跳过错误处理，则不显示错误消息
    if (error.config?.skipErrorHandle) {
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// Servers API
export const serverApi = {
  list: (params) => request.get('/servers', { params }),
  get: (id) => request.get(`/servers/${id}`),
  create: (data) => request.post('/servers', data),
  update: (id, data) => request.put(`/servers/${id}`, data),
  delete: (id, cascade = false) => request.delete(`/servers/${id}`, { params: { cascade }, skipErrorHandle: true }),
  getRelations: (id) => request.get(`/servers/${id}/relations`)
}

// Tools API
export const toolApi = {
  list: (params) => request.get('/tools', { params }),
  get: (id) => request.get(`/tools/${id}`),
  create: (data) => request.post('/tools', data),
  update: (id, data) => request.put(`/tools/${id}`, data),
  delete: (id) => request.delete(`/tools/${id}`),
  importPreview: (file, serverId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('serverId', serverId)
    return request.post('/tools/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  confirmImport: (data) => request.post('/tools/confirm-import', data),
  export: (data) => request.post('/tools/export', data, {
    responseType: 'blob',
    returnResponse: true
  })
}

// Resources API
export const resourceApi = {
  list: (params) => request.get('/resources', { params }),
  get: (id) => request.get(`/resources/${id}`),
  create: (data) => request.post('/resources', data),
  update: (id, data) => request.put(`/resources/${id}`, data),
  delete: (id) => request.delete(`/resources/${id}`),
  importPreview: (file, serverId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('serverId', serverId)
    return request.post('/resources/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  confirmImport: (data) => request.post('/resources/confirm-import', data),
  export: (data) => request.post('/resources/export', data, {
    responseType: 'blob',
    returnResponse: true
  })
}

// Prompts API
export const promptApi = {
  list: (params) => request.get('/prompts', { params }),
  get: (id) => request.get(`/prompts/${id}`),
  create: (data) => request.post('/prompts', data),
  update: (id, data) => request.put(`/prompts/${id}`, data),
  delete: (id) => request.delete(`/prompts/${id}`),
  importPreview: (file, serverId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('serverId', serverId)
    return request.post('/prompts/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  confirmImport: (data) => request.post('/prompts/confirm-import', data),
  export: (data) => request.post('/prompts/export', data, {
    responseType: 'blob',
    returnResponse: true
  })
}

// Audit Logs API
export const auditLogApi = {
  list: (params) => request.get('/audit-logs', { params }),
  get: (id) => request.get(`/audit-logs/${id}`)
}

// API Keys API
export const apiKeyApi = {
  list: (params) => request.get('/api-keys', { params }),
  get: (id) => request.get(`/api-keys/${id}`),
  create: (data) => request.post('/api-keys', data),
  toggle: (id, data) => request.put(`/api-keys/${id}`, data),
  delete: (id) => request.delete(`/api-keys/${id}`, { skipErrorHandle: true })
}

// 统一导出
export default {
  serverApi,
  toolApi,
  resourceApi,
  promptApi,
  auditLogApi,
  apiKeyApi
}
