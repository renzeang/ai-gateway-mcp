<template>
  <div class="server-import">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>导入 Server 配置</span>
        </div>
      </template>

      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="选择服务" />
        <el-step title="上传文件" />
        <el-step title="解析预览" />
        <el-step title="确认导入" />
      </el-steps>

      <div class="step-content">
        <!-- 步骤 1: 选择服务 -->
        <div v-show="activeStep === 0" class="step-panel">
          <el-form :model="form" label-width="100px">
            <el-form-item label="选择服务" required>
              <CapsuleSelect
                v-model="form.serverId"
                :options="serverList.map(s => ({ label: `${s.name} (${s.baseUrl})`, value: s.id }))"
                placeholder="请选择要导入的服务"
                filterable
                :width="'400px'"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!form.serverId" @click="handleNextToStep2">
                下一步
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤 2: 上传文件 -->
        <div v-show="activeStep === 1" class="step-panel">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".json,.yaml,.yml"
            :on-change="handleFileChange"
            :on-remove="handleRemoveFile"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 OpenAPI 3.0/3.1、Swagger 2.0 (JSON/YAML) 格式
              </div>
            </template>
          </el-upload>
          <div class="step-actions">
            <el-button @click="activeStep = 0">上一步</el-button>
            <el-button type="primary" :disabled="!uploadedFile" @click="handleUploadAndParse">
              下一步
            </el-button>
          </div>
        </div>

        <!-- 步骤 3: 解析预览 -->
        <div v-show="activeStep === 2" class="step-panel">
          <div v-loading="parsing" class="preview-container">
            <el-alert
              v-if="previewError"
              type="error"
              :title="previewError"
              show-icon
              closable
            />
            <div v-else-if="tools.length === 0 && resources.length === 0" class="empty-preview">
              <el-empty description="文件中未找到任何 Tools 或 Resources" />
            </div>
            <div v-else class="preview-list">
              <!-- 合并显示所有条目 -->
              <div class="table-header">
                <h3>预览条目 ({{ visibleItems.length }})</h3>
                <div class="batch-actions">
                  <el-button
                    v-if="deletedItems.length > 0"
                    size="small"
                    @click="handleRestoreAll"
                  >
                    恢复全部
                  </el-button>
                  <el-button
                    v-if="selectedItems.length > 0"
                    size="small"
                    type="danger"
                    @click="handleBatchDelete"
                  >
                    删除选中 ({{ selectedItems.length }})
                  </el-button>
                  <el-button
                    v-if="deletedItems.length > 0 && selectedItems.length > 0"
                    size="small"
                    type="success"
                    @click="handleBatchRestore"
                  >
                    恢复选中 ({{ selectedItems.length }})
                  </el-button>
                </div>
              </div>
              <el-table
                :data="visibleItems"
                border
                max-height="500"
                @selection-change="handleSelectionChange"
              >
                <el-table-column type="selection" width="40" />
                <el-table-column prop="name" label="名称" min-width="150">
                  <template #default="{ row }">
                    <el-input
                      v-model="row.name"
                      placeholder="请输入名称"
                      size="small"
                      :disabled="row._deleted"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="path" label="路径" min-width="200">
                  <template #default="{ row }">
                    <el-input
                      v-model="row.path"
                      placeholder="请输入路径"
                      size="small"
                      :disabled="row._deleted"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="method" label="方法" width="100">
                  <template #default="{ row }">
                    <CapsuleSelect
                      v-model="row.method"
                      :options="[
                        { label: 'GET', value: 'GET' },
                        { label: 'POST', value: 'POST' },
                        { label: 'PUT', value: 'PUT' },
                        { label: 'DELETE', value: 'DELETE' },
                        { label: 'PATCH', value: 'PATCH' }
                      ]"
                      placeholder="选择方法"
                      :width="'100%'"
                      :disabled="row._deleted"
                      @change="handleMethodChange(row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="请求体" width="80">
                  <template #default="{ row }">
                    <el-tag
                      :type="hasRequestBody(row) ? 'warning' : 'info'"
                      size="small"
                      round
                    >
                      {{ hasRequestBody(row) ? '有' : '无' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="120">
                  <template #default="{ row }">
                    <CapsuleSelect
                      v-model="row.computedType"
                      :options="[
                        { label: 'Tool', value: 'tool' },
                        { label: 'Resource', value: 'resource' }
                      ]"
                      placeholder="选择类型"
                      :width="'100%'"
                      :disabled="!canSwitchType(row) || row._deleted"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="重复" width="80">
                  <template #default="{ row }">
                    <el-tag
                      v-if="row.duplicate"
                      type="danger"
                      size="small"
                      round
                    >
                      重复
                    </el-tag>
                    <el-tag
                      v-else
                      type="success"
                      size="small"
                      round
                    >
                      唯一
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button
                      v-if="!row._deleted"
                      link
                      type="danger"
                      size="small"
                      @click="handleDeleteItem(row)"
                    >
                      删除
                    </el-button>
                    <el-button
                      v-else
                      link
                      type="success"
                      size="small"
                      @click="handleRestoreItem(row)"
                    >
                      恢复
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="说明" width="100">
                  <template #default="{ row }">
                    <el-tag
                      v-if="!canSwitchType(row)"
                      type="info"
                      size="small"
                      round
                    >
                      固定为{{ row.computedType === 'tool' ? 'Tool' : 'Resource' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-alert
                type="info"
                show-icon
                style="margin-top: 10px"
                title="类型说明"
                :closable="false"
              >
                <template #default>
                  <div>GET 请求且无请求体时可切换 Tool/Resource 类型；有请求体或 POST/PUT/DELETE/PATCH 固定为 Tool 类型（修改方法后类型会自动切换）</div>
                </template>
              </el-alert>
            </div>
          </div>
          <div class="step-actions">
            <el-button @click="activeStep = 1">上一步</el-button>
            <el-button
              v-if="!previewError"
              type="primary"
              :disabled="visibleItems.length === 0"
              @click="handleConfirmImport"
            >
              确认导入 ({{ visibleItems.length }})
            </el-button>
          </div>
        </div>

        <!-- 步骤 4: 确认导入结果 -->
        <div v-show="activeStep === 4" class="step-panel">
          <el-result
            v-if="importResult"
            :icon="importResult.successCount > 0 ? 'success' : 'warning'"
            :title="importResult.successCount > 0 ? '导入成功' : '导入完成'"
            :sub-title="`成功导入 ${importResult.successCount} 项（Tools: ${importResult.toolCount}, Resources: ${importResult.resourceCount}）`"
          >
            <template #extra>
              <el-button type="primary" @click="handleBackToList">返回 Server 列表</el-button>
              <el-button @click="handleReset">继续导入</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { serverApi } from '@/api'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const activeStep = ref(0)
const form = reactive({
  serverId: null
})

const serverList = ref([])
const uploadRef = ref(null)
const uploadedFile = ref(null)
const parsing = ref(false)
const previewError = ref('')
const tools = ref([])
const resources = ref([])
const importResult = ref(null)
const deletedItems = ref([])  // 存储已删除的条目
const selectedItems = ref([])  // 存储选中的条目

// 合并所有条目为一个列表，并添加 computedType 字段
const allItems = computed(() => {
  const all = []

  // 处理 Tools
  tools.value.forEach((tool, index) => {
    all.push({
      ...tool,
      id: `tool-${index}`,
      originalType: 'tool',
      computedType: tool.computedType || 'tool'
    })
  })

  // 处理 Resources
  resources.value.forEach((resource, index) => {
    all.push({
      ...resource,
      id: `resource-${index}`,
      originalType: 'resource',
      computedType: resource.computedType || 'resource'
    })
  })

  return all
})

// 判断是否有请求体（inputSchema 存在且非空）
const hasRequestBody = (row) => {
  if (!row.inputSchema) return false
  // inputSchema 可能是字符串或对象
  if (typeof row.inputSchema === 'string') {
    return row.inputSchema.trim() !== '' && row.inputSchema !== '{}'
  }
  if (typeof row.inputSchema === 'object') {
    return Object.keys(row.inputSchema).length > 0
  }
  return false
}

// 判断是否可以切换类型：GET 请求且无请求体时可切换
const canSwitchType = (row) => {
  // 有请求体，固定为 Tool
  if (hasRequestBody(row)) {
    return false
  }
  // GET 请求且无请求体，可以切换 Tool/Resource
  if (row.method === 'GET') {
    return true
  }
  // POST/PUT/DELETE/PATCH 固定为 Tool
  return false
}

// 处理方法改变时的类型自动切换
const handleMethodChange = (row) => {
  // 如果改为非 GET 方法，强制设置为 Tool 类型
  if (row.method !== 'GET') {
    row.computedType = 'tool'
  }
  // 如果改为 GET 方法，但有请求体，仍然固定为 Tool
  if (row.method === 'GET' && hasRequestBody(row)) {
    row.computedType = 'tool'
  }
  // 如果改为 GET 方法且无请求体，保持当前类型不变（用户可以自己选择）
}

// 可见条目（排除已删除的）
const visibleItems = computed(() => {
  return allItems.value.filter(item => !item._deleted)
})

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedItems.value = selection
}

// 删除单个条目
const handleDeleteItem = (row) => {
  row._deleted = true
  deletedItems.value.push(row)
  // 清除选择
  selectedItems.value = selectedItems.value.filter(item => item.id !== row.id)
}

// 恢复单个条目
const handleRestoreItem = (row) => {
  row._deleted = false
  deletedItems.value = deletedItems.value.filter(item => item.id !== row.id)
}

// 批量删除
const handleBatchDelete = () => {
  selectedItems.value.forEach(item => {
    item._deleted = true
  })
  deletedItems.value.push(...selectedItems.value)
  selectedItems.value = []
  ElMessage.success(`已删除 ${deletedItems.value.length} 项`)
}

// 批量恢复
const handleBatchRestore = () => {
  selectedItems.value.forEach(item => {
    item._deleted = false
  })
  deletedItems.value = deletedItems.value.filter(item => !selectedItems.value.find(s => s.id === item.id))
  selectedItems.value = []
  ElMessage.success(`已恢复 ${selectedItems.value.length} 项`)
}

// 恢复全部
const handleRestoreAll = () => {
  deletedItems.value.forEach(item => {
    item._deleted = false
  })
  deletedItems.value = []
  selectedItems.value = []
  ElMessage.success('已恢复全部条目')
}

// 加载 Server 列表
const loadServerList = async () => {
  try {
    const res = await serverApi.list({ page: 1, pageSize: 1000 })
    serverList.value = (res.data.records || []).map(server => ({
      ...server,
      label: `${server.name} (${server.baseUrl})`
    }))
  } catch (error) {
    console.error('加载 Server 列表失败:', error)
    ElMessage.error('加载 Server 列表失败')
  }
}

const handleServerChange = () => {
  // 清除之前的文件
  uploadedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const handleNextToStep2 = () => {
  activeStep.value = 1
}

const handleFileChange = (file) => {
  uploadedFile.value = file.raw
}

const handleRemoveFile = () => {
  uploadedFile.value = null
}

const handleUploadAndParse = async () => {
  if (!uploadedFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }

  parsing.value = true
  previewError.value = ''
  tools.value = []
  resources.value = []

  try {
    const formData = new FormData()
    formData.append('file', uploadedFile.value)

    const response = await fetch(`/api/v1/admin/servers/${form.serverId}/import/preview`, {
      method: 'POST',
      body: formData
    })

    const result = await response.json()
    console.log('[ServerImport] 解析预览响应:', result)

    if (result.code === 'SUCCESS' && result.data) {
      // 为每个条目添加 computedType 字段和删除标记
      tools.value = (result.data.tools || []).map(t => ({
        ...t,
        computedType: 'tool',
        _deleted: t.duplicate || false  // 重复的条目自动标记为删除
      }))
      resources.value = (result.data.resources || []).map(r => ({
        ...r,
        computedType: 'resource',
        _deleted: r.duplicate || false  // 重复的条目自动标记为删除
      }))
      console.log('[ServerImport] Tools:', tools.value.length, 'Resources:', resources.value.length)
      console.log('[ServerImport] 重复条目已自动标记为删除')
      activeStep.value = 2  // 跳转到步骤 3：解析预览
    } else {
      previewError.value = result.message || result.code || '解析失败'
    }
  } catch (error) {
    console.error('[ServerImport] 解析失败:', error)
    previewError.value = '解析失败：' + (error.message || '未知错误')
  } finally {
    parsing.value = false
  }
}

const handleConfirmImport = async () => {
  try {
    // 使用 visibleItems 准备导入数据（排除已删除的条目）
    const items = visibleItems.value.map(item => {
      const baseItem = {
        type: item.computedType, // 使用用户选择的类型
        name: item.name,         // 用户编辑后的名称
        path: item.path,         // 用户编辑后的路径
        method: item.method,     // 用户编辑后的方法
        description: item.description,
        skip: false
      }

      // 根据类型添加特定字段
      if (item.computedType === 'tool') {
        baseItem.inputSchema = item.inputSchema
        baseItem.headerMappings = item.headerMappings
      } else {
        baseItem.template = item.template
      }

      return baseItem
    })

    console.log('[ServerImport] 准备导入的条目:', items)
    console.log('[ServerImport] 条目数量:', items.length)
    console.log('[ServerImport] Tools:', items.filter(i => i.type === 'tool').length)
    console.log('[ServerImport] Resources:', items.filter(i => i.type === 'resource').length)

    const response = await fetch(`/api/v1/admin/servers/${form.serverId}/import/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ items })
    })

    const result = await response.json()

    if (result.code === 'SUCCESS') {
      importResult.value = result.data
      activeStep.value = 4  // 跳转到步骤 4：确认导入结果
    } else {
      ElMessage.error('导入失败：' + result.message)
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败：' + error.message)
  }
}

const handleBackToList = () => {
  router.push('/servers')
}

const handleReset = () => {
  activeStep.value = 0
  form.serverId = null
  uploadedFile.value = null
  tools.value = []
  resources.value = []
  importResult.value = null
  previewError.value = ''
  deletedItems.value = []
  selectedItems.value = []
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

onMounted(() => {
  loadServerList()
})
</script>

<style scoped>
.server-import {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-content {
  margin-top: 30px;
}

.step-panel {
  padding: 20px 0;
}

.step-actions {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.preview-container {
  min-height: 300px;
}

.empty-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.preview-list h3 {
  margin: 10px 0;
  font-size: 16px;
  color: #303133;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.table-header h3 {
  margin: 0;
}

.batch-actions {
  display: flex;
  gap: 10px;
}

:deep(.el-steps) {
  margin-bottom: 30px;
}
</style>
