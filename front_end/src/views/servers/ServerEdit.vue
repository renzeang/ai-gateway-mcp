<template>
  <div class="server-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑 Server' : '创建 Server' }}</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="formData.baseUrl" placeholder="请输入 Base URL" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>

        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="formData.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            提交
          </el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { serverApi } from '@/api'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitLoading = ref(false)

const isEdit = ref(false)
const formData = reactive({
  name: '',
  baseUrl: '',
  description: '',
  enabled: true
})

// 校验 URL 端口号
const validateUrlPort = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入 Base URL'))
    return
  }

  try {
    const url = new URL(value)
    const port = url.port

    // 如果有显式端口号，校验范围
    if (port && (port < 0 || port > 65535)) {
      callback(new Error(`端口号必须在 0-65535 之间，当前端口：${port}`))
      return
    }

    // 校验协议
    if (!['http:', 'https:'].includes(url.protocol)) {
      callback(new Error('协议必须是 http 或 https'))
      return
    }

    callback()
  } catch (e) {
    callback(new Error('Base URL 格式不正确'))
  }
}

const formRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' }
  ],
  baseUrl: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' },
    { validator: validateUrlPort, trigger: 'blur' }
  ]
}

const loadData = async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    try {
      const res = await serverApi.get(id)
      Object.assign(formData, res.data)
    } catch (error) {
      ElMessage.error('获取 Server 信息失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitLoading.value = true
    if (isEdit.value) {
      await serverApi.update(route.params.id, formData)
      ElMessage.success('更新成功')
    } else {
      await serverApi.create(formData)
      ElMessage.success('创建成功')
    }
    router.push('/servers')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitLoading.value = false
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
.server-edit {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
