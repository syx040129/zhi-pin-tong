<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { Gender } from '@/types/api'
import { changePassword, updateProfile, uploadAvatar } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { genderMap, roleMap, tagOf } from '@/utils/format'

defineOptions({ name: 'ProfileView' })

const router = useRouter()
const auth = useAuthStore()

const profile = computed(() => auth.user)

/* ===== 基本信息 ===== */
const infoRef = ref<FormInstance>()
const savingInfo = ref(false)
const infoForm = reactive({
  nickname: '',
  gender: 'UNKNOWN' as Gender,
  phone: '',
  email: '',
})

const infoRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

function fillInfo(): void {
  const user = profile.value
  if (!user) return
  infoForm.nickname = user.nickname || ''
  infoForm.gender = user.gender || 'UNKNOWN'
  infoForm.phone = user.phone || ''
  infoForm.email = user.email || ''
}

async function submitInfo(): Promise<void> {
  if (!infoRef.value || savingInfo.value) return
  try {
    await infoRef.value.validate()
  } catch {
    return
  }
  savingInfo.value = true
  try {
    await updateProfile({
      nickname: infoForm.nickname.trim(),
      gender: infoForm.gender,
      phone: infoForm.phone.trim() || undefined,
      email: infoForm.email.trim() || undefined,
    })
    await auth.fetchMe()
    ElMessage.success('个人资料已更新')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    savingInfo.value = false
  }
}

/* ===== 头像上传 ===== */
const uploading = ref(false)

async function onAvatarChange(uploadFile: { raw?: File }): Promise<void> {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    await uploadAvatar(file)
    await auth.fetchMe()
    ElMessage.success('头像已更新')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    uploading.value = false
  }
}

/* ===== 修改密码 ===== */
const pwdRef = ref<FormInstance>()
const savingPwd = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6-30 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submitPassword(): Promise<void> {
  if (!pwdRef.value || savingPwd.value) return
  try {
    await pwdRef.value.validate()
  } catch {
    return
  }
  savingPwd.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    auth.logout()
    router.replace('/login')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    savingPwd.value = false
  }
}

onMounted(async () => {
  try {
    await auth.fetchMe()
  } catch {
    /* 已由拦截器提示并处理 */
  }
  fillInfo()
})
</script>

<template>
  <div class="page profile-page">
    <div class="profile-page__inner" v-if="profile">
      <!-- 左侧资料卡 -->
      <aside class="panel profile-card">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :auto-upload="false"
          accept="image/*"
          :on-change="onAvatarChange"
        >
          <div v-loading="uploading" class="avatar-uploader__box">
            <el-avatar :size="96" :src="profile.avatar || undefined" class="profile-card__avatar">
              {{ profile.nickname?.[0] || profile.username?.[0] || '我' }}
            </el-avatar>
            <span class="avatar-uploader__tip"><el-icon><Camera /></el-icon>更换头像</span>
          </div>
        </el-upload>

        <h2 class="profile-card__name">{{ profile.nickname }}</h2>
        <div class="profile-card__sub">
          <el-tag size="small" :type="tagOf(roleMap, profile.role).type" effect="light">
            {{ tagOf(roleMap, profile.role).label }}
          </el-tag>
          <span>@{{ profile.username }}</span>
        </div>

        <ul class="profile-card__list">
          <li>
            <span>注册时间</span>
            <strong>{{ profile.createTime?.replace('T', ' ').slice(0, 10) }}</strong>
          </li>
          <li>
            <span>性别</span>
            <strong>{{ tagOf(genderMap, profile.gender).label }}</strong>
          </li>
          <li>
            <span>手机号</span>
            <strong>{{ profile.phone || '未填写' }}</strong>
          </li>
          <li>
            <span>邮箱</span>
            <strong>{{ profile.email || '未填写' }}</strong>
          </li>
        </ul>
      </aside>

      <!-- 右侧编辑区 -->
      <div class="profile-page__main">
        <div class="panel edit-card">
          <h3 class="edit-card__title"><el-icon><EditPen /></el-icon>基本资料</h3>
          <el-form
            ref="infoRef"
            :model="infoForm"
            :rules="infoRules"
            label-width="90px"
            class="edit-form"
          >
            <el-form-item label="用户名">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="infoForm.nickname" placeholder="请输入昵称" maxlength="30" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="infoForm.gender">
                <el-radio-button value="MALE">男</el-radio-button>
                <el-radio-button value="FEMALE">女</el-radio-button>
                <el-radio-button value="UNKNOWN">保密</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingInfo" @click="submitInfo">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="panel edit-card">
          <h3 class="edit-card__title"><el-icon><Lock /></el-icon>修改密码</h3>
          <el-form
            ref="pwdRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            class="edit-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入原密码"
                autocomplete="current-password"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                show-password
                placeholder="6-30 位新密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="pwdForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入新密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" plain :loading="savingPwd" @click="submitPassword">
                确认修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page__inner {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
  align-items: start;
}

/* ===== 左侧资料卡 ===== */
.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px;
  text-align: center;
}

.avatar-uploader {
  cursor: pointer;
}

.avatar-uploader__box {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
}

.profile-card__avatar {
  background: var(--c-primary);
  color: #fff;
  font-size: 34px;
  font-weight: 700;
}

.avatar-uploader__tip {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 5px 0;
  border-radius: 0 0 48px 48px;
  color: #fff;
  font-size: 11px;
  background: rgba(15, 23, 42, 0.55);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-uploader:hover .avatar-uploader__tip {
  opacity: 1;
}

.profile-card__name {
  margin: 18px 0 0;
  font-size: 20px;
  font-weight: 700;
}

.profile-card__sub {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--c-text-3);
  font-size: 13px;
}

.profile-card__list {
  width: 100%;
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed var(--c-border);
}

.profile-card__list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  color: var(--c-text-2);
  font-size: 13px;
}

.profile-card__list strong {
  color: var(--c-text);
  font-weight: 500;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 右侧编辑 ===== */
.profile-page__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.edit-card {
  padding: 26px 30px 10px;
}

.edit-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 22px;
  padding-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  border-bottom: 1px solid var(--c-border);
}

.edit-card__title .el-icon {
  color: var(--c-primary);
}

.edit-form {
  max-width: 560px;
}

@media (max-width: 900px) {
  .profile-page__inner {
    grid-template-columns: 1fr;
  }
}
</style>
