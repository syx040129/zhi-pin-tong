import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'

export async function downloadResumeFile(url: string): Promise<void> {
  if (!url.startsWith('/api/files/resume/')) return
  try {
    const base = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')
    const response = await axios.get(base + url.slice(4), {
      headers: { Authorization: `Bearer ${getToken()}` }, responseType: 'blob',
    })
    if (String(response.headers['content-type']).includes('application/json')) {
      const error = JSON.parse(await response.data.text())
      throw new Error(error.message || '下载失败')
    }
    const objectUrl = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = url.split('/').pop() || 'resume'
    link.click()
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  }
}
