<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FavoriteVO } from '@/types/api'
import { listFavorites, removeFavorite } from '@/api/favorite'
import { formatDateTime, jobStatusMap, salaryText, tagOf } from '@/utils/format'

defineOptions({ name: 'FavoritesView' })

const router = useRouter()

const list = ref<FavoriteVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)
const busyId = ref<number | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listFavorites({ page: page.value, size })
    list.value = res.records
    total.value = res.total
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(next: number): void {
  page.value = next
  load()
}

async function unfavorite(item: FavoriteVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `取消收藏「${item.jobName}」后需要重新收藏，确认取消吗？`,
      '取消收藏',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' },
    )
  } catch {
    return
  }
  if (busyId.value) return
  busyId.value = item.id
  try {
    await removeFavorite(item.jobId)
    ElMessage.success('已取消收藏')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await load()
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page favorites-page">
    <div class="favorites-page__inner">
      <div class="page-head">
        <div>
          <h1 class="page-head__title grad-text">我的收藏</h1>
          <p class="page-head__desc">收藏心仪职位，投递前多对比看看</p>
        </div>
        <el-button @click="router.push('/jobs')">
          <el-icon><Search /></el-icon>去发现职位
        </el-button>
      </div>

      <div v-loading="loading" class="fav-area">
        <el-empty
          v-if="!loading && list.length === 0"
          description="还没有收藏任何职位"
        >
          <el-button type="primary" @click="router.push('/jobs')">
            <el-icon><Search /></el-icon>去逛职位
          </el-button>
        </el-empty>

        <template v-else>
          <div class="fav-grid">
            <div v-for="item in list" :key="item.id" class="fav-card panel">
              <div class="fav-card__head">
                <el-avatar :size="46" :src="item.companyLogo || undefined" class="fav-card__logo">
                  {{ item.companyName?.[0] || '企' }}
                </el-avatar>
                <div class="fav-card__main">
                  <div class="fav-card__name" :title="item.jobName">{{ item.jobName }}</div>
                  <div class="fav-card__company" :title="item.companyName">{{ item.companyName }}</div>
                </div>
                <el-tag
                  size="small"
                  :type="tagOf(jobStatusMap, item.jobStatus).type"
                  effect="light"
                >
                  {{ tagOf(jobStatusMap, item.jobStatus).label }}
                </el-tag>
              </div>

              <div class="fav-card__mid">
                <span class="fav-card__salary grad-text">{{ salaryText(item.salaryMin, item.salaryMax) }}</span>
                <span v-if="item.city"><el-icon><Location /></el-icon>{{ item.city }}</span>
                <span>{{ item.education || '学历不限' }}</span>
                <span>{{ item.experience || '经验不限' }}</span>
              </div>

              <div class="fav-card__foot">
                <span class="fav-card__time">
                  <el-icon><Clock /></el-icon>收藏于 {{ formatDateTime(item.createTime) }}
                </span>
                <div class="fav-card__ops">
                  <el-button text type="primary" @click="router.push(`/jobs/${item.jobId}`)">
                    查看职位
                  </el-button>
                  <el-button
                    text
                    type="danger"
                    :loading="busyId === item.id"
                    @click="unfavorite(item)"
                  >
                    取消收藏
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="total > size" class="pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="size"
              :current-page="page"
              @current-change="handlePageChange"
            />
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.favorites-page__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.page-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

.fav-area {
  min-height: 320px;
}

.fav-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.fav-card {
  padding: 20px 22px;
  transition: box-shadow 0.2s ease;
}

.fav-card:hover {
  box-shadow: var(--shadow-md);
}

.fav-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fav-card__logo {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.fav-card__main {
  flex: 1;
  min-width: 0;
}

.fav-card__name {
  overflow: hidden;
  color: var(--c-text);
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-card__company {
  margin-top: 4px;
  overflow: hidden;
  color: var(--c-text-2);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-card__mid {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 16px;
  color: var(--c-text-2);
  font-size: 13px;
}

.fav-card__mid span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.fav-card__salary {
  font-size: 17px;
  font-weight: 650;
}

.fav-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--c-border);
}

.fav-card__time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-3);
  font-size: 12px;
}

.fav-card__ops {
  display: flex;
  flex: none;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .fav-grid {
    grid-template-columns: 1fr;
  }
}
</style>
