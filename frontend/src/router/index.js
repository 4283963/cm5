import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '系统总览' }
      },
      {
        path: 'tools',
        name: 'ToolList',
        component: () => import('../views/ToolList.vue'),
        meta: { title: '刀具管理' }
      },
      {
        path: 'warnings',
        name: 'WarningCenter',
        component: () => import('../views/WarningCenter.vue'),
        meta: { title: '预警中心' }
      },
      {
        path: 'procurement',
        name: 'ProcurementList',
        component: () => import('../views/ProcurementList.vue'),
        meta: { title: '采购申请' }
      },
      {
        path: 'scan-records',
        name: 'ScanRecords',
        component: () => import('../views/ScanRecords.vue'),
        meta: { title: '扫描记录' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
