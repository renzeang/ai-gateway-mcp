<template>
  <div class="server-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Servers 管理</span>
          <div class="header-actions">
            <el-button type="success" @click="handleImport" data-testid="btn-import">
              <el-icon><Upload /></el-icon>
              导入 Tools/Resources
            </el-button>
            <el-button type="primary" @click="handleCreate" data-testid="btn-create">
              <el-icon><Plus /></el-icon>
              创建 Server
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
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" round>
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" :formatter="formatDateTime" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row.id)">编辑</el-button>
            <el-button link type="success" @click="handleExport(row)">导出</el-button>
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

    <!-- 导出弹窗 -->
    <ServerExportDialog ref="exportDialogRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElCheckbox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { serverApi } from '@/api'
import dayjs from 'dayjs'
import ServerExportDialog from './ServerExportDialog.vue'
import CapsuleSelect from '@/components/CapsuleSelect.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  enabled: null
})

const formatDateTime = (row, column, cellValue) => {
  return cellValue ? dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await serverApi.list(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取 Server 列表失败:', error)
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
  queryParams.enabled = null
  queryParams.page = 1
  fetchData()
}

const handleCreate = () => {
  router.push('/servers/create')
}

const handleEdit = (id) => {
  router.push(`/servers/${id}/edit`)
}

const handleDelete = async (id) => {
  try {
    // 先获取关联数据数量
    const relations = await serverApi.getRelations(id)
    const { toolCount, resourceCount, promptCount, total } = relations.data || relations

    if (total > 0) {
      // 有关联数据，显示带可选框的确认对话框
      let deleteCascade = false

      // 创建自定义内容
      const messageContent = h('div', { style: { display: 'flex', flexDirection: 'column', gap: '12px' } }, [
        h('div', { style: { color: '#333' } }, [
          h('span', {}, `该 Server 下存在关联数据：`),
          h('ul', { style: { margin: '8px 0', paddingLeft: '20px' } }, [
            h('li', {}, `Tools: ${toolCount} 个`),
            h('li', {}, `Resources: ${resourceCount} 个`),
            h('li', {}, `Prompts: ${promptCount} 个`)
          ])
        ]),
        h('label', { style: { display: 'flex', alignItems: 'center', gap: '8px', color: '#f56c6c', cursor: 'pointer' } }, [
          h('input', {
            type: 'checkbox',
            checked: deleteCascade,
            onChange: (e) => { deleteCascade = e.target.checked }
          }),
          h('span', {}, '同时删除其下所有 Tools/Resources/Prompts')
        ])
      ])

      try {
        await ElMessageBox.confirm(
          messageContent,
          '提示',
          {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning',
            showCancelButton: true,
            distinguishCancelAndClose: true
          }
        )
      } catch (action) {
        if (action === 'cancel' || action === 'close') {
          return // 用户取消或关闭对话框
        }
      }

      await serverApi.delete(id, deleteCascade)
    } else {
      // 没有关联数据，直接确认删除
      await ElMessageBox.confirm('确定要删除该 Server 吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await serverApi.delete(id, false)
    }

    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      // 不显示后端返回的具体错误信息，统一显示"删除失败"
      ElMessage.error('删除失败')
    }
  }
}

const handleImport = () => {
  router.push('/servers/import')
}

const exportDialogRef = ref(null)

const handleExport = (row) => {
  exportDialogRef.value?.show(row)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.server-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.el-pagination {
  display: flex;
}
</style>
