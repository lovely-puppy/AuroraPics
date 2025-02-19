<template>
  <a-locale-provider :locale="zhCn">
    <div id="app">
      <BasicLayout/>
    </div>
  </a-locale-provider>
</template>

<script setup lang="ts">
// 国际化
import zhCn from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')
// 基本布局（header,body,bottom）
import BasicLayout from '@/layouts/BasicLayout.vue'
// import { healthUsingGet } from '@/api/mainController.ts'
// import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

//已经改为在权限校验中获取
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()
//
// healthUsingGet().then(res => {
//   console.log(res);
// })
</script>

<style scoped>

</style>
