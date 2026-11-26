<template>
  <div class="api-key-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>创建 API Key</span>
          <el-button @click="handleBack" data-testid="btn-back-api-key">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="别名" prop="alias">
          <el-input
            v-model="formData.alias"
            placeholder="请输入别名"
            maxlength="100"
            data-testid="input-alias"
          />
        </el-form-item>

        <el-form-item label="Server 作用域">
          <div class="server-select">
            <el-checkbox
              v-model="selectAll"
              @change="handleSelectAll"
              data-testid="checkbox-all-servers"
            >
              全选
            </el-checkbox>
            <el-checkbox-group v-model="formData.serverIds" data-testid="checkbox-servers">
              <el-checkbox
                v-for="server in servers"
                :key="server.id"
                :label="server.id"
              >
                {{ server.name }} ({{ server.baseUrl }})
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>

        <el-form-item label="过期时间">
          <div class="expiry-row">
            <el-checkbox
              v-model="noExpire"
              data-testid="checkbox-no-expire"
              @change="handleNoExpireChange"
            >
              不设置
            </el-checkbox>
            <el-date-picker
              v-if="!noExpire"
              v-model="formData.expiresAt"
              type="datetime"
              placeholder="选择过期时间"
              data-testid="input-expires-at"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :disabled-date="disabledDate"
              style="margin-left: 16px"
            />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
            data-testid="btn-create-api-key"
          >
            创建
          </el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成功弹窗：显示明文 Key（仅一次） -->
    <el-dialog
      v-model="successVisible"
      title="API Key 创建成功"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="success-content">
        <el-alert
          title="请妥善保存以下 Key，关闭后将无法再次查看！"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <div class="key-display">
          <el-input
            :model-value="createdKey"
            readonly
            data-testid="input-created-key"
          />
          <el-button @click="handleCopy" data-testid="btn-copy-key" style="margin-left: 8px">
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleConfirmSaved" data-testid="btn-confirm-saved">
          确认已保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { apiKeyApi, serverApi } from '@/api'

const router = useRouter()

const formRef = ref(null)
const submitLoading = ref(false)
const servers = ref([])

const formData = reactive({
  alias: '',
  serverIds: [],
  expiresAt: ''
})

const noExpire = ref(true)
const selectAll = computed({
  get: () => servers.value.length > 0 && formData.serverIds.length === servers.value.length,
  set: () => {}
})

const formRules = {
  alias: [
    { required: true, message: '请输入别名', trigger: 'blur' },
    { max: 100, message: '别名不能超过 100 个字符', trigger: 'blur' }
  ]
}

const successVisible = ref(false)
const createdKey = ref('')

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

const handleSelectAll = (checked) => {
  if (checked) {
    formData.serverIds = servers.value.map((s) => s.id)
  } else {
    formData.serverIds = []
  }
}

const handleNoExpireChange = (val) => {
  if (val) {
    formData.expiresAt = ''
  }
}

const loadServers = async () => {
  try {
    const res = await serverApi.list({ page: 1, pageSize: 100 })
    servers.value = res.data.records || []
  } catch (error) {
    console.error('获取 Server 列表失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      alias: formData.alias,
      serverIds: formData.serverIds.length > 0 ? formData.serverIds : null,
      expiresAt: noExpire.value ? null : formData.expiresAt || null
    }
    const res = await apiKeyApi.create(payload)
    createdKey.value = res.data.key
    successVisible.value = true
  } catch (error) {
    console.error('创建 API Key 失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleCopy = () => {
  navigator.clipboard.writeText(createdKey.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const handleConfirmSaved = () => {
  successVisible.value = false
  router.push('/api-keys')
}

const handleBack = () => {
  router.push('/api-keys')
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.api-key-create {
  max-width: 720px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.server-select {
  width: 100%;
}

.expiry-row {
  display: flex;
  align-items: center;
}

.success-content {
  padding: 8px 0;
}

.key-display {
  display: flex;
  align-items: center;
}
</style>
