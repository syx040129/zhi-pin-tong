/**
 * 通用格式化工具与「状态 -> 中文文案/标签」映射。
 * 后端时间字段为 yyyy-MM-dd HH:mm:ss（含 'T' 分隔的 ISO-8601 变体），统一在此格式化。
 */

export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

export interface TagOption {
  label: string
  type: TagType
}

export function formatDateTime(value?: string | null): string {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}

export function formatDate(value?: string | null): string {
  if (!value) return ''
  return value.slice(0, 10)
}

/** 薪资区间：salaryMin=15, salaryMax=25 -> '15-25K'；缺失返回 '面议' */
export function salaryText(min?: number | null, max?: number | null): string {
  if (min && max) return `${min}-${max}K`
  if (min) return `${min}K起`
  if (max) return `${max}K以内`
  return '面议'
}

export function joinSkills(skills?: string[] | null, sep = '、'): string {
  if (!skills || skills.length === 0) return ''
  return skills.join(sep)
}

/** 根据映射取值，兜底展示 '-'，避免页面对未知名枚举抛错 */
export function tagOf(map: Record<string, TagOption>, value?: string | null): TagOption {
  if (value && map[value]) return map[value]
  return { label: value || '-', type: 'info' }
}

/* ===== 各状态映射（label + el-tag type） ===== */

export const roleMap: Record<string, TagOption> = {
  USER: { label: '求职者', type: 'primary' },
  COMPANY: { label: '企业', type: 'success' },
  ADMIN: { label: '管理员', type: 'danger' },
}

export const userStatusMap: Record<string, TagOption> = {
  ENABLED: { label: '正常', type: 'success' },
  DISABLED: { label: '已禁用', type: 'danger' },
}

export const genderMap: Record<string, TagOption> = {
  MALE: { label: '男', type: 'primary' },
  FEMALE: { label: '女', type: 'danger' },
  UNKNOWN: { label: '保密', type: 'info' },
}

export const jobStatusMap: Record<string, TagOption> = {
  ONLINE: { label: '招聘中', type: 'success' },
  OFFLINE: { label: '已下线', type: 'info' },
}

export const companyStatusMap: Record<string, TagOption> = {
  ACTIVE: { label: '营业中', type: 'success' },
  DISABLED: { label: '已禁用', type: 'danger' },
}

export const applicationStatusMap: Record<string, TagOption> = {
  PENDING: { label: '待处理', type: 'warning' },
  VIEWED: { label: '已查看', type: 'primary' },
  INTERVIEW: { label: '面试中', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  HIRED: { label: '已录用', type: 'success' },
}

export const reportTargetMap: Record<string, TagOption> = {
  JOB: { label: '岗位', type: 'primary' },
  COMPANY: { label: '企业', type: 'warning' },
}

export const reportStatusMap: Record<string, TagOption> = {
  PENDING: { label: '待处理', type: 'warning' },
  HANDLED: { label: '已处理', type: 'success' },
  IGNORED: { label: '已忽略', type: 'info' },
}

export const experienceTypeMap: Record<string, TagOption> = {
  EDUCATION: { label: '教育经历', type: 'primary' },
  WORK: { label: '工作经历', type: 'success' },
  PROJECT: { label: '项目经历', type: 'warning' },
  INTERNSHIP: { label: '实习经历', type: 'info' },
}

export const interviewStatusMap: Record<string, TagOption> = {
  ONGOING: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
}

export const interviewDifficultyMap: Record<string, TagOption> = {
  简单: { label: '简单', type: 'success' },
  中等: { label: '中等', type: 'warning' },
  困难: { label: '困难', type: 'danger' },
}

export const logTypeMap: Record<string, TagOption> = {
  LOGIN: { label: '登录', type: 'primary' },
  OPERATE: { label: '操作', type: 'success' },
  AI: { label: 'AI', type: 'warning' },
  ERROR: { label: '异常', type: 'danger' },
}
