<template>
  <div class="api-key-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>API Keys 管理</span>
          <el-button type="primary" @click="handleCreate" data-testid="btn-create-api-key">
            <el-icon><Plus /></el-icon>
            创建 API Key
          </el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="别名">
          <el-input
            v-model="queryParams.alias"
            placeholder="请输入别名"
            clearable
            @keyup.enter="handleSearch"
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
        <el-table-column prop="alias" label="别名" min-width="150" />
        <el-table-column prop="keyPrefix" label="Key 前缀" width="160" />
        <el-table-column label="Server 作用域" min-width="200">
          <template #default="{ row }">
            <span v-if="!row.serverNames || row.serverNames.length === 0">
              <el-tag type="success" size="small">全部</el-tag>
            </span>
            <span v-else>
              <el-tag
                v-for="(name, i) in row.serverNames"
                :key="i"
                size="small"
                style="margin-right: 4px"
              >
                {{ name }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="true"
              :inactive-value="false"
              @change="handleToggle(row)"
              :data-testid="row.enabled ? 'btn-deactivate-key' : 'btn-activate-key'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="过期时间" width="180">
          <template #default="{ row }">
            <span v-if="row.expiresAt">{{ formatDateTime(row.expiresAt) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastUsedAt" label="最后使用" width="180">
          <template #default="{ row }">
            <span v-if="row.lastUsedAt">{{ formatDateTime(row.lastUsedAt) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            <span v-if="row.createdAt">{{ formatDateTime(row.createdAt) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row.id)" data-testid="btn-delete-key">
              删除
            </el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { apiKeyApi } from '@/api'
import dayjs from 'dayjs'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  alias: '',
  enabled: null
})

const formatDateTime = (val) => {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await apiKeyApi.list(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取 API Key 列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.alias = ''
  queryParams.enabled = null
  queryParams.page = 1
  fetchData()
}

const handleCreate = () => {
  router.push('/api-keys/create')
}

const handleToggle = async (row) => {
  try {
    await apiKeyApi.toggle(row.id, { enabled: row.enabled })
    ElMessage.success(row.enabled ? '已启用' : '已停用')
  } catch (error) {
    row.enabled = !row.enabled
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该 API Key 吗？删除后关联的 SSE 连接将被断开。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiKeyApi.delete(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.api-key-list {
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
</style>
