<template>
  <div class="prompt-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Prompts 管理</span>
          <div>
            <el-button @click="handleImport">
              <el-icon><Upload /></el-icon>
              导入
            </el-button>
            <el-button @click="handleExport" :disabled="selectedPrompts.length === 0">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              创建 Prompt
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="名称">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="服务">
          <CapsuleSelect
            v-model="queryParams.serverId"
            :options="serverOptions"
            placeholder="全部"
            clearable
            :width="'250px'"
            filterable
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <CapsuleSelect
            v-model="queryParams.enabled"
            :options="[
              { label: '启用', value: true },
              { label: '禁用', value: false }
            ]"
            placeholder="全部"
            clearable
            :width="'120px'"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="serverName" label="关联服务" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" round>
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" :formatter="formatDateTime" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row.id)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download } from '@element-plus/icons-vue'
import { promptApi, serverApi } from '@/api'
import dayjs from 'dayjs'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const servers = ref([])
const selectedPrompts = ref([])

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  serverId: '',
  enabled: null
})

const formatDateTime = (row, column, cellValue) => {
  return cellValue ? dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-'
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

// 服务下拉选项（带 URL）
const serverOptions = computed(() => servers.value)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await promptApi.list(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取 Prompt 列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.name = ''
  queryParams.serverId = ''
  queryParams.enabled = null
  queryParams.page = 1
  fetchData()
}

const handleSelectionChange = (selection) => {
  selectedPrompts.value = selection.map(item => item.id)
}

const handleCreate = () => {
  router.push('/prompts/create')
}

const handleEdit = (id) => {
  router.push(`/prompts/${id}/edit`)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该 Prompt 吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await promptApi.delete(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleImport = () => {
  router.push('/prompts/import')
}

const handleExport = async () => {
  try {
    const res = await promptApi.export({ ids: selectedPrompts.value })

    // 从响应头获取文件名
    const contentDisposition = res.headers['content-disposition']
    let fileName = 'prompts-export.json'
    if (contentDisposition) {
      // 解析 filename*=UTF-8''encoded 格式
      const match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
      if (match) {
        fileName = decodeURIComponent(match[1])
      }
    }

    const blob = new Blob([res.data], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功：' + fileName)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败：' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  fetchServers()
  fetchData()
})
</script>

<style scoped>
.prompt-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.el-pagination {
  display: flex;
}
</style>
