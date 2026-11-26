<template>
  <div class="resource-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Resources 管理</span>
          <div>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              创建 Resource
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
        <el-form-item label="方法">
          <CapsuleSelect
            v-model="queryParams.method"
            :options="[
              { label: 'GET', value: 'GET' },
              { label: 'POST', value: 'POST' },
              { label: 'PUT', value: 'PUT' },
              { label: 'DELETE', value: 'DELETE' }
            ]"
            placeholder="全部"
            clearable
            :width="'150px'"
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
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="uri" label="URI" min-width="150" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="method" label="方法" width="100">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.method)" round>{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serverName" label="关联服务" min-width="120" />
        <el-table-column prop="path" label="路径" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" round>
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
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
import { Plus } from '@element-plus/icons-vue'
import { resourceApi, serverApi } from '@/api'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const servers = ref([])

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  method: '',
  serverId: '',
  enabled: null
})

const getMethodType = (method) => {
  const types = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return types[method] || 'info'
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
    const res = await resourceApi.list(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取 Resource 列表失败:', error)
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
  queryParams.method = ''
  queryParams.serverId = ''
  queryParams.enabled = null
  queryParams.page = 1
  fetchData()
}

const handleCreate = () => {
  router.push('/resources/create')
}

const handleEdit = (id) => {
  router.push(`/resources/${id}/edit`)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该 Resource 吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resourceApi.delete(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  fetchServers()
  fetchData()
})
</script>

<style scoped>
.resource-list {
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
