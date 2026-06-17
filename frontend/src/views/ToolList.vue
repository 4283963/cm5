<template>
  <div class="tool-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>刀具管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="showAddDialog">
              <el-icon><Plus /></el-icon> 新增刀具
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="名称/编码/RFID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="在用" value="IN_USE" />
            <el-option label="待更换" value="PENDING_REPLACE" />
            <el-option label="已报废" value="SCRAPPED" />
            <el-option label="已替换" value="REPLACED" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.toolType" placeholder="全部" clearable style="width: 140px">
            <el-option label="车削刀片" value="TURNING" />
            <el-option label="铣削刀片" value="MILLING" />
            <el-option label="钻削刀片" value="DRILLING" />
            <el-option label="镗削刀片" value="BORING" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="rfidCode" label="RFID编码" width="150" />
        <el-table-column prop="toolCode" label="刀具编码" width="120" />
        <el-table-column prop="toolName" label="刀具名称" width="130" />
        <el-table-column prop="toolType" label="类型" width="100">
          <template #default="{ row }">
            {{ typeLabels[row.toolType] || row.toolType }}
          </template>
        </el-table-column>
        <el-table-column prop="machineName" label="所在机床" width="120" />
        <el-table-column prop="healthScore" label="健康度" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.healthScore)"
              :color="getHealthColor(row.healthScore)"
              :stroke-width="14"
              :text-inside="true"
            />
          </template>
        </el-table-column>
        <el-table-column prop="totalCuttingHours" label="切削工时(h)" width="110" />
        <el-table-column prop="accumulatedWear" label="磨损量(mm)" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleRecalculate(row)">重算健康度</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑刀具' : '新增刀具'" width="600px" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="RFID编码" required>
          <el-input v-model="formData.rfidCode" :disabled="isEdit" placeholder="RFID电子标签编码" />
        </el-form-item>
        <el-form-item label="刀具编码" required>
          <el-input v-model="formData.toolCode" placeholder="刀具物料编码" />
        </el-form-item>
        <el-form-item label="刀具名称" required>
          <el-input v-model="formData.toolName" placeholder="如：CNMG120408-PM" />
        </el-form-item>
        <el-form-item label="刀具类型">
          <el-select v-model="formData.toolType" style="width: 100%">
            <el-option label="车削刀片" value="TURNING" />
            <el-option label="铣削刀片" value="MILLING" />
            <el-option label="钻削刀片" value="DRILLING" />
            <el-option label="镗削刀片" value="BORING" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="formData.specification" placeholder="规格参数" />
        </el-form-item>
        <el-form-item label="机床编号">
          <el-input v-model="formData.machineId" placeholder="如：CNC-001" />
        </el-form-item>
        <el-form-item label="机床名称">
          <el-input v-model="formData.machineName" placeholder="如：DMG MORI NLX 2500" />
        </el-form-item>
        <el-form-item label="最大切削工时">
          <el-input-number v-model="formData.maxCuttingHours" :min="1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大磨损限量">
          <el-input-number v-model="formData.maxWearLimit" :min="0.001" :precision="4" :step="0.01" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToolList, createTool, updateTool, deleteTool, recalculateHealth } from '../api/tool'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const editId = ref(null)

const typeLabels = { TURNING: '车削刀片', MILLING: '铣削刀片', DRILLING: '钻削刀片', BORING: '镗削刀片' }

const searchForm = reactive({ keyword: '', status: '', toolType: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formData = reactive({
  rfidCode: '', toolCode: '', toolName: '', toolType: 'TURNING',
  specification: '', machineId: '', machineName: '',
  maxCuttingHours: 500, maxWearLimit: 0.5,
})

function getHealthColor(score) {
  const s = Number(score)
  if (s >= 80) return '#67c23a'
  if (s >= 20) return '#e6a23c'
  return '#f56c6c'
}

function getStatusType(status) {
  const map = { IN_USE: 'success', PENDING_REPLACE: 'danger', SCRAPPED: 'info', REPLACED: 'warning' }
  return map[status] || 'info'
}

function getStatusLabel(status) {
  const map = { IN_USE: '在用', PENDING_REPLACE: '待更换', SCRAPPED: '已报废', REPLACED: '已替换' }
  return map[status] || status
}

async function loadData() {
  loading.value = true
  try {
    const res = await getToolList({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined,
      toolType: searchForm.toolType || undefined,
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
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.toolType = ''
  pagination.page = 1
  loadData()
}

function showAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    rfidCode: '', toolCode: '', toolName: '', toolType: 'TURNING',
    specification: '', machineId: '', machineName: '',
    maxCuttingHours: 500, maxWearLimit: 0.5,
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(formData, {
    rfidCode: row.rfidCode, toolCode: row.toolCode, toolName: row.toolName,
    toolType: row.toolType, specification: row.specification,
    machineId: row.machineId, machineName: row.machineName,
    maxCuttingHours: row.maxCuttingHours, maxWearLimit: row.maxWearLimit,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateTool(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createTool(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* ignore */ }
  submitLoading.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除刀具"${row.toolName}"？`, '删除确认', { type: 'warning' })
    await deleteTool(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* ignore */ }
}

async function handleRecalculate(row) {
  try {
    await recalculateHealth(row.id)
    ElMessage.success('健康度重算完成')
    loadData()
  } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.tool-list {
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
</style>
