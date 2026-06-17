<template>
  <div class="budget-list">
    <el-card shadow="hover">
      <template #header>
        <span>刀具预算单</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="searchForm.budgetStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="已使用" value="USED" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="预算单号/刀具编码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="budgetNo" label="预算单号" width="200" />
        <el-table-column prop="sourceType" label="来源类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.sourceType === 'TRADE_IN'" type="warning" size="small">以旧换新</el-tag>
            <span v-else>{{ row.sourceType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="toolCode" label="刀具编码" width="120" />
        <el-table-column prop="toolName" label="刀具名称" width="130" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="subsidyAmount" label="以旧换新补贴" width="120">
          <template #default="{ row }">
            <span style="color: #67c23a; font-weight: 600">-¥{{ Number(row.subsidyAmount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualPayment" label="实付金额" width="110">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600">¥{{ Number(row.actualPayment).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="budgetStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.budgetStatus)" size="small" effect="dark">
              {{ statusLabels[row.budgetStatus] || row.budgetStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="有效期至" width="170" />
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.budgetStatus === 'ACTIVE'"
              type="success" link size="small"
              @click="handleMarkUsed(row)">
              核销
            </el-button>
            <el-button
              v-if="row.budgetStatus === 'ACTIVE'"
              type="danger" link size="small"
              @click="handleCancel(row)">
              取消
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgetList, markBudgetUsed, cancelBudget } from '../api/toolBudget'

const loading = ref(false)
const tableData = ref([])

const statusLabels = {
  ACTIVE: '有效', USED: '已使用', EXPIRED: '已过期', CANCELLED: '已取消'
}

const searchForm = reactive({ budgetStatus: '', keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

function statusTagType(status) {
  const map = { ACTIVE: 'success', USED: 'info', EXPIRED: 'warning', CANCELLED: 'danger' }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getBudgetList({
      budgetStatus: searchForm.budgetStatus || undefined,
      keyword: searchForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function resetSearch() {
  searchForm.budgetStatus = ''
  searchForm.keyword = ''
  pagination.page = 1
  loadData()
}

async function handleMarkUsed(row) {
  try {
    await ElMessageBox.confirm(`确认核销预算单"${row.budgetNo}"？`, '核销确认', { type: 'warning' })
    await markBudgetUsed(row.id)
    ElMessage.success('核销成功')
    loadData()
  } catch (e) { /* ignore */ }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(`确认取消预算单"${row.budgetNo}"？`, '取消确认', { type: 'warning' })
    await cancelBudget(row.id, '手动取消')
    ElMessage.success('已取消')
    loadData()
  } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.budget-list {
  padding: 4px;
}

.search-form {
  margin-bottom: 16px;
}
</style>
