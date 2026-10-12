import request from '@/utils/request'
import type { ResumeForm, ResumeParseVO, ResumeVO } from '@/types/api'

export function listResumes(): Promise<ResumeVO[]> {
  return request.get<ResumeVO[]>('/resume/list')
}

export function getResume(id: number): Promise<ResumeVO> {
  return request.get<ResumeVO>(`/resume/${id}`)
}

export function createResume(form: ResumeForm): Promise<ResumeVO> {
  return request.post<ResumeVO>('/resume', form)
}

export function updateResume(id: number, form: ResumeForm): Promise<ResumeVO> {
  return request.put<ResumeVO>(`/resume/${id}`, form)
}

export function deleteResume(id: number): Promise<void> {
  return request.delete(`/resume/${id}`)
}

/** 上传简历附件（pdf/docx/doc/txt，≤20MB），返回更新后的简历 */
export function uploadResumeFile(id: number, file: File): Promise<ResumeVO> {
  const form = new FormData()
  form.append('file', file)
  return request.post<ResumeVO>(`/resume/${id}/file`, form)
}

/** 触发 AI/规则解析简历文件，返回解析结果 */
export function parseResume(id: number): Promise<ResumeParseVO> {
  return request.post<ResumeParseVO>(`/resume/${id}/parse`)
}
