<template>
  <div class="scan-records">
    <el-card shadow="hover">
      <template #header>
        <span>RFID扫描记录</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="RFID编码">
          <el-input v-model="searchForm.rfidCode" placeholder="RFID编码" clearable />
        </el-form-item>
        <el-form-item label="扫描类型">
          <el-select v-model="searchForm.scanType" placeholder="全部" clearable style="width: 140px">
            <el-option label="巡检" value="INSPECT" />
            <el-option label="更换" value="REPLACE" />
            <el-option label="报废" value="SCRAP" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="rfidCode" label="RFID编码" width="150" />
        <el-table-column prop="scanType" label="扫描类型" width="100">
          <template #default="{ row }">
            <el-tag :type="scanTypeTag(row.scanType)" size="small">
              {{ scanTypeLabels[row.scanType] || row.scanType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作员" width="100" />
        <el-table-column prop="workstationId" label="工位" width="100" />
        <el-table-column prop="gatewayId" label="网关" width="100" />
        <el-table-column prop="cuttingHours" label="切削工时(h)" width="120" />
        <el-table-column prop="wearValue" label="磨损量(mm)" width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="scanTime" label="扫描时间" width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
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
import { getScanRecordList } from '../api/scanRecord'

const loading = ref(false)
const tableData = ref([])
const dateRange = ref(null)

const scanTypeLabels = { INSPECT: '巡检', REPLACE: '更换', SCRAP: '报废' }

const searchForm = reactive({ rfidCode: '', scanType: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

function scanTypeTag(type) {
  const map = { INSPECT: 'primary', REPLACE: 'warning', SCRAP: 'danger' }
  return map[type] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      rfidCode: searchForm.rfidCode || undefined,
      scanType: searchForm.scanType || undefined,
      startTime: dateRange.value ? dateRange.value[0] : undefined,
      endTime: dateRange.value ? dateRange.value[1] : undefined,
      page: pagination.page,
      size: pagination.size,
    }
    const res = await getScanRecordList(params)
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
  searchForm.rfidCode = ''
  searchForm.scanType = ''
  dateRange.value = null
  pagination.page = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.scan-records {
  padding: 4px;
}

.search-form {
  margin-bottom: 16px;
}
</style>
