<template>
  <div class="resource-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑 Resource' : '创建 Resource' }}</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="URI" prop="uri">
              <el-input v-model="formData.uri" placeholder="请输入 URI，如 /users/{id}" @input="handleUriChange" />
            </el-form-item>

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

            <el-form-item label="路径" prop="path">
              <el-input v-model="formData.path" placeholder="请输入后端路径，如 /users/{id}" @input="handlePathChange" />
            </el-form-item>

            <el-form-item label="方法" prop="method">
              <CapsuleSelect
                v-model="formData.method"
                :options="[
                  { label: 'GET', value: 'GET' },
                  { label: 'POST', value: 'POST' },
                  { label: 'PUT', value: 'PUT' },
                  { label: 'DELETE', value: 'DELETE' }
                ]"
                placeholder="请选择方法"
                :width="'100%'"
              />
            </el-form-item>

            <el-form-item label="Template">
              <el-switch v-model="formData.template" active-text="是模板" inactive-text="非模板" />
            </el-form-item>

            <el-form-item label="MIME 类型" prop="mimeType">
              <CapsuleSelect
                v-model="formData.mimeType"
                :options="[
                  { label: 'application/json', value: 'application/json' },
                  { label: 'text/plain', value: 'text/plain' },
                  { label: 'text/html', value: 'text/html' },
                  { label: 'application/xml', value: 'application/xml' }
                ]"
                placeholder="请选择 MIME 类型"
                :width="'100%'"
              />
            </el-form-item>

            <el-form-item label="启用状态" prop="enabled">
              <el-switch v-model="formData.enabled" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <!-- InputSchema 编辑器 -->
            <el-divider content-position="left">Input Schema 配置</el-divider>
            <SchemaTreeEditor v-model="schemaFields" />
            <SchemaPreview :fields="schemaFields" />
          </el-col>
        </el-row>

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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resourceApi, serverApi } from '@/api'
import { SchemaTreeEditor, SchemaPreview } from '@/components/schema'
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
  uri: '',
  name: '',
  description: '',
  serverId: '',
  path: '',
  method: 'GET',
  template: false,
  mimeType: 'application/json',
  enabled: true,
  inputSchema: {
    type: 'object',
    properties: {},
    required: []
  },
  headerMappings: []
})

// 树形字段配置
const schemaFields = ref([])

/**
 * 从模板字符串中提取变量（支持 {variable} 格式）
 */
const extractTemplateVariables = (template) => {
  if (!template) return []
  const regex = /\{([^}]+)\}/g
  const matches = [...template.matchAll(regex)]
  return matches.map(match => match[1])
}

/**
 * 同步模板变量到字段列表
 */
const syncTemplateVariables = (template, paramType) => {
  const templateParams = extractTemplateVariables(template)

  if (templateParams.length === 0) return

  // 获取现有的该类型字段
  const existingFields = schemaFields.value
    .filter(field => field.paramType === paramType)
    .map(field => field.name)

  // 需要添加的新变量
  const newParams = templateParams.filter(name => !existingFields.includes(name))

  // 需要删除的已不存在的变量
  const removedParams = existingFields.filter(name => !templateParams.includes(name))

  // 删除已不存在的变量
  if (removedParams.length > 0) {
    schemaFields.value = schemaFields.value.filter(
      field => field.paramType !== paramType || !removedParams.includes(field.name)
    )
  }

  // 添加新的变量
  if (newParams.length > 0) {
    newParams.forEach(name => {
      schemaFields.value.push({
        id: `${paramType}_${name}_${Date.now()}`,
        name,
        type: 'string',
        paramType: paramType,
        headerName: '',
        description: `${paramType} variable: ${name}`,
        required: true,
        children: []
      })
    })
    ElMessage.info(`自动添加${paramType}变量：${newParams.join(', ')}`)
  }
}

/**
 * 处理 URI 变化，自动提取模板变量
 */
const handleUriChange = () => {
  syncTemplateVariables(formData.uri, 'path')
}

/**
 * 处理路径变化，自动提取模板变量
 */
const handlePathChange = () => {
  syncTemplateVariables(formData.path, 'path')
}

const formRules = {
  uri: [
    { required: true, message: '请输入 URI', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' }
  ],
  serverId: [
    { required: true, message: '请选择服务', trigger: 'change' }
  ],
  path: [
    { required: true, message: '请输入路径', trigger: 'blur' }
  ],
  method: [
    { required: true, message: '请选择方法', trigger: 'change' }
  ]
}

/**
 * 递归解析 inputSchema 为树形字段结构
 */
const parseSchemaToTree = (properties, required = [], parentPath = '') => {
  const fields = []
  if (!properties) return fields

  Object.entries(properties).forEach(([name, config]) => {
    const field = {
      id: parentPath ? `${parentPath}.${name}` : name,
      name,
      type: config.type || 'string',
      paramType: config['x-param-type'] || 'json',
      headerName: config['x-header-name'] || '',
      description: config.description || '',
      required: required.includes(name) || config['x-required'] || false,
      children: []
    }

    // 处理嵌套对象
    if (config.type === 'object' && config.properties) {
      field.children = parseSchemaToTree(
        config.properties,
        config.required || [],
        field.id
      )
    }

    // 处理数组元素
    if (config.type === 'array' && config.items && config.items.properties) {
      field.children = parseSchemaToTree(
        config.items.properties,
        config.items.required || [],
        field.id
      )
    }

    fields.push(field)
  })

  return fields
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
      const res = await resourceApi.get(id)
      const data = res.data
      Object.assign(formData, {
        uri: data.uri,
        name: data.name,
        description: data.description,
        serverId: data.serverId,
        path: data.path,
        method: data.method,
        template: data.template === 'true' || data.template === true,
        mimeType: data.mimeType,
        enabled: data.enabled
      })

      // 解析 inputSchema 为树形数据
      let inputSchema = data.inputSchema
      if (typeof data.inputSchema === 'string') {
        try {
          inputSchema = JSON.parse(data.inputSchema)
        } catch (e) {
          console.error('解析 inputSchema 失败:', e)
          inputSchema = null
        }
      }
      if (inputSchema && inputSchema.properties) {
        schemaFields.value = parseSchemaToTree(
          inputSchema.properties,
          inputSchema.required || []
        )
      }
    } catch (error) {
      ElMessage.error('获取 Resource 信息失败')
    }
  }
}

/**
 * 递归构建树形 Schema
 */
const buildSchemaFromTree = (fields, targetProperties, targetRequired) => {
  if (!fields) return

  fields.forEach(field => {
    if (!field.name) return

    const propSchema = {
      type: field.type
    }

    if (field.description) {
      propSchema.description = field.description
    }

    // 添加入参方式标记
    if (field.paramType && field.paramType !== 'json') {
      propSchema['x-param-type'] = field.paramType
    }

    // 添加 Header 名称
    if (field.paramType === 'header' && field.headerName) {
      propSchema['x-header-name'] = field.headerName
    }

    // 处理嵌套对象
    if (field.type === 'object' && field.children && field.children.length > 0) {
      propSchema.properties = {}
      propSchema.required = []
      buildSchemaFromTree(field.children, propSchema.properties, propSchema.required)
      if (propSchema.required.length === 0) delete propSchema.required
    }

    // 处理数组
    if (field.type === 'array' && field.children && field.children.length > 0) {
      const itemSchema = {
        type: 'object',
        properties: {},
        required: []
      }
      buildSchemaFromTree(field.children, itemSchema.properties, itemSchema.required)
      if (itemSchema.required.length === 0) delete itemSchema.required
      propSchema.items = itemSchema
    }

    targetProperties[field.name] = propSchema

    if (field.required) {
      targetRequired.push(field.name)
    }
  })
}

const buildInputSchema = () => {
  const properties = {}
  const required = []

  buildSchemaFromTree(schemaFields.value, properties, required)

  const schema = {
    type: 'object',
    properties
  }

  if (required.length > 0) {
    schema.required = required
  }

  return schema
}

/**
 * 从 Schema 中提取 header mappings
 */
const buildHeaderMappings = () => {
  const headerMappings = []

  const extractHeaders = (fields) => {
    if (!fields) return
    fields.forEach(field => {
      if (field.paramType === 'header') {
        headerMappings.push({
          name: field.headerName || field.name.toUpperCase().replace(/_/g, '-'),
          field: field.name,
          direction: 'request',
          required: field.required
        })
      }
      if (field.children && field.children.length > 0) {
        extractHeaders(field.children)
      }
    })
  }

  extractHeaders(schemaFields.value)
  return headerMappings
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitLoading.value = true

    // 构建 inputSchema（转为 JSON 字符串）
    const inputSchemaObj = buildInputSchema()

    // 构建 headerMappings（转为 JSON 字符串）
    const headerMappingsObj = buildHeaderMappings()

    // 创建提交数据，将对象转为 JSON 字符串
    const submitData = {
      ...formData,
      template: String(formData.template),
      inputSchema: JSON.stringify(inputSchemaObj),
      headerMappings: JSON.stringify(headerMappingsObj)
    }

    if (isEdit.value) {
      await resourceApi.update(route.params.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await resourceApi.create(submitData)
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
.resource-edit {
  height: 100%;
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-form) {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
