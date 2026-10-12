<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCompanies, adminCompanyStatus, adminDeleteCompany } from '@/api/admin'
import type { CompanyStatus, CompanyVO } from '@/types/api'
import { companyStatusMap, formatDateTime, tagOf } from '@/utils/format'

defineOptions({ name: 'AdminCompaniesView' })

const loading = ref(false)
const list = ref<CompanyVO[]>([])
const total = ref(0)
const busy = ref('')
const filters = reactive<{ keyword: string; status: '' | CompanyStatus }>({ keyword: '', status: '' })
const page = ref(1)
const size = ref(10)

const statusOptions = Object.keys(companyStatusMap) as CompanyStatus[]

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminCompanies({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
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

function handleReset(): void {
  filters.keyword = ''
  filters.status = ''
  handleSearch()
}

async function toggleStatus(row: CompanyVO): Promise<void> {
  const target: CompanyStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const verb = target === 'DISABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${verb}企业「${row.companyName}」吗？${target === 'DISABLED' ? '禁用后该企业的岗位将不可见。' : ''}`,
      `${verb}企业`,
      { type: target === 'DISABLED' ? 'warning' : 'success', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `status-${row.id}`
  try {
    await adminCompanyStatus(row.id, target)
    row.status = target
    ElMessage.success(target === 'ACTIVE' ? '企业已启用' : '企业已禁用')
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

async function handleDelete(row: CompanyVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除企业「${row.companyName}」吗？其关联岗位与数据将被一并删除，不可恢复。`,
      '删除企业',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `delete-${row.id}`
  try {
    await adminDeleteCompany(row.id)
    ElMessage.success('企业已删除')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await fetchList()
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="companies-page">
    <div class="panel companies-panel">
      <div class="companies-head">
        <div>
          <div class="companies-head__title">企业管理</div>
          <div class="companies-head__sub">平台入驻企业，可查看资料并执行禁用 / 启用 / 删除</div>
        </div>
      </div>

      <div class="companies-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索企业名称 / 绑定账号"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option v-for="key in statusOptions" :key="key" :label="companyStatusMap[key].label" :value="key" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="companies-table">
        <el-table-column label="企业" min-width="220">
          <template #default="{ row }">
            <div class="company-cell">
              <div class="logo-box">
                <img v-if="row.logo" :src="row.logo" alt="" class="logo-img" />
                <span v-else class="logo-img logo-fallback">{{ (row.companyName || '企').charAt(0) }}</span>
              </div>
              <div class="company-cell__main">
                <div class="company-name">{{ row.companyName }}</div>
                <div class="company-sub">{{ row.industry || '未填写行业' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="规模" width="120">
          <template #default="{ row }">{{ row.scale || '-' }}</template>
        </el-table-column>
        <el-table-column label="地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '-' }}</template>
        </el-table-column>
        <el-table-column label="在线岗位" width="100" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.jobCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定账号" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagOf(companyStatusMap, row.status).type" round>{{ tagOf(companyStatusMap, row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入驻时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              link
              type="warning"
              :disabled="busy !== ''"
              @click="toggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button v-else link type="success" :disabled="busy !== ''" @click="toggleStatus(row)">
              启用
            </el-button>
            <el-button link type="danger" :disabled="busy !== ''" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的企业" :image-size="90" />
        </template>
      </el-table>

      <div class="companies-pager">
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
.companies-page {
  min-height: 100%;
}

.companies-panel {
  padding: 20px 22px;
}

.companies-head {
  margin-bottom: 18px;
}

.companies-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.companies-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.companies-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.keyword-input {
  width: 280px;
}

.status-select {
  width: 150px;
}

.company-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-box {
  flex: none;
  width: 42px;
  height: 42px;
  overflow: hidden;
  border-radius: 10px;
  background: var(--c-primary-soft);
}

.logo-img {
  width: 42px;
  height: 42px;
  object-fit: cover;
}

.logo-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-primary);
  font-weight: 700;
}

.company-name {
  font-weight: 600;
  color: var(--c-text);
}

.company-sub {
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
}

.companies-table {
  width: 100%;
}

.companies-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
