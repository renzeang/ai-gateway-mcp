<template>
  <el-container class="layout-container">
    <!-- 侧边导航 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <span v-if="!sidebarCollapsed">MCP Gateway</span>
        <span v-else>MCP</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/servers" data-testid="nav-servers">
          <el-icon><Box /></el-icon>
          <span>Servers</span>
        </el-menu-item>
        <el-menu-item index="/tools" data-testid="nav-tools">
          <el-icon><Setting /></el-icon>
          <span>Tools</span>
        </el-menu-item>
        <el-menu-item index="/resources" data-testid="nav-resources">
          <el-icon><Document /></el-icon>
          <span>Resources</span>
        </el-menu-item>
        <el-menu-item index="/prompts" data-testid="nav-prompts">
          <el-icon><ChatDotRound /></el-icon>
          <span>Prompts</span>
        </el-menu-item>
        <el-menu-item index="/audit-logs" data-testid="nav-audit-logs">
          <el-icon><DocumentCopy /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/api-keys" data-testid="nav-api-keys">
          <el-icon><Key /></el-icon>
          <span>API Keys</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" icon="User" />
              <span class="username">Admin</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>设置</el-dropdown-item>
                <el-dropdown-item divided>退出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Fold,
  Expand,
  Setting,
  Box,
  Document,
  ChatDotRound,
  DocumentCopy,
  Key
} from '@element-plus/icons-vue'

const route = useRoute()
const sidebarCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.el-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  z-index: 9;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #606266;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
