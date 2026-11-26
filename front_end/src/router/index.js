import { createRouter, createWebHashHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/servers',
    children: [
      {
        path: 'servers',
        name: 'Servers',
        component: () => import('../views/servers/ServerList.vue'),
        meta: { title: 'Servers 管理' }
      },
      {
        path: 'servers/create',
        name: 'ServerCreate',
        component: () => import('../views/servers/ServerEdit.vue'),
        meta: { title: '创建 Server' }
      },
      {
        path: 'servers/:id/edit',
        name: 'ServerEdit',
        component: () => import('../views/servers/ServerEdit.vue'),
        meta: { title: '编辑 Server' }
      },
      {
        path: 'servers/import',
        name: 'ServerImport',
        component: () => import('../views/servers/ServerImport.vue'),
        meta: { title: '导入 Server' }
      },
      {
        path: 'tools',
        name: 'Tools',
        component: () => import('../views/tools/ToolList.vue'),
        meta: { title: 'Tools 管理' }
      },
      {
        path: 'tools/create',
        name: 'ToolCreate',
        component: () => import('../views/tools/ToolEdit.vue'),
        meta: { title: '创建 Tool' }
      },
      {
        path: 'tools/:id/edit',
        name: 'ToolEdit',
        component: () => import('../views/tools/ToolEdit.vue'),
        meta: { title: '编辑 Tool' }
      },
      {
        path: 'tools/import',
        name: 'ToolImport',
        component: () => import('../views/tools/ToolImport.vue'),
        meta: { title: '导入 Tools' }
      },
      {
        path: 'resources',
        name: 'Resources',
        component: () => import('../views/resources/ResourceList.vue'),
        meta: { title: 'Resources 管理' }
      },
      {
        path: 'resources/create',
        name: 'ResourceCreate',
        component: () => import('../views/resources/ResourceEdit.vue'),
        meta: { title: '创建 Resource' }
      },
      {
        path: 'resources/:id/edit',
        name: 'ResourceEdit',
        component: () => import('../views/resources/ResourceEdit.vue'),
        meta: { title: '编辑 Resource' }
      },
      {
        path: 'resources/import',
        name: 'ResourceImport',
        component: () => import('../views/resources/ResourceImport.vue'),
        meta: { title: '导入 Resources' }
      },
      {
        path: 'prompts',
        name: 'Prompts',
        component: () => import('../views/prompts/PromptList.vue'),
        meta: { title: 'Prompts 管理' }
      },
      {
        path: 'prompts/create',
        name: 'PromptCreate',
        component: () => import('../views/prompts/PromptEdit.vue'),
        meta: { title: '创建 Prompt' }
      },
      {
        path: 'prompts/:id/edit',
        name: 'PromptEdit',
        component: () => import('../views/prompts/PromptEdit.vue'),
        meta: { title: '编辑 Prompt' }
      },
      {
        path: 'audit-logs',
        name: 'AuditLogs',
        component: () => import('../views/audit-logs/AuditLogList.vue'),
        meta: { title: '审计日志' }
      },
      {
        path: 'audit-logs/:id',
        name: 'AuditLogDetail',
        component: () => import('../views/audit-logs/AuditLogDetail.vue'),
        meta: { title: '审计日志详情' }
      },
      {
        path: 'api-keys',
        name: 'ApiKeys',
        component: () => import('../views/api-keys/ApiKeyList.vue'),
        meta: { title: 'API Keys 管理' }
      },
      {
        path: 'api-keys/create',
        name: 'ApiKeyCreate',
        component: () => import('../views/api-keys/ApiKeyCreate.vue'),
        meta: { title: '创建 API Key' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
