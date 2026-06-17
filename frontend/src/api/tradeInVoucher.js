import request from '../utils/request'

export function getVoucherList(params) {
  return request.get('/trade-in-vouchers', { params })
}

export function getVoucherById(id) {
  return request.get(`/trade-in-vouchers/${id}`)
}

export function approveVoucher(id, approver) {
  return request.put(`/trade-in-vouchers/${id}/approve`, null, { params: { approver } })
}

export function rejectVoucher(id, rejectReason, approver) {
  return request.put(`/trade-in-vouchers/${id}/reject`, null, { params: { rejectReason, approver } })
}

export function retrySupplierSync(id) {
  return request.post(`/trade-in-vouchers/${id}/retry-supplier`)
}
