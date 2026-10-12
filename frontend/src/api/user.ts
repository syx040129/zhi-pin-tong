import request from '@/utils/request'
import type { ProfileUpdateRequest, UserProfileVO } from '@/types/api'

export function getProfile(): Promise<UserProfileVO> {
  return request.get<UserProfileVO>('/user/profile')
}

export function updateProfile(payload: ProfileUpdateRequest): Promise<UserProfileVO> {
  return request.put<UserProfileVO>('/user/profile', payload)
}

export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return request.put('/user/password', { oldPassword, newPassword })
}

/** 上传头像（multipart），返回新头像 URL */
export function uploadAvatar(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  return request.post<string>('/user/avatar', form)
}
