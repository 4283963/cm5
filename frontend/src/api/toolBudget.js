import request from '../utils/request'

export function getBudgetList(params) {
  return request.get('/tool-budgets', { params })
}

export function getBudgetById(id) {
  return request.get(`/tool-budgets/${id}`)
}

export function markBudgetUsed(id) {
  return request.put(`/tool-budgets/${id}/mark-used`)
}

export function cancelBudget(id, reason) {
  return request.put(`/tool-budgets/${id}/cancel`, null, { params: { reason } })
}
