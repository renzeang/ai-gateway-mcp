<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`导出 Server: ${serverName}`"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="export-options">
      <el-form>
        <!-- 全选所有 -->
        <el-form-item>
          <el-checkbox
            v-model="selectAll"
            :indeterminate="isIndeterminate"
            @change="handleSelectAllChange"
          >
            全选所有（{{ selectedCount }} 项已选择）
          </el-checkbox>
        </el-form-item>

        <!-- Tools 分类 -->
        <el-divider content-position="left">Tools</el-divider>
        <el-form-item>
          <el-checkbox
            v-model="selectAllTools"
            :indeterminate="isToolsIndeterminate"
            @change="handleSelectAllToolsChange"
          >
            全选 Tools（{{ toolsSelectedCount }}/{{ toolsList.length }}）
          </el-checkbox>
        </el-form-item>
        <div class="item-list">
          <div v-for="tool in toolsList" :key="tool.id" class="item-row">
            <el-checkbox v-model="tool.selected">
              {{ tool.name }} - {{ tool.method }} {{ tool.path }}
            </el-checkbox>
          </div>
          <div v-if="toolsList.length === 0" class="empty-text">无 Tools</div>
        </div>

        <!-- Resources 分类 -->
        <el-divider content-position="left">Resources</el-divider>
        <el-form-item>
          <el-checkbox
            v-model="selectAllResources"
            :indeterminate="isResourcesIndeterminate"
            @change="handleSelectAllResourcesChange"
          >
            全选 Resources（{{ resourcesSelectedCount }}/{{ resourcesList.length }}）
          </el-checkbox>
        </el-form-item>
        <div class="item-list">
          <div v-for="resource in resourcesList" :key="resource.id" class="item-row">
            <el-checkbox v-model="resource.selected">
              {{ resource.name }} - GET {{ resource.path }}
            </el-checkbox>
          </div>
          <div v-if="resourcesList.length === 0" class="empty-text">无 Resources</div>
        </div>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleExport" :disabled="selectedCount === 0">
        导出
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { serverApi } from '@/api'

const dialogVisible = ref(false)
const serverId = ref(null)
const serverName = ref('')

const toolsList = ref([])
const resourcesList = ref([])

const selectAll = ref(false)
const selectAllTools = ref(false)
const selectAllResources = ref(false)

// 计算选中数量
const toolsSelectedCount = computed(() => toolsList.value.filter(t => t.selected).length)
const resourcesSelectedCount = computed(() => resourcesList.value.filter(r => r.selected).length)
const selectedCount = computed(() => toolsSelectedCount.value + resourcesSelectedCount.value)

// 半选状态
const isIndeterminate = computed(() => {
  const total = toolsList.value.length + resourcesList.value.length
  return selectedCount.value > 0 && selectedCount.value < total
})

const isToolsIndeterminate = computed(() => {
  return toolsSelectedCount.value > 0 && toolsSelectedCount.value < toolsList.value.length
})

const isResourcesIndeterminate = computed(() => {
  return resourcesSelectedCount.value > 0 && resourcesSelectedCount.value < resourcesList.value.length
})

// 全选所有
const handleSelectAllChange = (val) => {
  selectAllTools.value = val
  selectAllResources.value = val
  toolsList.value.forEach(t => t.selected = val)
  resourcesList.value.forEach(r => r.selected = val)
}

// 全选 Tools
const handleSelectAllToolsChange = (val) => {
  toolsList.value.forEach(t => t.selected = val)
}

// 全选 Resources
const handleSelectAllResourcesChange = (val) => {
  resourcesList.value.forEach(r => r.selected = val)
}

// 监听选择变化，更新全选状态
watch([toolsList, resourcesList], () => {
  const total = toolsList.value.length + resourcesList.value.length
  selectAll.value = selectedCount.value === total && total > 0
  selectAllTools.value = toolsSelectedCount.value === toolsList.value.length && toolsList.value.length > 0
  selectAllResources.value = resourcesSelectedCount.value === resourcesList.value.length && resourcesList.value.length > 0
}, { deep: true })

// 加载 Server 下的 Tools 和 Resources
const loadServerItems = async (id) => {
  try {
    // 这里需要调用 API 获取 Server 下的 Tools 和 Resources
    // 暂时使用占位数据，后续需要实现对应的 API
    const [toolsRes, resourcesRes] = await Promise.all([
      fetch(`/api/v1/admin/tools?serverId=${id}&pageSize=1000`).then(r => r.json()),
      fetch(`/api/v1/admin/resources?serverId=${id}&pageSize=1000`).then(r => r.json())
    ])

    toolsList.value = (toolsRes.data?.records || []).map(t => ({
      id: t.id,
      name: t.name,
      method: t.method,
      path: t.path,
      selected: true // 默认全选
    }))

    resourcesList.value = (resourcesRes.data?.records || []).map(r => ({
      id: r.id,
      name: r.uri || r.name,
      path: r.path,
      selected: true // 默认全选
    }))
  } catch (error) {
    console.error('加载 Server 项目失败:', error)
    ElMessage.error('加载 Server 项目失败')
  }
}

// 导出
const handleExport = async () => {
  const toolIds = toolsList.value.filter(t => t.selected).map(t => t.id)
  const resourceIds = resourcesList.value.filter(r => r.selected).map(r => r.id)

  try {
    const response = await fetch(`/api/v1/admin/servers/${serverId.value}/export`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        toolIds,
        resourceIds
      })
    })

    if (!response.ok) {
      throw new Error('导出失败')
    }

    // 处理文件下载
    const blob = await response.blob()
    const disposition = response.headers.get('Content-Disposition')
    let filename = `server-export-${serverName.value}-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.json`

    if (disposition) {
      const matches = /filename\*=UTF-8''(.+)/.exec(disposition)
      if (matches && matches[1]) {
        filename = decodeURIComponent(matches[1])
      }
    }

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
    dialogVisible.value = false
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败：' + error.message)
  }
}

// 显示弹窗
const show = async (server) => {
  serverId.value = server.id
  serverName.value = server.name
  dialogVisible.value = true

  // 重置选择状态
  toolsList.value = []
  resourcesList.value = []
  selectAll.value = false
  selectAllTools.value = false
  selectAllResources.value = false

  // 加载数据
  await loadServerItems(server.id)
}

// 暴露方法
defineExpose({ show })
</script>

<style scoped>
.export-options {
  max-height: 400px;
  overflow-y: auto;
}

.item-list {
  margin-left: 20px;
}

.item-row {
  margin-bottom: 8px;
}

.empty-text {
  color: #999;
  font-size: 12px;
  text-align: center;
  padding: 10px;
}

:deep(.el-divider__text) {
  font-weight: bold;
}
</style>
