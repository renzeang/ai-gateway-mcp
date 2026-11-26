<template>
  <div class="schema-tree-editor">
    <div class="editor-header">
      <h4>字段配置</h4>
      <div class="header-actions">
        <el-button size="small" @click="expandAll">
          <el-icon><Plus /></el-icon>
          展开所有
        </el-button>
        <el-button size="small" @click="collapseAll">
          <el-icon><Minus /></el-icon>
          折叠所有
        </el-button>
        <el-button type="primary" size="small" @click="addRootField">
          <el-icon><Plus /></el-icon>
          添加根字段
        </el-button>
      </div>
    </div>

    <div v-if="fields.length === 0" class="empty-state">
      <el-empty description="暂无字段，请添加根字段" :image-size="80" />
    </div>

    <div v-else class="fields-list">
      <SchemaFieldNode
        v-for="(field, index) in fields"
        :key="field.id"
        :field="field"
        :depth="0"
        @remove="handleRemove"
        @update:field="handleUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus, Minus } from '@element-plus/icons-vue'
import SchemaFieldNode from './SchemaFieldNode.vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const fields = ref(props.modelValue || [])

// 监听外部变化
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    fields.value = newVal
  }
})

// 添加根字段
const addRootField = () => {
  fields.value.push({
    id: Date.now().toString(),
    name: '',
    type: 'string',
    paramType: 'json',
    headerName: '',
    description: '',
    required: false,
    children: []
  })
  emitUpdate()
}

// 删除字段
const handleRemove = (path) => {
  // 根字段删除
  const fieldName = path.split('.')[0]
  const index = fields.value.findIndex(f => f.name === fieldName || f.id === path)
  if (index !== -1) {
    fields.value.splice(index, 1)
    emitUpdate()
  }
}

// 更新字段
const handleUpdate = (field) => {
  emitUpdate()
}

// 发出更新事件
const emitUpdate = () => {
  emit('update:modelValue', fields.value)
}

// 展开所有
const expandAll = () => {
  // 通过自定义事件通知子组件
  window.dispatchEvent(new CustomEvent('schema-expand-all'))
}

// 折叠所有
const collapseAll = () => {
  window.dispatchEvent(new CustomEvent('schema-collapse-all'))
}
</script>

<style scoped>
.schema-tree-editor {
  width: 100%;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.editor-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 40px 0;
}

.fields-list {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  background-color: #fff;
}
</style>
