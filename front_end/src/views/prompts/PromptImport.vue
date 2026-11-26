<template>
  <div class="prompt-import">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>导入 Prompts</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="上传文件" />
        <el-step title="解析预览" />
        <el-step title="冲突处理" />
        <el-step title="确认导入" />
      </el-steps>

      <!-- 步骤 1: 上传文件 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form :model="importForm" label-width="120px" style="max-width: 600px; margin: 0 auto 20px;">
          <el-form-item label="选择服务" required>
            <CapsuleSelect
              v-model="importForm.serverId"
              :options="filteredServers"
              placeholder="请选择服务"
              filterable
              :width="'100%'"
            />
          </el-form-item>
        </el-form>
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept=".json"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 JSON 格式：{ "prompts": [{ "name", "description", "template", "arguments" }] }
            </div>
          </template>
        </el-upload>
        <div v-if="previewData" class="preview-info">
          <el-alert
            title="文件解析成功"
            type="success"
            :closable="false"
            style="margin-top: 20px"
          />
          <el-descriptions :column="2" border style="margin-top: 15px">
            <el-descriptions-item label="目标服务">
              {{ getServerName(importForm.serverId) }}
            </el-descriptions-item>
            <el-descriptions-item label="Prompts 数量">
              {{ previewData.prompts?.length || 0 }}
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="previewData.prompts" border style="margin-top: 15px" max-height="300">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="参数数量">
              <template #default="{ row }">
                {{ row.arguments?.length || 0 }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="step-actions">
          <el-button type="primary" :disabled="!previewData || !importForm.serverId" @click="nextStep">
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤 2: 冲突处理 -->
      <div v-show="currentStep === 1" class="step-content">
        <div v-if="conflicts.length === 0">
          <el-alert title="无冲突" type="success" :closable="false" />
          <p style="margin-top: 15px">没有发现同名 Prompt，可以直接导入</p>
        </div>
        <div v-else>
          <el-alert
            title="发现命名冲突"
            type="warning"
            :closable="false"
            description="请选择每个冲突项的处理方式"
          />
          <el-table :data="conflicts" border style="margin-top: 15px">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="处理方式" width="200">
              <template #default="{ row }">
                <el-radio-group v-model="row.action">
                  <el-radio :label="'skip'">跳过</el-radio>
                  <el-radio :label="'override'">覆盖</el-radio>
                </el-radio-group>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="handleImport">
            确认导入
          </el-button>
        </div>
      </div>

      <!-- 步骤 3: 确认导入 -->
      <div v-show="currentStep === 2" class="step-content">
        <div v-loading="importing" element-loading-text="导入中...">
          <el-result
            v-if="importResult"
            :icon="importResult.success ? 'success' : 'error'"
            :title="importResult.success ? '导入成功' : '导入失败'"
          >
            <template #sub-title>
              <p>成功：{{ importResult.successCount || 0 }}</p>
              <p>跳过：{{ importResult.skipCount || 0 }}</p>
              <p>覆盖：{{ importResult.overrideCount || 0 }}</p>
              <p v-if="importResult.error">错误：{{ importResult.error }}</p>
            </template>
            <template #extra>
              <el-button type="primary" @click="handleFinish">完成</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { promptApi, serverApi } from '@/api'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const uploadRef = ref(null)
const currentStep = ref(0)
const previewData = ref(null)
const selectedFile = ref(null)
const conflicts = ref([])
const importing = ref(false)
const importResult = ref(null)
const servers = ref([])

const importForm = reactive({
  serverId: ''
})

const filteredServers = computed(() => {
  return servers.value
})

const getServerName = (serverId) => {
  const server = servers.value.find(s => s.id === serverId)
  return server ? `${server.name} (${server.baseUrl})` : ''
}

const fetchServers = async () => {
  try {
    const res = await serverApi.list({ pageSize: 100 })
    servers.value = (res.data.records || []).map(server => ({
      label: `${server.name} (${server.baseUrl})`,
      value: server.id,
      name: server.name,
      baseUrl: server.baseUrl
    }))
  } catch (error) {
    console.error('获取服务列表失败:', error)
  }
}

const handleFileChange = async (file) => {
  if (!importForm.serverId) {
    ElMessage.warning('请先选择服务')
    return
  }
  selectedFile.value = file.raw
  try {
    const res = await promptApi.importPreview(file.raw, importForm.serverId)
    previewData.value = res.data
    ElMessage.success('文件解析成功')
  } catch (error) {
    ElMessage.error('文件解析失败')
    previewData.value = null
  }
}

const nextStep = () => {
  if (currentStep.value < 2) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleConfirmPreview = () => {
  if (previewData.value?.conflicts) {
    conflicts.value = previewData.value.conflicts.map(c => ({
      ...c,
      action: 'skip'
    }))
  }
  nextStep()
}

const handleImport = async () => {
  importing.value = true
  try {
    const conflictConfig = conflicts.value.reduce((acc, c) => {
      acc[c.name] = c.action
      return acc
    }, {})

    const res = await promptApi.confirmImport({
      serverId: importForm.serverId,
      conflicts: conflictConfig
    })

    importResult.value = {
      success: true,
      successCount: res.data?.successCount || 0,
      skipCount: res.data?.skipCount || 0,
      overrideCount: res.data?.overrideCount || 0
    }
  } catch (error) {
    importResult.value = {
      success: false,
      error: error.message || '导入失败'
    }
  } finally {
    importing.value = false
  }
  nextStep()
}

const handleFinish = () => {
  router.push('/prompts')
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  fetchServers()
})
</script>

<style scoped>
.prompt-import {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-content {
  padding: 30px 0;
  min-height: 300px;
}

.step-actions {
  margin-top: 20px;
  text-align: center;
}

.preview-info {
  margin-top: 20px;
}
</style>
