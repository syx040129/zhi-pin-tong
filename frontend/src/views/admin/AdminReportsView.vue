<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminDeleteReport, adminReportStatus, adminReports } from '@/api/admin'
import type { ReportStatus, ReportVO } from '@/types/api'
import { formatDateTime, reportStatusMap, reportTargetMap, tagOf } from '@/utils/format'

defineOptions({ name: 'AdminReportsView' })

const loading = ref(false)
const list = ref<ReportVO[]>([])
const total = ref(0)
const busy = ref('')
const filters = reactive<{ status: '' | ReportStatus }>({ status: '' })
const page = ref(1)
const size = ref(10)

const statusOptions = Object.keys(reportStatusMap) as ReportStatus[]

const dialogVisible = ref(false)
const current = ref<ReportVO | null>(null)
const formRef = ref<FormInstance>()
const handleForm = reactive<{ result: '' | ReportStatus; remark: string }>({ result: '', remark: '' })

const rules: FormRules = {
  result: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
}

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminReports({
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

function openHandle(row: ReportVO): void {
  current.value = row
  handleForm.result = ''
  handleForm.remark = ''
  dialogVisible.value = true
}

function submitHandle(): void {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || !current.value) return
    busy.value = `handle-${current.value.id}`
    try {
      await adminReportStatus(
        current.value.id,
        handleForm.result as ReportStatus,
        handleForm.remark.trim() || undefined,
      )
      ElMessage.success('举报已处理')
      dialogVisible.value = false
      await fetchList()
    } catch {
      // 拦截器已提示
    } finally {
      busy.value = ''
    }
  })
}

async function handleDelete(row: ReportVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除编号 ${row.id} 的举报记录吗？`, '删除举报', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  busy.value = `delete-${row.id}`
  try {
    await adminDeleteReport(row.id)
    ElMessage.success('举报记录已删除')
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
  <div class="reports-page">
    <div class="panel reports-panel">
      <div class="reports-head">
        <div>
          <div class="reports-head__title">举报管理</div>
          <div class="reports-head__sub">处理用户对岗位与企业的举报</div>
        </div>
      </div>

      <div class="reports-filters">
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option v-for="key in statusOptions" :key="key" :label="reportStatusMap[key].label" :value="key" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="reports-table">
        <el-table-column prop="id" label="编号" width="80" align="center" />
        <el-table-column label="举报人" width="130">
          <template #default="{ row }">{{ row.reporterName }}</template>
        </el-table-column>
        <el-table-column label="举报对象" min-width="200">
          <template #default="{ row }">
            <el-tag :type="tagOf(reportTargetMap, row.targetType).type" size="small" round>
              {{ tagOf(reportTargetMap, row.targetType).label }}
            </el-tag>
            <span class="target-name">{{ row.targetName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" width="140" show-overflow-tooltip />
        <el-table-column label="举报详情" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.detail || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagOf(reportStatusMap, row.status).type" round>
              {{ tagOf(reportStatusMap, row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handleRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="举报时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="primary" :disabled="busy !== ''" @click="openHandle(row)">
                <el-icon style="margin-right: 3px"><EditPen /></el-icon>处理
              </el-button>
            </template>
            <el-button link type="danger" :disabled="busy !== ''" @click="handleDelete(row)">
              <el-icon style="margin-right: 3px"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无举报记录" :image-size="90" />
        </template>
      </el-table>

      <div class="reports-pager">
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

    <el-dialog v-model="dialogVisible" title="处理举报" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="handleForm" :rules="rules" label-width="90px">
        <el-form-item label="举报对象">
          <span class="dialog-line">
            <el-tag :type="tagOf(reportTargetMap, current?.targetType).type" size="small" round>
              {{ tagOf(reportTargetMap, current?.targetType).label }}
            </el-tag>
            <span class="dialog-target">{{ current?.targetName }}</span>
          </span>
        </el-form-item>
        <el-form-item label="举报原因">
          <span class="dialog-line">{{ current?.reason }}</span>
        </el-form-item>
        <el-form-item label="举报详情">
          <div class="dialog-detail">{{ current?.detail || '-' }}</div>
        </el-form-item>
        <el-form-item label="处理结果" prop="result">
          <el-radio-group v-model="handleForm.result">
            <el-radio value="HANDLED">有效举报，已处理</el-radio>
            <el-radio value="IGNORED">无效举报，忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="填写处理说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <button type="button" class="grad-btn" :disabled="busy !== ''" @click="submitHandle">
          提交处理
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.reports-page {
  min-height: 100%;
}

.reports-panel {
  padding: 20px 22px;
}

.reports-head {
  margin-bottom: 18px;
}

.reports-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.reports-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.reports-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.status-select {
  width: 160px;
}

.target-name {
  margin-left: 8px;
  color: var(--c-text);
  font-weight: 500;
}

.reports-table {
  width: 100%;
}

.reports-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.dialog-line {
  color: var(--c-text-2);
}

.dialog-target {
  margin-left: 8px;
  font-weight: 500;
}

.dialog-detail {
  width: 100%;
  color: var(--c-text-2);
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
