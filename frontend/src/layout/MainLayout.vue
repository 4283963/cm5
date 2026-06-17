<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo-section">
        <el-icon :size="28" color="#409EFF"><SetUp /></el-icon>
        <span class="logo-text">刀具生命周期管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1d1e2c"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>系统总览</span>
        </el-menu-item>
        <el-menu-item index="/tools">
          <el-icon><SetUp /></el-icon>
          <span>刀具管理</span>
        </el-menu-item>
        <el-menu-item index="/warnings">
          <el-icon><Warning /></el-icon>
          <span>预警中心</span>
          <el-badge v-if="warningCount > 0" :value="warningCount" :max="99" class="menu-badge" />
        </el-menu-item>
        <el-menu-item index="/procurement">
          <el-icon><ShoppingCart /></el-icon>
          <span>采购申请</span>
        </el-menu-item>
        <el-menu-item index="/scan-records">
          <el-icon><Barcode /></el-icon>
          <span>扫描记录</span>
        </el-menu-item>
        <el-menu-item index="/trade-in-vouchers">
          <el-icon><Present /></el-icon>
          <span>以旧换新</span>
        </el-menu-item>
        <el-menu-item index="/tool-budgets">
          <el-icon><Wallet /></el-icon>
          <span>预算管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag v-if="wsConnected" type="success" size="small" effect="dark">
            <el-icon><Connection /></el-icon> 网关在线
          </el-tag>
          <el-tag v-else type="info" size="small" effect="dark">
            <el-icon><Connection /></el-icon> 网关离线
          </el-tag>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUnacknowledgedCount } from '../api/warning'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '刀具生命周期管理系统')
const warningCount = ref(0)
const wsConnected = ref(false)

let stompClient = null

async function fetchWarningCount() {
  try {
    const res = await getUnacknowledgedCount()
    warningCount.value = res.data.count
  } catch (e) { /* ignore */ }
}

function connectWebSocket() {
  try {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 10000,
      onConnect: () => {
        wsConnected.value = true
        stompClient.subscribe('/topic/warnings', (message) => {
          const data = JSON.parse(message.body)
          warningCount.value += 1
          ElMessage({
            type: data.needsReplacement ? 'error' : 'warning',
            message: data.message,
            duration: 8000,
            showClose: true,
          })
        })
      },
      onDisconnect: () => {
        wsConnected.value = false
      },
      onStompError: () => {
        wsConnected.value = false
      },
    })
    stompClient.activate()
  } catch (e) {
    wsConnected.value = false
  }
}

onMounted(() => {
  fetchWarningCount()
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #1d1e2c;
  overflow-y: auto;
}

.logo-section {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #333;
}

.logo-text {
  color: #e0e0e0;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-main {
  background: #f0f2f5;
  overflow-y: auto;
}

.menu-badge {
  margin-left: 8px;
}

.el-menu {
  border-right: none;
}
</style>
