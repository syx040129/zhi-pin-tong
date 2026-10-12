import request from '@/utils/request'
import type {
  AdminOverview,
  AdminStatusRequest,
  AdminTrends,
  AiConfig,
  AiTestResult,
  ApplicationStatus,
  ApplicationVO,
  CompanyStatus,
  CompanyVO,
  JobStatus,
  JobVO,
  PageResult,
  PasswordResetRequest,
  ReportStatus,
  ReportVO,
  ResumeAdminVO,
  SysLog,
  SysUserAdminVO,
  UserRole,
  UserStatus,
} from '@/types/api'

/* ===== 用户管理 ===== */

export interface AdminUserQuery {
  keyword?: string
  role?: UserRole
  status?: UserStatus
  page?: number
  size?: number
}

export function adminUsers(params: AdminUserQuery): Promise<PageResult<SysUserAdminVO>> {
  return request.get<PageResult<SysUserAdminVO>>('/admin/users', { params })
}

export function adminUserStatus(id: number, status: UserStatus): Promise<void> {
  return request.put(`/admin/users/${id}/status`, { status } satisfies AdminStatusRequest)
}

/** 重置密码；payload 缺省/为空时后端重置为 123456 */
export function adminResetPassword(
  id: number,
  payload?: PasswordResetRequest,
): Promise<void> {
  return request.put(`/admin/users/${id}/password`, payload ?? {})
}

export function adminDeleteUser(id: number): Promise<void> {
  return request.delete(`/admin/users/${id}`)
}

/* ===== 数据统计 ===== */

export function adminOverview(): Promise<AdminOverview> {
  return request.get<AdminOverview>('/admin/stats/overview')
}

export function adminTrends(days = 30): Promise<AdminTrends> {
  return request.get<AdminTrends>('/admin/stats/trends', { params: { days } })
}

/* ===== 简历管理 ===== */

export interface AdminResumeQuery {
  keyword?: string
  page?: number
  size?: number
}

export function adminResumes(params: AdminResumeQuery): Promise<PageResult<ResumeAdminVO>> {
  return request.get<PageResult<ResumeAdminVO>>('/admin/resumes', { params })
}

export function adminDeleteResume(id: number): Promise<void> {
  return request.delete(`/admin/resumes/${id}`)
}

/* ===== 举报管理 ===== */

export interface AdminReportQuery {
  status?: ReportStatus
  page?: number
  size?: number
}

export function adminReports(params: AdminReportQuery): Promise<PageResult<ReportVO>> {
  return request.get<PageResult<ReportVO>>('/admin/reports', { params })
}

export function adminReportStatus(
  id: number,
  status: ReportStatus,
  handleRemark?: string,
): Promise<void> {
  return request.put(`/admin/reports/${id}/status`, { status, handleRemark })
}

export function adminDeleteReport(id: number): Promise<void> {
  return request.delete(`/admin/reports/${id}`)
}

/* ===== 系统日志 ===== */

export interface AdminLogQuery {
  type?: 'LOGIN' | 'OPERATE' | 'AI' | 'ERROR'
  page?: number
  size?: number
}

export function adminLogs(params: AdminLogQuery): Promise<PageResult<SysLog>> {
  return request.get<PageResult<SysLog>>('/admin/logs', { params })
}

/* ===== 岗位管理 ===== */

export interface AdminJobQuery {
  keyword?: string
  jobType?: string
  status?: JobStatus
  page?: number
  size?: number
}

export function adminJobs(params: AdminJobQuery): Promise<PageResult<JobVO>> {
  return request.get<PageResult<JobVO>>('/admin/jobs', { params })
}

export function adminJobStatus(id: number, status: JobStatus): Promise<void> {
  return request.put(`/admin/jobs/${id}/status`, { status } satisfies AdminStatusRequest)
}

export function adminDeleteJob(id: number): Promise<void> {
  return request.delete(`/admin/jobs/${id}`)
}

/* ===== 企业管理 ===== */

export interface AdminCompanyQuery {
  keyword?: string
  status?: CompanyStatus
  page?: number
  size?: number
}

export function adminCompanies(params: AdminCompanyQuery): Promise<PageResult<CompanyVO>> {
  return request.get<PageResult<CompanyVO>>('/admin/companies', { params })
}

export function adminCompanyStatus(id: number, status: CompanyStatus): Promise<void> {
  return request.put(`/admin/companies/${id}/status`, { status } satisfies AdminStatusRequest)
}

export function adminDeleteCompany(id: number): Promise<void> {
  return request.delete(`/admin/companies/${id}`)
}

/* ===== 投递管理 ===== */

export interface AdminApplicationQuery {
  keyword?: string
  status?: ApplicationStatus
  page?: number
  size?: number
}

export function adminApplications(
  params: AdminApplicationQuery,
): Promise<PageResult<ApplicationVO>> {
  return request.get<PageResult<ApplicationVO>>('/admin/applications', { params })
}

export function adminDeleteApplication(id: number): Promise<void> {
  return request.delete(`/admin/applications/${id}`)
}

/* ===== AI 配置 ===== */

export function adminAiConfig(): Promise<AiConfig> {
  return request.get<AiConfig>('/admin/ai/config')
}

export function adminAiTest(): Promise<AiTestResult> {
  return request.post<AiTestResult>('/admin/ai/test')
}
