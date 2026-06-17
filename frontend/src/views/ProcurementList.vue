<template>
  <div class="procurement-list">
    <el-card shadow="hover">
      <template #header>
        <span>采购申请</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="ERP状态">
          <el-select v-model="searchForm.erpStatus" placeholder="全部" clearable style="width: 160px">
            <el-option label="待提交" value="PENDING" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已审批" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="提交失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="申请号/刀具编码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="requestNo" label="申请编号" width="200" />
        <el-table-column prop="rfidCode" label="RFID编码" width="150" />
        <el-table-column prop="toolCode" label="刀具编码" width="120" />
        <el-table-column prop="toolName" label="刀具名称" width="130" />
        <el-table-column prop="specification" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="reason" label="采购原因" min-width="260" show-overflow-tooltip />
        <el-table-column prop="erpStatus" label="ERP状态" width="110">
          <template #default="{ row }">
            <el-tag :type="erpStatusType(row.erpStatus)" size="small" effect="dark">
              {{ erpStatusLabels[row.erpStatus] || row.erpStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestedTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.erpStatus === 'FAILED'"
              type="warning" link size="small"
              @click="handleRetry(row)">
              重新提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProcurementList, retryErpSubmission } from '../api/procurement'

const loading = ref(false)
const tableData = ref([])

const erpStatusLabels = {
  PENDING: '待提交', SUBMITTED: '已提交', APPROVED: '已审批',
  REJECTED: '已拒绝', FAILED: '提交失败'
}

const searchForm = reactive({ erpStatus: '', keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

function erpStatusType(status) {
  const map = { PENDING: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', FAILED: 'danger' }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProcurementList({
      erpStatus: searchForm.erpStatus || undefined,
      keyword: searchForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function handleRetry(row) {
  try {
    await retryErpSubmission(row.id)
    ElMessage.success('已重新提交至ERP')
    loadData()
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function resetSearch() {
  searchForm.erpStatus = ''
  searchForm.keyword = ''
  pagination.page = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.procurement-list {
  padding: 4px;
}

.search-form {
  margin-bottom: 16px;
}
</style>
