import request from '@/utils/request'

export type UploadCategory = 'avatar' | 'logo' | 'image' | 'resume'

/** 通用上传返回 /api/files/... URL；公开图片可直接访问，简历附件下载需 JWT 与归属授权。 */
export function uploadFile(file: File, category: UploadCategory): Promise<{ url: string }> {
  const form = new FormData()
  form.append('file', file)
  form.append('category', category)
  return request.post<{ url: string }>('/files/upload', form)
}
