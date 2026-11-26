import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    // 全局状态
    sidebarCollapsed: false
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    }
  }
})
