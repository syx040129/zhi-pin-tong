import request from '@/utils/request'
import type { PageResult, ReportForm, ReportStatus, ReportVO } from '@/types/api'

export function submitReport(form: ReportForm): Promise<void> {
  return request.post('/reports', form)
}

export function myReports(params: {
  status?: ReportStatus
  page?: number
  size?: number
}): Promise<PageResult<ReportVO>> {
  return request.get<PageResult<ReportVO>>('/reports/my', { params })
}
