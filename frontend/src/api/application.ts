import request from '@/utils/request'
import type { ApplicationForm, ApplicationStatus, ApplicationVO, PageResult } from '@/types/api'

export function apply(form: ApplicationForm): Promise<ApplicationVO> {
  return request.post<ApplicationVO>('/applications', form)
}

export function myApplications(params: {
  status?: ApplicationStatus
  page?: number
  size?: number
}): Promise<PageResult<ApplicationVO>> {
  return request.get<PageResult<ApplicationVO>>('/applications', { params })
}

export function myApplication(id: number): Promise<ApplicationVO> {
  return request.get<ApplicationVO>(`/applications/${id}`)
}

export function withdrawApplication(id: number): Promise<void> {
  return request.delete(`/applications/${id}`)
}
