import request from '../utils/request'

export function getToolList(params) {
  return request.get('/tools', { params })
}

export function getToolById(id) {
  return request.get(`/tools/${id}`)
}

export function getToolByRfid(rfidCode) {
  return request.get(`/tools/rfid/${rfidCode}`)
}

export function createTool(data) {
  return request.post('/tools', data)
}

export function updateTool(id, data) {
  return request.put(`/tools/${id}`, data)
}

export function deleteTool(id) {
  return request.delete(`/tools/${id}`)
}

export function processRfidScan(data) {
  return request.post('/tools/rfid-scan', data)
}

export function getHealthWarningTools() {
  return request.get('/tools/health-warning')
}

export function recalculateHealth(id) {
  return request.post(`/tools/${id}/recalculate-health`)
}

export function getDashboardStats() {
  return request.get('/tools/dashboard/stats')
}
