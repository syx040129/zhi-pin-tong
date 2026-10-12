/**
 * 全量接口类型（字段名与后端契约一一对应，均为 camelCase）。
 * 时间字段：LocalDateTime -> "yyyy-MM-dd HH:mm:ss"（部分含 'T'，格式化见 utils/format.ts）。
 */

/* ===== 通用 ===== */

export type UserRole = 'USER' | 'COMPANY' | 'ADMIN'
export type UserStatus = 'ENABLED' | 'DISABLED'
export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN'
export type JobStatus = 'ONLINE' | 'OFFLINE'
export type CompanyStatus = 'ACTIVE' | 'DISABLED'
export type ApplicationStatus = 'PENDING' | 'VIEWED' | 'INTERVIEW' | 'REJECTED' | 'HIRED'
export type ReportStatus = 'PENDING' | 'HANDLED' | 'IGNORED'
export type ReportTargetType = 'JOB' | 'COMPANY'
export type ExperienceType = 'EDUCATION' | 'WORK' | 'PROJECT' | 'INTERNSHIP'
export type InterviewStatus = 'ONGOING' | 'COMPLETED'
export type AiSource = 'LLM' | 'RULE' | 'FALLBACK'
export type AiMode = 'REAL' | 'MOCK' | 'DISABLED'

export interface Result<T> {
  code: number
  message: string
  data: T | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/* ===== 用户 / 认证 ===== */

export interface UserProfileVO {
  id: number
  username: string
  nickname: string
  avatar?: string | null
  phone?: string | null
  email?: string | null
  gender: Gender
  role: UserRole
  status: UserStatus
  createTime: string
  companyId?: number | null
  companyName?: string | null
}

export interface LoginResponse {
  token: string
  user: UserProfileVO
}

export interface ProfileUpdateRequest {
  nickname?: string
  phone?: string
  email?: string
  gender?: Gender
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
  role: Exclude<UserRole, 'ADMIN'>
}

/* ===== 岗位 ===== */

export interface JobSkill {
  id?: number
  skillName: string
  requiredLevel?: number | null
  weight?: number | null
}

export interface JobQuery {
  keyword?: string
  city?: string
  jobType?: string
  education?: string
  experience?: string
  salaryMin?: number
  salaryMax?: number
  sort?: 'default' | 'hot' | 'salary'
  page: number
  size: number
}

export interface JobVO {
  id: number
  companyId: number
  companyName: string
  companyLogo?: string | null
  companyIndustry?: string | null
  jobName: string
  jobType: string
  salaryMin?: number | null
  salaryMax?: number | null
  city: string
  education: string
  experience: string
  description: string
  requirements: string
  status: JobStatus
  viewCount: number
  createTime: string
  skills: JobSkill[]
  favorite?: boolean | null
  applicationStatus?: ApplicationStatus | null
  matchScore?: number | null
  totalApplications?: number | null
  pendingApplications?: number | null
}

export interface JobForm {
  jobName: string
  jobType: string
  salaryMin: number
  salaryMax: number
  city: string
  education: string
  experience: string
  description: string
  requirements: string
  skills?: JobSkill[]
}

/* ===== 简历 ===== */

export interface ExperienceForm {
  id?: number
  type: ExperienceType
  title: string
  company?: string
  content?: string
  startDate?: string
  endDate?: string
  sort?: number
}

export interface ResumeForm {
  name?: string
  phone?: string
  email?: string
  education?: string
  school?: string
  major?: string
  skills?: string[]
  summary?: string
  experiences?: ExperienceForm[]
}

export interface ResumeExperience {
  id: number
  resumeId: number
  type: ExperienceType
  title: string
  company?: string | null
  content?: string | null
  startDate?: string | null
  endDate?: string | null
  sort: number
}

export interface ResumeVO {
  id: number
  userId: number
  name?: string | null
  phone?: string | null
  email?: string | null
  education?: string | null
  school?: string | null
  major?: string | null
  skills?: string[] | null
  summary?: string | null
  resumeFileUrl?: string | null
  completionScore: number
  createTime: string
  updateTime: string
  experiences: ResumeExperience[]
}

export interface ResumeParseVO {
  resumeId: number
  fileName: string
  fileUrl: string
  text: string
  filled: boolean
  summary?: string | null
  name?: string | null
  phone?: string | null
  email?: string | null
  education?: string | null
  school?: string | null
  major?: string | null
  skills: string[]
  experiences: ExperienceForm[]
}

/* ===== 企业 ===== */

export interface CompanyForm {
  companyName: string
  industry?: string
  scale?: string
  address?: string
  description?: string
  website?: string
}

export interface CompanyVO {
  id: number
  userId: number
  companyName: string
  logo?: string | null
  industry?: string | null
  scale?: string | null
  address?: string | null
  description?: string | null
  website?: string | null
  status: CompanyStatus
  createTime: string
  jobCount?: number | null
  username?: string | null
  contactPhone?: string | null
}

/* ===== 投递 ===== */

export interface ApplicationForm {
  jobId: number
  resumeId?: number
  coverLetter?: string
}

export interface ApplicationStatusForm {
  status: Exclude<ApplicationStatus, 'PENDING'>
}

export interface ApplicationVO {
  id: number
  jobId: number
  jobName: string
  jobType: string
  city: string
  salaryMin?: number | null
  salaryMax?: number | null
  companyId: number
  companyName: string
  companyLogo?: string | null
  userId: number
  candidateName: string
  candidateAvatar?: string | null
  candidatePhone?: string | null
  candidateEmail?: string | null
  resumeId?: number | null
  resumeName?: string | null
  status: ApplicationStatus
  coverLetter?: string | null
  matchScore?: number | null
  createTime: string
  updateTime: string
}

/* ===== 收藏 ===== */

export interface FavoriteVO {
  id: number
  jobId: number
  jobName: string
  jobType: string
  city: string
  salaryMin?: number | null
  salaryMax?: number | null
  education: string
  experience: string
  jobStatus: JobStatus
  companyId: number
  companyName: string
  companyLogo?: string | null
  createTime: string
}

/* ===== 举报 ===== */

export interface ReportForm {
  targetType: ReportTargetType
  targetId: number
  reason: string
  detail?: string
}

export interface ReportVO {
  id: number
  userId: number
  reporterName: string
  targetType: ReportTargetType
  targetId: number
  targetName: string
  reason: string
  detail?: string | null
  status: ReportStatus
  handleRemark?: string | null
  createTime: string
}

/* ===== AI：简历分析 / 优化 ===== */

export interface AiAnalysisVO {
  id: number
  resumeId: number
  resumeName: string
  overallScore: number
  skillScore: number
  experienceScore: number
  educationScore: number
  completenessScore: number
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  targetJobs: string[]
  source: AiSource
  createTime: string
}

export interface OptimizeRequest {
  resumeId: number
  jobId: number
}

export interface OptimizeItemVO {
  field: string
  original: string
  optimized: string
  suggestion: string
  keywords: string[]
}

export interface OptimizeResultVO {
  resumeId: number
  jobId: number
  jobName: string
  summary: string
  items: OptimizeItemVO[]
}

/* ===== AI：岗位匹配 / 推荐 ===== */

export interface MatchRequest {
  jobId: number
  resumeId?: number
}

export interface MatchResultVO {
  jobId: number
  jobName: string
  companyName: string
  companyLogo?: string | null
  matchScore: number
  skillScore: number
  experienceScore: number
  educationScore: number
  semanticScore: number
  matchedSkills: string[]
  missingSkills: string[]
  reason: string
  source: AiSource
  semanticSource: 'EMBEDDING' | 'LOCAL' | 'FALLBACK'
  reasonSource: AiSource
  executionNote: string
}

export interface RecommendItemVO {
  jobId: number
  jobName: string
  jobType: string
  city: string
  salaryMin?: number | null
  salaryMax?: number | null
  education: string
  experience: string
  companyId: number
  companyName: string
  companyLogo?: string | null
  matchScore: number
  skillScore: number
  experienceScore: number
  educationScore?: number | null
  semanticScore?: number | null
  matchedSkills: string[]
  missingSkills: string[]
  reason: string
}

/* ===== AI：模拟面试 ===== */

export interface InterviewStartRequest {
  jobId?: number
  difficulty: '简单' | '中等' | '困难'
  questionCount?: number
  title?: string
}

export interface InterviewQuestionVO {
  id: number
  question: string
  questionType: string
  difficulty: string
  sort: number
  answer?: string | null
  score?: number | null
  feedback?: string | null
}

export interface InterviewVO {
  id: number
  jobId?: number | null
  jobName?: string | null
  title: string
  difficulty: '简单' | '中等' | '困难'
  questionCount: number
  totalScore?: number | null
  status: InterviewStatus
  createTime: string
  questions: InterviewQuestionVO[]
}

export interface InterviewAnswerRequest {
  questionId: number
  answer: string
}

export interface InterviewReportVO {
  interviewId: number
  title: string
  totalScore: number
  answeredCount: number
  questionCount: number
  dimensionScores: Record<string, number>
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
}

/* ===== 企业端：候选人 AI 分析 ===== */

export interface CompanyCandidateMatchVO {
  applicationId: number
  candidateUserId: number
  candidateName: string
  jobId: number
  jobName: string
  match: MatchResultVO | null
  strengths: string[]
  risks: string[]
  suggestions: string[]
  advice: string
}

export interface CompanyStats {
  jobCount: number
  onlineJobCount: number
  applicationCount: number
  pendingCount: number
  interviewCount: number
  hiredCount: number
  applicationStatus: { name: string; value: number }[]
}

/* ===== 管理后台 ===== */

export interface SysUserAdminVO {
  id: number
  username: string
  nickname: string
  avatar?: string | null
  phone?: string | null
  email?: string | null
  gender?: Gender | null
  role: UserRole
  status: UserStatus
  createTime: string
}

export interface AdminStatusRequest {
  status: string
}

export interface PasswordResetRequest {
  password?: string
}

export interface ResumeAdminVO {
  id: number
  userId: number
  userNickname: string
  userName: string
  phone?: string | null
  email?: string | null
  education?: string | null
  school?: string | null
  major?: string | null
  skills?: string | null
  completionScore?: number | null
  resumeFileUrl?: string | null
  createTime: string
  updateTime: string
}

export interface SysLog {
  id: number
  userId?: number | null
  username?: string | null
  type: 'LOGIN' | 'OPERATE' | 'AI' | 'ERROR'
  module?: string | null
  action?: string | null
  detail?: string | null
  ip?: string | null
  costMs?: number | null
  createTime: string
}

export interface AdminOverview {
  totalUsers: number
  totalCompanies: number
  totalJobs: number
  totalApplications: number
  totalResumes: number
  totalInterviews: number
  pendingReports: number
  jobTypeDistribution: { name: string; value: number }[]
  companyJobTop: { name: string; value: number }[]
}

export interface AdminTrends {
  users: { date: string; count: number }[]
  jobs: { date: string; count: number }[]
  applications: { date: string; count: number }[]
}

export interface AiConfig {
  mode: AiMode
  enabled: boolean
  mock: boolean
  baseUrl?: string | null
  model?: string | null
  embeddingModel?: string | null
  embeddingEnabled?: boolean
  hasApiKey: boolean
  recommendPoolSize: number
  recommendLimit: number
}

export interface AiTestResult {
  mode: AiMode
  ok: boolean
  message: string
  reply?: string | null
  latencyMs?: number | null
}

/* ===== 首页 / 健康检查 ===== */

export interface HomeStats {
  jobCount: number
  companyCount: number
  userCount: number
  applicationCount: number
}

export interface HealthInfo {
  status: 'UP' | 'DEGRADED'
  mysql: boolean
  redis: boolean
  ai: {
    mode: AiMode
    enabled: boolean
    mock: boolean
    hasApiKey: boolean
  }
}

/* ===== AI 智能助手（右下角悬浮对话框） ===== */

export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessageVO {
  id?: number
  sessionId?: number | null
  role: ChatRole
  content: string
  suggestions?: string[] | null
  source?: AiSource | null
  createTime?: string | null
}

export interface ChatSessionVO {
  id: number
  title: string
  messageCount: number
  lastMessage?: string | null
  createTime?: string | null
}

export interface ChatGreetingVO {
  greeting: string
  quickQuestions: string[]
  mode: AiMode
  loggedIn: boolean
}

export interface ChatHistoryVO {
  sessionId?: number | null
  title?: string | null
  messageCount?: number | null
  messages: ChatMessageVO[]
}

export interface ChatAskRequest {
  content: string
  sessionId?: number | null
  /** JOB / RESUME，配合 contextId 让助手感知当前浏览对象 */
  contextType?: string | null
  contextId?: number | null
}

export interface ChatAskVO {
  content: string
  sessionId?: number | null
  suggestions: string[]
  source: AiSource
  messageCount: number
  persisted: boolean
}
