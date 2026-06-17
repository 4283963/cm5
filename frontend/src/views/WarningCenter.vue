<template>
  <div class="warning-center">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>预警中心</span>
          <el-tag type="danger" v-if="unackCount > 0" effect="dark">
            {{ unackCount }} 条未处理预警
          </el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.isAcknowledged" placeholder="全部" clearable style="width: 140px">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="searchForm.warningLevel" placeholder="全部" clearable style="width: 140px">
            <el-option label="紧急" value="URGENT" />
            <el-option label="警告" value="WARNING" />
            <el-option label="提示" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警类型">
          <el-select v-model="searchForm.warningType" placeholder="全部" clearable style="width: 140px">
            <el-option label="健康度低" value="HEALTH_LOW" />
            <el-option label="磨损超标" value="WEAR_EXCEED" />
            <el-option label="工时超标" value="HOURS_EXCEED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%"
        :row-class-name="tableRowClassName">
        <el-table-column prop="rfidCode" label="RFID编码" width="150" />
        <el-table-column prop="warningType" label="预警类型" width="120">
          <template #default="{ row }">
            {{ warningTypeLabels[row.warningType] || row.warningType }}
          </template>
        </el-table-column>
        <el-table-column prop="warningLevel" label="预警级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.warningLevel)" size="small" effect="dark">
              {{ levelLabels[row.warningLevel] || row.warningLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthScore" label="健康度" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.healthScore < 10 ? '#f56c6c' : row.healthScore < 20 ? '#e6a23c' : '#67c23a', fontWeight: 700 }">
              {{ row.healthScore }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="预警信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="isAcknowledged" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAcknowledged === 1 ? 'success' : 'danger'" size="small">
              {{ row.isAcknowledged === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="预警时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isAcknowledged === 0"
              type="primary" link size="small"
              @click="handleAcknowledge(row)">
              确认处理
            </el-button>
            <span v-else class="ack-info">{{ row.acknowledgedBy }}</span>
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
import { getWarningList, getUnacknowledgedCount, acknowledgeWarning } from '../api/warning'

const loading = ref(false)
const unackCount = ref(0)
const tableData = ref([])

const warningTypeLabels = { HEALTH_LOW: '健康度低', WEAR_EXCEED: '磨损超标', HOURS_EXCEED: '工时超标' }
const levelLabels = { URGENT: '紧急', WARNING: '警告', INFO: '提示' }

const searchForm = reactive({ isAcknowledged: undefined, warningLevel: '', warningType: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

function levelTagType(level) {
  const map = { URGENT: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}

function tableRowClassName({ row }) {
  if (row.isAcknowledged === 0 && row.warningLevel === 'URGENT') return 'urgent-row'
  return ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWarningList({
      isAcknowledged: searchForm.isAcknowledged,
      warningLevel: searchForm.warningLevel || undefined,
      warningType: searchForm.warningType || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function loadUnackCount() {
  try {
    const res = await getUnacknowledgedCount()
    unackCount.value = res.data.count
  } catch (e) { /* ignore */ }
}

async function handleAcknowledge(row) {
  try {
    await acknowledgeWarning(row.id, 'ADMIN')
    ElMessage.success('已确认处理')
    loadData()
    loadUnackCount()
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function resetSearch() {
  searchForm.isAcknowledged = undefined
  searchForm.warningLevel = ''
  searchForm.warningType = ''
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadData()
  loadUnackCount()
})
</script>

<style scoped>
.warning-center {
  padding: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}

.ack-info {
  font-size: 12px;
  color: #909399;
}
</style>

<style>
.el-table .urgent-row {
  background-color: #fef0f0 !important;
}
</style>
