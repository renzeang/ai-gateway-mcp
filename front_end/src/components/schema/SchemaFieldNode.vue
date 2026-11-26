<template>
  <div class="schema-field-node" :style="{ marginLeft: depth * 20 + 'px' }">
    <el-card :class="['field-card', { 'is-header': isHeader, 'is-query': isQuery }]">
      <template #header>
        <div class="card-header">
          <span class="field-path">{{ path }}</span>
          <el-tag :type="field.type" size="small" class="field-type" round>{{ field.type }}</el-tag>
          <el-tag v-if="isPath" type="danger" size="small" class="param-type-tag" round>Path</el-tag>
          <el-tag v-if="isHeader" type="warning" size="small" class="param-type-tag" round>Header</el-tag>
          <el-tag v-if="isQuery" type="info" size="small" class="param-type-tag" round>Query</el-tag>
          <el-tag v-if="isJson" type="success" size="small" class="param-type-tag" round>JSON</el-tag>
          <div class="actions">
            <el-button v-if="canExpand" link @click="collapsed = !collapsed">
              <el-icon><component :is="collapsed ? 'Plus' : 'Minus'" /></el-icon>
            </el-button>
            <el-button link type="danger" @click="$emit('remove', path)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="field" label-width="100px" size="default">
        <!-- 字段名 -->
        <el-form-item label="字段名">
          <el-input v-model="field.name" placeholder="字段名" :disabled="depth > 0" />
        </el-form-item>

        <!-- 类型选择 -->
        <el-form-item label="类型">
          <el-select v-model="field.type" placeholder="选择类型" @change="onTypeChange">
            <el-option label="string" value="string" />
            <el-option label="number" value="number" />
            <el-option label="boolean" value="boolean" />
            <el-option label="object" value="object" :disabled="field.paramType === 'query' || field.paramType === 'header'" />
            <el-option label="array" value="array" :disabled="field.paramType === 'header'" />
          </el-select>
        </el-form-item>

        <!-- 入参方式 -->
        <el-form-item label="入参方式">
          <el-radio-group v-model="field.paramType" @change="onParamTypeChange">
            <el-radio label="path">Path Variable</el-radio>
            <el-radio label="json">JSON Body</el-radio>
            <el-radio label="query" :disabled="!canBeQuery">QueryParams</el-radio>
            <el-radio label="header" :disabled="!canBeHeader">Header</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- Header 名称（仅当 paramType=header 时显示） -->
        <el-form-item v-if="field.paramType === 'header'" label="Header 名">
          <el-input v-model="field.headerName" :placeholder="defaultHeaderName" />
        </el-form-item>

        <!-- 描述 -->
        <el-form-item label="描述">
          <el-input v-model="field.description" type="textarea" :rows="2" placeholder="字段描述" />
        </el-form-item>

        <!-- 必填 -->
        <el-form-item label="必填">
          <el-checkbox v-model="field.required" />
        </el-form-item>
      </el-form>

      <!-- 子字段配置（object/array 类型且未折叠时显示） -->
      <div v-if="canExpand && !collapsed" class="children-fields">
        <el-divider content-position="left">
          <span>{{ field.type === 'array' ? '数组元素字段' : '子字段' }}</span>
        </el-divider>

        <div v-for="(child, index) in field.children" :key="child.id" class="child-wrapper">
          <SchemaFieldNode
            :field="child"
            :depth="depth + 1"
            :parent-path="path"
            @remove="handleChildRemove"
          />
        </div>

        <el-button type="primary" plain @click="addChild" class="add-child-btn">
          <el-icon><Plus /></el-icon>
          添加{{ field.type === 'array' ? '元素字段' : '子字段' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Minus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  field: {
    type: Object,
    required: true,
    default: () => ({
      id: '',
      name: '',
      type: 'string',
      paramType: 'json',
      headerName: '',
      description: '',
      required: false,
      children: []
    })
  },
  depth: {
    type: Number,
    default: 0
  },
  parentPath: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['remove', 'update:field'])

const collapsed = ref(false)
const field = props.field

// 生成字段路径
const path = computed(() => {
  if (props.depth === 0) {
    return field.name || '新字段'
  }
  return props.parentPath ? `${props.parentPath}.${field.name || '新字段'}` : field.name || '新字段'
})

// 是否可以展开/折叠
const canExpand = computed(() => {
  return field.type === 'object' || field.type === 'array'
})

// 是否可以是 Query 参数（基础类型 + 数组）
const canBeQuery = computed(() => {
  return ['string', 'number', 'boolean', 'array'].includes(field.type)
})

// 是否可以是 Header 参数（只有基础类型）
const canBeHeader = computed(() => {
  return ['string', 'number', 'boolean'].includes(field.type)
})

// 判断当前入参方式
const isPath = computed(() => field.paramType === 'path')
const isHeader = computed(() => field.paramType === 'header')
const isQuery = computed(() => field.paramType === 'query')
const isJson = computed(() => field.paramType === 'json' || !field.paramType)

// 默认 Header 名
const defaultHeaderName = computed(() => {
  if (!field.name) return ''
  return field.name.toUpperCase().replace(/_/g, '-')
})

// 类型变化时的处理
const onTypeChange = (newType) => {
  // 如果是 array 类型，初始化 items 结构
  if (newType === 'array' && !field.children) {
    field.children = []
  }
}

// 入参方式变化时的处理
const onParamTypeChange = () => {
  // 无特殊处理
}

// 添加子字段
const addChild = () => {
  if (!field.children) {
    field.children = []
  }
  field.children.push({
    id: Date.now().toString(),
    name: '',
    type: 'string',
    paramType: 'json',
    headerName: '',
    description: '',
    required: false,
    children: []
  })
}

// 处理子字段删除
const handleChildRemove = (childPath) => {
  if (!field.children) return
  const index = field.children.findIndex(c => {
    const childFullPath = props.parentPath ? `${props.parentPath}.${field.name}.${c.name}` : `${field.name}.${c.name}`
    return childFullPath === childPath || c.id === childPath.split('.').pop()
  })
  if (index !== -1) {
    field.children.splice(index, 1)
  }
}

// 监听字段名变化，更新路径
watch(() => field.name, () => {
  emit('update:field', field)
})
</script>

<style scoped>
.schema-field-node {
  margin-bottom: 12px;
}

.field-card {
  border-left: 4px solid #409EFF;
}

.field-card.is-header {
  border-left-color: #E6A23C;
}

.field-card.is-query {
  border-left-color: #909399;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.field-path {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.field-type {
  text-transform: uppercase;
  font-size: 12px;
}

.param-type-tag {
  margin-left: 4px;
}

.actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.children-fields {
  margin-top: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.child-wrapper {
  margin-bottom: 12px;
}

.add-child-btn {
  margin-top: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-form-item__label) {
  font-size: 13px;
}
</style>
