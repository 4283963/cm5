import request from '../utils/request'

export function getProcurementList(params) {
  return request.get('/procurement', { params })
}

export function retryErpSubmission(id) {
  return request.post(`/procurement/${id}/retry`)
}
