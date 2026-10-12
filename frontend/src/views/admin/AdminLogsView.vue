<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { adminLogs } from '@/api/admin'
import type { SysLog } from '@/types/api'
import { formatDateTime, logTypeMap, tagOf } from '@/utils/format'

defineOptions({ name: 'AdminLogsView' })

type LogType = 'LOGIN' | 'OPERATE' | 'AI' | 'ERROR'

const loading = ref(false)
const list = ref<SysLog[]>([])
const total = ref(0)
const filters = reactive<{ type: '' | LogType }>({ type: '' })
const page = ref(1)
const size = ref(10)

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminLogs({
      type: filters.type || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.records
    total.value = res.total
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void fetchList()
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="logs-page">
    <div class="panel logs-panel">
      <div class="logs-head">
        <div>
          <div class="logs-head__title">系统日志</div>
          <div class="logs-head__sub">记录登录、操作、AI 调用与系统异常，仅支持只读查看</div>
        </div>
        <el-button type="primary" plain @click="handleSearch">
          <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <div class="logs-filter">
        <el-select
          v-model="filters.type"
          placeholder="全部类型"
          clearable
          class="type-select"
          @change="handleSearch"
        >
          <el-option v-for="(opt, key) in logTypeMap" :key="key" :label="opt.label" :value="key" />
        </el-select>
        <span class="logs-count">共 {{ total }} 条日志</span>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="logs-table">
        <el-table-column label="时间" width="172">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="96">
          <template #default="{ row }">
            <el-tag :type="tagOf(logTypeMap, row.type).type" round>
              {{ tagOf(logTypeMap, row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="120">
          <template #default="{ row }">{{ row.module || '-' }}</template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="150" show-overflow-tooltip />
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
        <el-table-column label="IP" width="150">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.costMs != null">{{ row.costMs }} ms</span>
            <span v-else class="dim-text">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无系统日志" :image-size="90" />
        </template>
      </el-table>

      <div class="logs-pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSearch"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.logs-page {
  min-height: 100%;
}

.logs-panel {
  padding: 20px 22px;
}

.logs-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.logs-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.logs-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.logs-filter {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.type-select {
  width: 200px;
}

.logs-count {
  color: var(--c-text-3);
  font-size: 13px;
}

.logs-table {
  width: 100%;
}

.dim-text {
  color: var(--c-text-3);
}

.logs-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
