<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-total">
          <div class="stat-number">{{ stats.total || 0 }}</div>
          <div class="stat-label">刀具总数</div>
          <el-icon class="stat-icon"><SetUp /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-inuse">
          <div class="stat-number">{{ stats.inUse || 0 }}</div>
          <div class="stat-label">在用</div>
          <el-icon class="stat-icon"><CircleCheck /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-pending">
          <div class="stat-number">{{ stats.pendingReplace || 0 }}</div>
          <div class="stat-label">待更换</div>
          <el-icon class="stat-icon"><Warning /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-scrapped">
          <div class="stat-number">{{ stats.scrapped || 0 }}</div>
          <div class="stat-label">已报废</div>
          <el-icon class="stat-icon"><Delete /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>健康度预警刀具</span>
          </template>
          <el-table :data="warningTools" stripe style="width: 100%" max-height="400">
            <el-table-column prop="rfidCode" label="RFID编码" width="160" />
            <el-table-column prop="toolName" label="刀具名称" width="140" />
            <el-table-column prop="machineName" label="所在机床" width="140" />
            <el-table-column prop="healthScore" label="健康度" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="Number(row.healthScore)"
                  :color="getHealthColor(row.healthScore)"
                  :stroke-width="16"
                  :text-inside="true"
                />
              </template>
            </el-table-column>
            <el-table-column prop="totalCuttingHours" label="切削工时(h)" width="120" />
            <el-table-column prop="accumulatedWear" label="累计磨损(mm)" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" style="margin-bottom: 20px;">
          <template #header>
            <span>RFID扫描模拟</span>
          </template>
          <el-form :model="scanForm" label-width="100px" size="default">
            <el-form-item label="RFID编码">
              <el-input v-model="scanForm.rfidCode" placeholder="扫描或输入RFID" />
            </el-form-item>
            <el-form-item label="扫描类型">
              <el-select v-model="scanForm.scanType" style="width: 100%">
                <el-option label="巡检" value="INSPECT" />
                <el-option label="更换" value="REPLACE" />
                <el-option label="报废" value="SCRAP" />
              </el-select>
            </el-form-item>
            <el-form-item label="切削工时">
              <el-input-number v-model="scanForm.cuttingHours" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="磨损量">
              <el-input-number v-model="scanForm.wearValue" :min="0" :precision="4" :step="0.001" style="width: 100%" />
            </el-form-item>
            <el-form-item label="操作员">
              <el-input v-model="scanForm.operatorName" placeholder="操作员姓名" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitScan" :loading="scanLoading" style="width: 100%">
                <el-icon><Barcode /></el-icon> 提交扫描
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <template #header>
            <span>健康度分布</span>
          </template>
          <div class="health-legend">
            <div class="legend-item">
              <span class="dot" style="background: #67c23a;"></span>
              <span>健康 (80-100%)</span>
            </div>
            <div class="legend-item">
              <span class="dot" style="background: #e6a23c;"></span>
              <span>注意 (20-80%)</span>
            </div>
            <div class="legend-item">
              <span class="dot" style="background: #f56c6c;"></span>
              <span>预警 (<20%)</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardStats, getHealthWarningTools, processRfidScan } from '../api/tool'

const stats = ref({})
const warningTools = ref([])
const scanLoading = ref(false)

const scanForm = reactive({
  rfidCode: '',
  scanType: 'INSPECT',
  cuttingHours: 0,
  wearValue: 0,
  operatorName: '',
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
  try {
    const [statsRes, warningRes] = await Promise.all([
      getDashboardStats(),
      getHealthWarningTools()
    ])
    stats.value = statsRes.data
    warningTools.value = warningRes.data
  } catch (e) { /* ignore */ }
}

async function submitScan() {
  if (!scanForm.rfidCode) {
    ElMessage.warning('请输入RFID编码')
    return
  }
  scanLoading.value = true
  try {
    const res = await processRfidScan(scanForm)
    const result = res.data
    const score = Number(result.currentHealthScore).toFixed(1)
    if (result.needsReplacement) {
      ElMessage.error(`刀具健康度: ${score}%，需立即更换！已自动提报采购申请。`)
    } else if (result.needsWarning) {
      ElMessage.warning(`刀具健康度: ${score}%，低于预警阈值，已发出预警！`)
    } else {
      ElMessage.success(`扫描成功，健康度: ${score}%`)
    }
    loadData()
  } catch (e) { /* ignore */ }
  scanLoading.value = false
}

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  padding: 4px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 48px;
  opacity: 0.15;
}

.stat-total .stat-number { color: #409EFF; }
.stat-inuse .stat-number { color: #67c23a; }
.stat-pending .stat-number { color: #e6a23c; }
.stat-scrapped .stat-number { color: #909399; }

.health-legend {
  padding: 8px 0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
  color: #606266;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}
</style>
