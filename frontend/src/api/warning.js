import request from '../utils/request'

export function getWarningList(params) {
  return request.get('/warnings', { params })
}

export function getUnacknowledgedCount() {
  return request.get('/warnings/unacknowledged-count')
}

export function acknowledgeWarning(id, acknowledgedBy) {
  return request.put(`/warnings/${id}/acknowledge`, null, { params: { acknowledgedBy } })
}
