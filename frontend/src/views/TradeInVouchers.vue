<template>
  <div class="voucher-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>以旧换新折价凭证</span>
          <el-tag v-if="pendingCount > 0" type="warning" effect="dark">
            {{ pendingCount }} 条待审批
          </el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="searchForm.voucherStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="凭证号/刀具编码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetSearch"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="voucherNo" label="凭证编号" width="200" />
        <el-table-column prop="rfidCode" label="RFID编码" width="140" />
        <el-table-column prop="toolName" label="刀具名称" width="130" />
        <el-table-column prop="originalPrice" label="采购原价" width="110">
          <template #default="{ row }">¥{{ Number(row.originalPrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="threeMonthOutput" label="三月产值" width="110">
          <template #default="{ row }">¥{{ Number(row.threeMonthOutput).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="healthScore" label="健康度" width="100">
          <template #default="{ row }">{{ Number(row.healthScore).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="residualValue" label="残余价值" width="110">
          <template #default="{ row }">
            <span class="residual-value">¥{{ Number(row.residualValue).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="discountRate" label="折价率" width="90">
          <template #default="{ row }">{{ (Number(row.discountRate) * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="voucherStatus" label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.voucherStatus)" size="small" effect="dark">
              {{ statusLabels[row.voucherStatus] || row.voucherStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="supplierStatus" label="供应商状态" width="110">
          <template #default="{ row }">
            <el-tag :type="supplierTagType(row.supplierStatus)" size="small">
              {{ supplierLabels[row.supplierStatus] || row.supplierStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.voucherStatus === 'PENDING'"
              type="success" link size="small"
              @click="handleApprove(row)">
              通过
            </el-button>
            <el-button
              v-if="row.voucherStatus === 'PENDING'"
              type="danger" link size="small"
              @click="handleReject(row)">
              驳回
            </el-button>
            <el-button
              v-if="row.voucherStatus === 'APPROVED' && row.supplierStatus === 'FAILED'"
              type="warning" link size="small"
              @click="handleRetrySupplier(row)">
              重推供应商
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

    <el-dialog v-model="detailVisible" title="以旧换新折价凭证" width="640px" destroy-on-close>
      <div v-if="currentVoucher" class="voucher-detail">
        <div class="voucher-header">
          <div class="voucher-no">{{ currentVoucher.voucherNo }}</div>
          <el-tag :type="statusTagType(currentVoucher.voucherStatus)" effect="dark" size="large">
            {{ statusLabels[currentVoucher.voucherStatus] }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border size="default" class="voucher-desc">
          <el-descriptions-item label="RFID编码">{{ currentVoucher.rfidCode }}</el-descriptions-item>
          <el-descriptions-item label="刀具编码">{{ currentVoucher.toolCode }}</el-descriptions-item>
          <el-descriptions-item label="刀具名称">{{ currentVoucher.toolName }}</el-descriptions-item>
          <el-descriptions-item label="规格型号">{{ currentVoucher.specification }}</el-descriptions-item>
          <el-descriptions-item label="采购原价">¥{{ Number(currentVoucher.originalPrice).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="累计切削工时">{{ currentVoucher.totalCuttingHours }} h</el-descriptions-item>
          <el-descriptions-item label="近三月产值">¥{{ Number(currentVoucher.threeMonthOutput).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="报废时健康度">{{ Number(currentVoucher.healthScore).toFixed(1) }}%</el-descriptions-item>
        </el-descriptions>

        <div class="voucher-value">
          <div class="value-item">
            <span class="value-label">折价率</span>
            <span class="value-num">{{ (Number(currentVoucher.discountRate) * 100).toFixed(1) }}%</span>
          </div>
          <div class="value-arrow">➜</div>
          <div class="value-item main">
            <span class="value-label">残余资产价值（以旧换新抵用）</span>
            <span class="value-num big">¥{{ Number(currentVoucher.residualValue).toFixed(2) }}</span>
          </div>
        </div>

        <div class="voucher-footer">
          <div v-if="currentVoucher.approveBy">
            <span class="muted">审批人：</span>{{ currentVoucher.approveBy }}
            <span class="muted" style="margin-left: 16px;">审批时间：</span>{{ currentVoucher.approveTime }}
          </div>
          <div v-if="currentVoucher.rejectReason" class="reject-reason">
            <el-tag type="danger" size="small">驳回原因</el-tag>
            <span>{{ currentVoucher.rejectReason }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div v-if="currentVoucher && currentVoucher.voucherStatus === 'PENDING'" class="dialog-footer">
          <el-button type="danger" @click="handleReject(currentVoucher)">驳回</el-button>
          <el-button type="primary" @click="handleApprove(currentVoucher)">审批通过</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回凭证" width="480px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item label="审批人">
          <el-input v-model="rejectForm.approver" placeholder="请输入审批人姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="submitLoading">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVoucherList, approveVoucher, rejectVoucher, retrySupplierSync
} from '../api/tradeInVoucher'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const pendingCount = ref(0)
const detailVisible = ref(false)
const rejectVisible = ref(false)
const currentVoucher = ref(null)

const statusLabels = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
const supplierLabels = { PENDING: '待推送', SUBMITTED: '已推送', CONFIRMED: '已确认', FAILED: '失败' }

const searchForm = reactive({ voucherStatus: '', keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const rejectForm = reactive({ reason: '', approver: '' })

function statusTagType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function supplierTagType(status) {
  const map = { PENDING: 'info', SUBMITTED: 'primary', CONFIRMED: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getVoucherList({
      voucherStatus: searchForm.voucherStatus || undefined,
      keyword: searchForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function loadPendingCount() {
  try {
    const res = await getVoucherList({ voucherStatus: 'PENDING', page: 1, size: 1 })
    pendingCount.value = res.data.total
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function resetSearch() {
  searchForm.voucherStatus = ''
  searchForm.keyword = ''
  pagination.page = 1
  loadData()
}

function handleView(row) {
  currentVoucher.value = row
  detailVisible.value = true
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认审批通过凭证"${row.voucherNo}"？审批通过后将自动推送供应商系统并创建预算单。`,
      '审批确认', { type: 'warning' })
    const approver = 'ADMIN'
    await approveVoucher(row.id, approver)
    ElMessage.success('审批通过，已推送供应商并生成预算单')
    loadData()
    loadPendingCount()
    if (detailVisible.value) detailVisible.value = false
  } catch (e) { /* ignore */ }
}

function handleReject(row) {
  currentVoucher.value = row
  rejectForm.reason = ''
  rejectForm.approver = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  if (!rejectForm.approver.trim()) {
    ElMessage.warning('请输入审批人')
    return
  }
  submitLoading.value = true
  try {
    await rejectVoucher(currentVoucher.value.id, rejectForm.reason, rejectForm.approver)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
    loadPendingCount()
  } catch (e) { /* ignore */ }
  submitLoading.value = false
}

async function handleRetrySupplier(row) {
  try {
    await retrySupplierSync(row.id)
    ElMessage.success('已重新推送供应商系统')
    loadData()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadData()
  loadPendingCount()
})
</script>

<style scoped>
.voucher-list {
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

.residual-value {
  color: #e6a23c;
  font-weight: 600;
}

.voucher-detail {
  padding: 8px 0;
}

.voucher-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.voucher-no {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.voucher-desc {
  margin-bottom: 20px;
}

.voucher-value {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.value-item {
  text-align: center;
}

.value-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.value-num {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.value-num.big {
  font-size: 32px;
  color: #e6a23c;
}

.value-arrow {
  font-size: 24px;
  color: #e6a23c;
}

.value-item.main {
  flex: 1;
}

.voucher-footer {
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  font-size: 13px;
  color: #606266;
}

.muted {
  color: #909399;
}

.reject-reason {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-footer {
  text-align: right;
}
</style>
