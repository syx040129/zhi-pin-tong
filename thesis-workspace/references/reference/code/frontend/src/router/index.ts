import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import type { UserRole } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

import UserLayout from '@/layouts/UserLayout.vue'
import CompanyLayout from '@/layouts/CompanyLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const HomeView = () => import('@/views/HomeView.vue')
const LoginView = () => import('@/views/auth/LoginView.vue')
const RegisterView = () => import('@/views/auth/RegisterView.vue')
const JobListView = () => import('@/views/job/JobListView.vue')
const JobDetailView = () => import('@/views/job/JobDetailView.vue')
const ProfileView = () => import('@/views/user/ProfileView.vue')
const ResumeListView = () => import('@/views/user/resume/ResumeListView.vue')
const ResumeEditView = () => import('@/views/user/resume/ResumeEditView.vue')
const ResumeAnalysisView = () => import('@/views/user/resume/ResumeAnalysisView.vue')
const RecommendView = () => import('@/views/user/recommend/RecommendView.vue')
const JobMatchView = () => import('@/views/user/recommend/JobMatchView.vue')
const MyApplicationsView = () => import('@/views/user/MyApplicationsView.vue')
const FavoritesView = () => import('@/views/user/FavoritesView.vue')
const InterviewListView = () => import('@/views/user/interview/InterviewListView.vue')
const InterviewPlayView = () => import('@/views/user/interview/InterviewPlayView.vue')
const InterviewReportView = () => import('@/views/user/interview/InterviewReportView.vue')

const CompanyDashboardView = () => import('@/views/company/CompanyDashboardView.vue')
const CompanyProfileView = () => import('@/views/company/CompanyProfileView.vue')
const CompanyJobsView = () => import('@/views/company/CompanyJobsView.vue')
const CompanyJobEditView = () => import('@/views/company/CompanyJobEditView.vue')
const CompanyApplicationsView = () => import('@/views/company/CompanyApplicationsView.vue')
const CompanyCandidatesView = () => import('@/views/company/CompanyCandidatesView.vue')

const AdminDashboardView = () => import('@/views/admin/AdminDashboardView.vue')
const AdminUsersView = () => import('@/views/admin/AdminUsersView.vue')
const AdminCompaniesView = () => import('@/views/admin/AdminCompaniesView.vue')
const AdminJobsView = () => import('@/views/admin/AdminJobsView.vue')
const AdminResumesView = () => import('@/views/admin/AdminResumesView.vue')
const AdminApplicationsView = () => import('@/views/admin/AdminApplicationsView.vue')
const AdminReportsView = () => import('@/views/admin/AdminReportsView.vue')
const AdminLogsView = () => import('@/views/admin/AdminLogsView.vue')
const AdminSettingsView = () => import('@/views/admin/AdminSettingsView.vue')

declare module 'vue-router' {
  interface RouteMeta {
    /** 是否需要登录 */
    requiresAuth?: boolean
    /** 允许访问的角色；缺省表示已登录即可 */
    roles?: UserRole[]
    /** 页面标题 */
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: UserLayout,
    children: [
      { path: '', name: 'Home', component: HomeView, meta: { title: '首页' } },
      {
        path: 'jobs',
        name: 'JobList',
        component: JobListView,
        meta: { title: '职位中心' },
      },
      {
        path: 'jobs/:id',
        name: 'JobDetail',
        component: JobDetailView,
        meta: { title: '职位详情' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView,
        meta: { requiresAuth: true, roles: ['USER'], title: '个人中心' },
      },
      {
        path: 'resume',
        name: 'ResumeList',
        component: ResumeListView,
        meta: { requiresAuth: true, roles: ['USER'], title: '我的简历' },
      },
      {
        path: 'resume/edit',
        name: 'ResumeEdit',
        component: ResumeEditView,
        meta: { requiresAuth: true, roles: ['USER'], title: '编辑简历' },
      },
      {
        path: 'resume/edit/:id',
        name: 'ResumeEditById',
        component: ResumeEditView,
        meta: { requiresAuth: true, roles: ['USER'], title: '编辑简历' },
      },
      {
        path: 'resume/analysis',
        name: 'ResumeAnalysis',
        component: ResumeAnalysisView,
        meta: { requiresAuth: true, roles: ['USER'], title: 'AI 简历分析' },
      },
      {
        path: 'recommend',
        name: 'Recommend',
        component: RecommendView,
        meta: { requiresAuth: true, roles: ['USER'], title: '岗位推荐' },
      },
      {
        path: 'recommend/:jobId',
        name: 'JobMatch',
        component: JobMatchView,
        meta: { requiresAuth: true, roles: ['USER'], title: '岗位匹配' },
      },
      {
        path: 'applications',
        name: 'MyApplications',
        component: MyApplicationsView,
        meta: { requiresAuth: true, roles: ['USER'], title: '我的投递' },
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: FavoritesView,
        meta: { requiresAuth: true, roles: ['USER'], title: '我的收藏' },
      },
      {
        path: 'ai/interview',
        name: 'InterviewList',
        component: InterviewListView,
        meta: { requiresAuth: true, roles: ['USER'], title: '面试练习' },
      },
      {
        path: 'ai/interview/:id',
        name: 'InterviewPlay',
        component: InterviewPlayView,
        meta: { requiresAuth: true, roles: ['USER'], title: '模拟面试' },
      },
      {
        path: 'ai/interview/:id/report',
        name: 'InterviewReport',
        component: InterviewReportView,
        meta: { requiresAuth: true, roles: ['USER'], title: '面试报告' },
      },
    ],
  },

  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
    meta: { title: '注册' },
  },

  {
    path: '/company',
    component: CompanyLayout,
    meta: { requiresAuth: true, roles: ['COMPANY'] },
    children: [
      {
        path: '',
        name: 'CompanyDashboard',
        component: CompanyDashboardView,
        meta: { title: '数据看板' },
      },
      {
        path: 'profile',
        name: 'CompanyProfile',
        component: CompanyProfileView,
        meta: { title: '企业信息' },
      },
      {
        path: 'jobs',
        name: 'CompanyJobs',
        component: CompanyJobsView,
        meta: { title: '岗位管理' },
      },
      {
        path: 'jobs/create',
        name: 'CompanyJobCreate',
        component: CompanyJobEditView,
        meta: { title: '发布岗位' },
      },
      {
        path: 'jobs/:id/edit',
        name: 'CompanyJobEdit',
        component: CompanyJobEditView,
        meta: { title: '编辑岗位' },
      },
      {
        path: 'applications',
        name: 'CompanyApplications',
        component: CompanyApplicationsView,
        meta: { title: '收到投递' },
      },
      {
        path: 'candidates',
        name: 'CompanyCandidates',
        component: CompanyCandidatesView,
        meta: { title: '候选人库' },
      },
    ],
  },

  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: AdminDashboardView,
        meta: { title: '数据看板' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: AdminUsersView,
        meta: { title: '用户管理' },
      },
      {
        path: 'companies',
        name: 'AdminCompanies',
        component: AdminCompaniesView,
        meta: { title: '企业管理' },
      },
      {
        path: 'jobs',
        name: 'AdminJobs',
        component: AdminJobsView,
        meta: { title: '岗位管理' },
      },
      {
        path: 'resumes',
        name: 'AdminResumes',
        component: AdminResumesView,
        meta: { title: '简历管理' },
      },
      {
        path: 'applications',
        name: 'AdminApplications',
        component: AdminApplicationsView,
        meta: { title: '投递管理' },
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: AdminReportsView,
        meta: { title: '举报管理' },
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: AdminLogsView,
        meta: { title: '系统日志' },
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: AdminSettingsView,
        meta: { title: 'AI 配置' },
      },
    ],
  },

  // roleHome() 兼容别名（/company/dashboard 与 /admin/dashboard 语义 = 各端首页）
  {
    path: '/company/dashboard',
    redirect: '/company',
    meta: { requiresAuth: true, roles: ['COMPANY'] },
  },
  {
    path: '/admin/dashboard',
    redirect: '/admin',
    meta: { requiresAuth: true, roles: ['ADMIN'] },
  },

  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  // 设置浏览器标题
  if (to.meta.title) {
    document.title = `${to.meta.title} · 智聘`
  } else {
    document.title = '智聘 · CareerNova'
  }

  // 需要登录的页面
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 角色受限页面
    const allowed = to.meta.roles
    if (allowed && allowed.length > 0 && (!auth.role || !allowed.includes(auth.role))) {
      return auth.roleHome()
    }
  }
  return true
})

export default router
