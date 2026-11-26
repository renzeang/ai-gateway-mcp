<template>
  <div class="audit-log-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计日志详情</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="User Token">
          <el-tag round>{{ maskToken(detail.userToken) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求类型">
          <el-tag round>{{ detail.requestType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标名称">{{ detail.targetName }}</el-descriptions-item>
        <el-descriptions-item label="API Key 别名">{{ detail.apiKeyAlias || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联服务 ID">{{ detail.serverId }}</el-descriptions-item>
        <el-descriptions-item label="响应状态">
          <el-tag :type="detail.responseStatus === 'success' ? 'success' : 'warning'" round>
            {{ detail.responseStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时 (ms)">{{ detail.latencyMs }}</el-descriptions-item>
        <el-descriptions-item label="是否错误">
          <el-tag :type="detail.isError ? 'danger' : 'success'" round>
            {{ detail.isError ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorCode" label="错误码">
          {{ detail.errorCode }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMessage" label="错误信息" :span="2">
          {{ detail.errorMessage }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">请求体</el-divider>
      <div class="json-viewer">
        <el-button link type="primary" @click="toggleRequestExpand" style="margin-bottom: 10px">
          {{ requestExpanded ? '收起' : '展开完整' }}
        </el-button>
        <pre v-if="requestExpanded" class="json-content">{{ formatJson(detail?.requestBody) }}</pre>
        <pre v-else class="json-content collapsed">{{ formatJson(detail?.requestBody) }}</pre>
      </div>

      <el-divider content-position="left">响应体</el-divider>
      <div class="json-viewer">
        <el-button link type="primary" @click="toggleResponseExpand" style="margin-bottom: 10px">
          {{ responseExpanded ? '收起' : '展开完整' }}
        </el-button>
        <pre v-if="responseExpanded" class="json-content">{{ formatJson(detail?.responseBody) }}</pre>
        <pre v-else class="json-content collapsed">{{ formatJson(detail?.responseBody) }}</pre>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { auditLogApi } from '@/api'

const router = useRouter()
const route = useRoute()

const detail = ref(null)
const requestExpanded = ref(false)
const responseExpanded = ref(false)

const maskToken = (token) => {
  if (!token) return ''
  if (token.length <= 8) return '***'
  return token.substring(0, 4) + '***' + token.substring(token.length - 4)
}

const formatJson = (jsonStr) => {
  if (!jsonStr) return ''
  try {
    if (typeof jsonStr === 'string') {
      return JSON.stringify(JSON.parse(jsonStr), null, 2)
    }
    return JSON.stringify(jsonStr, null, 2)
  } catch {
    return jsonStr
  }
}

const toggleRequestExpand = () => {
  requestExpanded.value = !requestExpanded.value
}

const toggleResponseExpand = () => {
  responseExpanded.value = !responseExpanded.value
}

const loadData = async () => {
  try {
    const res = await auditLogApi.get(route.params.id)
    detail.value = res.data
  } catch (error) {
    console.error('获取审计日志详情失败:', error)
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.audit-log-detail {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.json-viewer {
  margin: 15px 0;
}

.json-content {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 300px;
  overflow: auto;
}

.json-content.collapsed {
  max-height: 150px;
}
</style>
