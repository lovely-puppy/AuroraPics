import { createApp } from 'vue'
import { createPinia } from 'pinia'
//引入ant design
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import '@/access.ts'

import App from './App.vue'
import router from './router'
import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(VueCropper)

app.mount('#app')
