import request from '../utils/request'

export function getScanRecordList(params) {
  return request.get('/scan-records', { params })
}
