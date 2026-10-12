import request from '@/utils/request'
import type { FavoriteVO, PageResult } from '@/types/api'

export function addFavorite(jobId: number): Promise<void> {
  return request.post(`/favorites/${jobId}`)
}

export function removeFavorite(jobId: number): Promise<void> {
  return request.delete(`/favorites/${jobId}`)
}

export function listFavorites(params: {
  page?: number
  size?: number
}): Promise<PageResult<FavoriteVO>> {
  return request.get<PageResult<FavoriteVO>>('/favorites', { params })
}

export function favoriteStatus(jobId: number): Promise<{ favorite: boolean }> {
  return request.get<{ favorite: boolean }>(`/favorites/status/${jobId}`)
}
