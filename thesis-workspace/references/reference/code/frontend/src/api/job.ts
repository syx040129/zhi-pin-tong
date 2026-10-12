import request from '@/utils/request'
import type { JobQuery, JobVO, PageResult } from '@/types/api'

export function pageJobs(params: Partial<JobQuery>): Promise<PageResult<JobVO>> {
  return request.get<PageResult<JobVO>>('/jobs/page', { params })
}

export function getJob(id: number): Promise<JobVO> {
  return request.get<JobVO>(`/jobs/${id}`)
}
