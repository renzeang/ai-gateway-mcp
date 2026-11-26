<template>
  <div class="prompt-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑 Prompt' : '创建 Prompt' }}</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px" style="max-width: 600px; margin: 0 auto;"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入描述"
          />
        </el-form-item>

        <el-form-item label="关联服务" prop="serverId">
          <CapsuleSelect
            v-model="formData.serverId"
            :options="filteredServers"
            placeholder="请选择服务"
            filterable
            :width="'100%'"
          />
        </el-form-item>

        <el-form-item label="模板内容" prop="template">
          <el-input
            v-model="formData.template"
            type="textarea"
            :rows="8"
            placeholder="请输入 Prompt 模板，使用 {{variable}} 表示变量"
          />
        </el-form-item>

        <el-form-item label="参数定义" prop="arguments">
          <div class="arguments-editor">
            <el-table :data="argumentFields" border style="width: 100%">
              <el-table-column label="参数名" width="150">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="参数名" />
                </template>
              </el-table-column>
              <el-table-column label="描述" min-width="200">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="描述" />
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80">
                <template #default="{ row }">
                  <el-checkbox v-model="row.required" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeArgument($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button type="primary" @click="addArgument" style="margin-top: 10px">
              <el-icon><Plus /></el-icon>
              添加参数
            </el-button>
          </div>
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { promptApi, serverApi } from '@/api'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitLoading = ref(false)

const isEdit = ref(false)
const servers = ref([])
const serverSearch = ref('')

// 格式化服务列表并支持模糊搜索
const filteredServers = computed(() => {
  if (!serverSearch.value) return servers.value
  const keyword = serverSearch.value.toLowerCase()
  return servers.value.filter(server =>
    server.name.toLowerCase().includes(keyword) ||
    server.baseUrl.toLowerCase().includes(keyword)
  )
})

const formData = reactive({
  name: '',
  description: '',
  serverId: '',
  template: '',
  arguments: [],
  enabled: true
})

const argumentFields = ref([])

/**
 * 从 Prompt 模板中提取变量（支持 {{variable}} 格式）
 */
const extractTemplateVariables = (template) => {
  if (!template) return []
  const regex = /\{\{(\w+)\}\}/g
  const matches = [...template.matchAll(regex)]
  return [...new Set(matches.map(match => match[1]))] // 去重
}

/**
 * 同步模板变量到参数列表
 */
const syncTemplateVariables = () => {
  const templateParams = extractTemplateVariables(formData.template)

  if (templateParams.length === 0) return

  // 获取现有的参数名
  const existingParams = argumentFields.value.map(field => field.name)

  // 需要添加的新变量
  const newParams = templateParams.filter(name => !existingParams.includes(name))

  // 需要删除的已不存在的变量
  const removedParams = existingParams.filter(name => !templateParams.includes(name))

  // 删除已不存在的参数
  if (removedParams.length > 0) {
    argumentFields.value = argumentFields.value.filter(
      field => !removedParams.includes(field.name)
    )
  }

  // 添加新的参数
  if (newParams.length > 0) {
    newParams.forEach(name => {
      argumentFields.value.push({
        name,
        description: `Template variable: ${name}`,
        required: false
      })
    })
    ElMessage.info(`自动添加参数：${newParams.join(', ')}`)
  }
}

/**
 * 监听模板内容变化，自动提取变量
 */
watch(() => formData.template, () => {
  syncTemplateVariables()
})

const formRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' }
  ],
  serverId: [
    { required: true, message: '请选择服务', trigger: 'change' }
  ],
  template: [
    { required: true, message: '请输入模板内容', trigger: 'blur' }
  ]
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

const loadData = async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    try {
      const res = await promptApi.get(id)
      const data = res.data
      Object.assign(formData, {
        name: data.name,
        description: data.description,
        serverId: data.serverId,
        template: data.template,
        enabled: data.enabled
      })

      // 解析 arguments
      if (data.arguments && Array.isArray(data.arguments)) {
        argumentFields.value = data.arguments.map(arg => ({
          name: arg.name,
          description: arg.description || '',
          required: arg.required || false
        }))
      }
    } catch (error) {
      ElMessage.error('获取 Prompt 信息失败')
    }
  }
}

const addArgument = () => {
  argumentFields.value.push({
    name: '',
    description: '',
    required: false
  })
}

const removeArgument = (index) => {
  argumentFields.value.splice(index, 1)
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitLoading.value = true

    // 构建 arguments（转为 JSON 字符串）
    const argumentsObj = argumentFields.value
      .filter(f => f.name)
      .map(f => ({
        name: f.name,
        description: f.description,
        required: f.required
      }))

    // 创建提交数据，将对象转为 JSON 字符串
    const submitData = {
      ...formData,
      arguments: JSON.stringify(argumentsObj)
    }

    if (isEdit.value) {
      await promptApi.update(route.params.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await promptApi.create(submitData)
      ElMessage.success('创建成功')
    }
    router.back()
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
  fetchServers()
  loadData()
})
</script>

<style scoped>
.prompt-edit {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.arguments-editor {
  width: 100%;
}
</style>
