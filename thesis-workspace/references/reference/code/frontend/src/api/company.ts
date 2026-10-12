import request from '@/utils/request'
import type {
  ApplicationStatus,
  ApplicationVO,
  CompanyCandidateMatchVO,
  CompanyForm,
  CompanyStats,
  CompanyVO,
  JobForm,
  JobStatus,
  JobVO,
  PageResult,
} from '@/types/api'

export interface CompanyJobQuery {
  keyword?: string
  status?: JobStatus
  page?: number
  size?: number
}

export interface CompanyApplicationQuery {
  jobId?: number
  status?: ApplicationStatus
  page?: number
  size?: number
}

/* ===== 企业资料 ===== */

export function getCompanyProfile(): Promise<CompanyVO> {
  return request.get<CompanyVO>('/company/profile')
}

export function updateCompanyProfile(form: CompanyForm): Promise<CompanyVO> {
  return request.put<CompanyVO>('/company/profile', form)
}

export function uploadCompanyLogo(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  return request.post<string>('/company/logo', form)
}

export function companyStats(): Promise<CompanyStats> {
  return request.get<CompanyStats>('/company/stats')
}

/** 公开企业信息（含在线岗位数） */
export function publicCompany(id: number): Promise<CompanyVO> {
  return request.get<CompanyVO>(`/companies/${id}`)
}

/* ===== 企业岗位管理 ===== */

export function companyJobs(params: CompanyJobQuery): Promise<PageResult<JobVO>> {
  return request.get<PageResult<JobVO>>('/company/jobs', { params })
}

export function createCompanyJob(form: JobForm): Promise<JobVO> {
  return request.post<JobVO>('/company/jobs', form)
}

export function updateCompanyJob(id: number, form: JobForm): Promise<JobVO> {
  return request.put<JobVO>(`/company/jobs/${id}`, form)
}

export function updateCompanyJobStatus(id: number, status: JobStatus): Promise<void> {
  return request.put(`/company/jobs/${id}/status`, { status })
}

export function deleteCompanyJob(id: number): Promise<void> {
  return request.delete(`/company/jobs/${id}`)
}

/* ===== 收到的投递 ===== */

export function companyApplications(
  params: CompanyApplicationQuery,
): Promise<PageResult<ApplicationVO>> {
  return request.get<PageResult<ApplicationVO>>('/company/applications', { params })
}

export function companyApplication(id: number): Promise<ApplicationVO> {
  return request.get<ApplicationVO>(`/company/applications/${id}`)
}

export function updateCompanyApplicationStatus(
  id: number,
  status: Exclude<ApplicationStatus, 'PENDING'>,
): Promise<void> {
  return request.put(`/company/applications/${id}/status`, { status })
}

export function companyCandidateAnalysis(
  applicationId: number,
): Promise<CompanyCandidateMatchVO> {
  return request.get<CompanyCandidateMatchVO>(
    `/company/candidates/${applicationId}/analysis`,
  )
}
