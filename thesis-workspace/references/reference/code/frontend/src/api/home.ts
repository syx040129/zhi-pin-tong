import request from '@/utils/request'
import type { HealthInfo, HomeStats } from '@/types/api'

/** 首页统计（公开，后端缓存 300s） */
export function homeStats(): Promise<HomeStats> {
  return request.get<HomeStats>('/home/stats')
}

/** 系统健康状态（公开） */
export function health(): Promise<HealthInfo> {
  return request.get<HealthInfo>('/common/health')
}
