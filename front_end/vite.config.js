import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import {resolve} from 'path'

// https://vite.dev/config/
export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd(), '')

    return {
        plugins: [vue()],
        resolve: {
            alias: {
                '@': resolve(__dirname, 'src')
            }
        },
        server: {
            host: '0.0.0.0',
            port: 5173,
            proxy: {
                '/api/v1': {
                    target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
                    changeOrigin: true
                }
            }
        }
    }
})

