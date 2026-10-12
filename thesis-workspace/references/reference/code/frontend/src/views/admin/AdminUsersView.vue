<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteUser, adminResetPassword, adminUserStatus, adminUsers } from '@/api/admin'
import type { SysUserAdminVO } from '@/types/api'
import type { UserRole, UserStatus } from '@/types/api'
import { formatDateTime, roleMap, tagOf, userStatusMap } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'AdminUsersView' })

const auth = useAuthStore()

const loading = ref(false)
const list = ref<SysUserAdminVO[]>([])
const total = ref(0)
const busy = ref('')
const filters = reactive<{
  keyword: string
  role: '' | UserRole
  status: '' | UserStatus
}>({ keyword: '', role: '', status: '' })
const page = ref(1)
const size = ref(10)

const roleOptions = Object.keys(roleMap) as UserRole[]
const statusOptions = Object.keys(userStatusMap) as UserStatus[]

const meId = auth.user?.id ?? -1

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminUsers({
      keyword: filters.keyword.trim() || undefined,
      role: filters.role || undefined,
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
  filters.role = ''
  filters.status = ''
  handleSearch()
}

function isAdmin(row: SysUserAdminVO): boolean {
  return row.role === 'ADMIN'
}

function isSelf(row: SysUserAdminVO): boolean {
  return row.id === meId
}

async function toggleStatus(row: SysUserAdminVO): Promise<void> {
  if (isAdmin(row)) {
    ElMessage.warning('管理员账号不允许禁用')
    return
  }
  const target: UserStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const verb = target === 'DISABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${verb}用户「${row.nickname || row.username}」吗？${target === 'DISABLED' ? '禁用后该用户将无法登录。' : ''}`,
      `${verb}用户`,
      { type: target === 'DISABLED' ? 'warning' : 'success', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `status-${row.id}`
  try {
    await adminUserStatus(row.id, target)
    row.status = target
    ElMessage.success(target === 'ENABLED' ? '用户已启用' : '用户已禁用')
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

async function resetPassword(row: SysUserAdminVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定重置用户「${row.nickname || row.username}」的密码吗？重置后默认密码为 123456，请提醒用户及时修改。`,
      '重置密码',
      { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `reset-${row.id}`
  try {
    await adminResetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

async function handleDelete(row: SysUserAdminVO): Promise<void> {
  if (isAdmin(row) || isSelf(row)) return
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.nickname || row.username}」吗？删除后其相关数据将一并清理，不可恢复。`,
      '删除用户',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `delete-${row.id}`
  try {
    await adminDeleteUser(row.id)
    ElMessage.success('用户已删除')
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
  <div class="users-page">
    <div class="panel users-panel">
      <div class="users-head">
        <div>
          <div class="users-head__title">用户管理</div>
          <div class="users-head__sub">平台全部注册用户，支持启用、禁用、重置密码与删除</div>
        </div>
      </div>

      <div class="users-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索用户名 / 昵称 / 手机号 / 邮箱"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filters.role" placeholder="全部角色" clearable class="role-select" @change="handleSearch">
          <el-option v-for="key in roleOptions" :key="key" :label="roleMap[key].label" :value="key" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable class="status-select" @change="handleSearch">
          <el-option v-for="key in statusOptions" :key="key" :label="userStatusMap[key].label" :value="key" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="users-table">
        <el-table-column label="用户" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="34" :src="row.avatar || undefined" class="user-avatar">
                {{ (row.nickname || row.username || '用').charAt(0) }}
              </el-avatar>
              <div class="user-cell__main">
                <div class="user-cell__name">
                  {{ row.nickname || row.username }}
                  <span v-if="isSelf(row)" class="self-tag">我</span>
                </div>
                <div class="user-cell__sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="tagOf(roleMap, row.role).type" round>{{ tagOf(roleMap, row.role).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagOf(userStatusMap, row.status).type" round>{{ tagOf(userStatusMap, row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-tooltip :disabled="!isAdmin(row)" content="管理员账号不允许禁用" placement="top">
              <span>
                <el-button
                  v-if="row.status === 'ENABLED'"
                  link
                  type="warning"
                  :disabled="busy !== '' || isAdmin(row)"
                  @click="toggleStatus(row)"
                >
                  禁用
                </el-button>
                <el-button v-else link type="success" :disabled="busy !== ''" @click="toggleStatus(row)">
                  启用
                </el-button>
              </span>
            </el-tooltip>
            <el-button link type="primary" :disabled="busy !== ''" @click="resetPassword(row)">重置密码</el-button>
            <el-tooltip :disabled="!isAdmin(row) && !isSelf(row)" content="管理员与当前账号不可删除" placement="top">
              <span>
                <el-button
                  link
                  type="danger"
                  :disabled="busy !== '' || isAdmin(row) || isSelf(row)"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的用户" :image-size="90" />
        </template>
      </el-table>

      <div class="users-pager">
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
.users-page {
  min-height: 100%;
}

.users-panel {
  padding: 20px 22px;
}

.users-head {
  margin-bottom: 18px;
}

.users-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.users-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.users-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.keyword-input {
  width: 300px;
}

.role-select,
.status-select {
  width: 140px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.user-cell__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--c-text);
}

.self-tag {
  padding: 1px 5px;
  border-radius: 4px;
  color: var(--c-primary);
  font-size: 11px;
  background: rgba(79, 107, 246, 0.12);
}

.user-cell__sub {
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
}

.users-table {
  width: 100%;
}

.users-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
