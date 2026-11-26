<template>
  <div class="audit-log-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计日志</span>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="目标名称">
          <el-input
            v-model="queryParams.targetName"
            placeholder="请输入目标名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="请求类型">
          <CapsuleSelect
            v-model="queryParams.requestType"
            :options="[
              { label: 'tool', value: 'tool' },
              { label: 'resource', value: 'resource' },
              { label: 'prompt', value: 'prompt' }
            ]"
            placeholder="全部"
            clearable
          />
        </el-form-item>
        <el-form-item label="是否错误">
          <CapsuleSelect
            v-model="queryParams.isError"
            :options="[
              { label: '是', value: true },
              { label: '否', value: false }
            ]"
            placeholder="全部"
            clearable
          />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
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
        <el-table-column prop="requestType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag round>{{ row.requestType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标名称" min-width="150" />
        <el-table-column prop="apiKeyAlias" label="API Key" min-width="120">
          <template #default="{ row }">
            {{ row.apiKeyAlias || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="userToken" label="User Token" width="150">
          <template #default="{ row }">
            {{ maskToken(row.userToken) }}
          </template>
        </el-table-column>
        <el-table-column prop="responseStatus" label="响应状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.responseStatus === 200 ? 'success' : 'warning'" round>
              {{ row.responseStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isError" label="是否错误" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isError ? 'danger' : 'success'" round>
              {{ row.isError ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时 (ms)" width="100" />
        <el-table-column prop="createdAt" label="时间" width="180" :formatter="formatDateTime" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row.id)">详情</el-button>
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
import { auditLogApi } from '@/api'
import dayjs from 'dayjs'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  targetName: '',
  requestType: '',
  isError: null,
  startDate: '',
  endDate: ''
})

const maskToken = (token) => {
  if (!token) return ''
  if (token.length <= 8) return '***'
  return token.substring(0, 4) + '***' + token.substring(token.length - 4)
}

const formatDateTime = (row, column, cellValue) => {
  return cellValue ? dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await auditLogApi.list(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取审计日志失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.targetName = ''
  queryParams.requestType = ''
  queryParams.isError = null
  queryParams.startDate = ''
  queryParams.endDate = ''
  queryParams.page = 1
  dateRange.value = []
  fetchData()
}

const handleDateChange = (value) => {
  if (value && value.length === 2) {
    queryParams.startDate = value[0]
    queryParams.endDate = value[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

const handleViewDetail = (id) => {
  router.push(`/audit-logs/${id}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.audit-log-list {
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
