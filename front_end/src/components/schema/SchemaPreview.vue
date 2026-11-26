<template>
  <div class="schema-preview">
    <div class="preview-header">
      <h4>JSON Schema 预览</h4>
      <el-button size="small" @click="copyToClipboard">
        <el-icon><DocumentCopy /></el-icon>
        复制
      </el-button>
    </div>
    <pre class="preview-content"><code>{{ formattedSchema }}</code></pre>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  fields: {
    type: Array,
    required: true,
    default: () => []
  }
})

// 将字段树转换为标准 JSON Schema 格式
const buildSchema = () => {
  const schema = {
    type: 'object',
    properties: {},
    required: []
  }

  const processField = (field, targetProperties, targetRequired) => {
    if (!field.name) return

    const propSchema = {
      type: field.type
    }

    // 添加描述
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

    // 添加类别标记
    if (field.category) {
      propSchema['x-category'] = true
    }

    // 处理嵌套对象
    if (field.type === 'object' && field.children && field.children.length > 0) {
      propSchema.properties = {}
      propSchema.required = []
      field.children.forEach(child => {
        processField(child, propSchema.properties, propSchema.required)
      })
      // 清理空的 required 数组
      if (propSchema.required.length === 0) {
        delete propSchema.required
      }
    }

    // 处理数组
    if (field.type === 'array') {
      if (field.children && field.children.length > 0) {
        // 数组元素是对象
        const itemSchema = {
          type: 'object',
          properties: {},
          required: []
        }
        field.children.forEach(child => {
          processField(child, itemSchema.properties, itemSchema.required)
        })
        if (itemSchema.required.length === 0) {
          delete itemSchema.required
        }
        propSchema.items = itemSchema
      } else {
        // 简单类型数组
        propSchema.items = { type: 'string' }
      }
    }

    targetProperties[field.name] = propSchema

    // 必填字段
    if (field.required) {
      targetRequired.push(field.name)
    }
  }

  // 处理所有根字段
  props.fields.forEach(field => {
    processField(field, schema.properties, schema.required)
  })

  // 清理空的 required 数组
  if (schema.required.length === 0) {
    delete schema.required
  }

  return schema
}

const schema = computed(() => buildSchema())

const formattedSchema = computed(() => {
  try {
    return JSON.stringify(schema.value, null, 2)
  } catch (e) {
    return 'Error formatting schema'
  }
})

const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(formattedSchema.value)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.schema-preview {
  width: 100%;
  margin-top: 16px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.preview-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.preview-content {
  background-color: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.preview-content code {
  white-space: pre;
}
</style>
