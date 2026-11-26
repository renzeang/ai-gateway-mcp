import { defineStore } from 'pinia'
import api from '@/api'

export const useServerStore = defineStore('server', {
  state: () => ({
    servers: [],
    total: 0,
    loading: false
  }),
  actions: {
    async fetchServers(params = {}) {
      this.loading = true
      try {
        const res = await api.getServers(params)
        this.servers = res.data.list
        this.total = res.data.total
      } finally {
        this.loading = false
      }
    },
    async deleteServer(id) {
      await api.deleteServer(id)
      await this.fetchServers()
    }
  }
})
