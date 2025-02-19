<template>
  <div id="mySpacePage">
    <h1>正在加载中...</h1>
  </div>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { onMounted } from 'vue'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  // 判断用户是否登录
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
  }
  // 用户已经登录
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    current: 1,
    pageSize: 1,
    spaceType: SPACE_TYPE_ENUM.PRIVATE
  })
  if (res.data.code === 0) {
    // 用户有个人空间，加载第一个空间
    if (res.data.data?.records?.length > 0) {
      const space = res.data.data.records[0]
      router.replace(`/space/${space.id}`)
    } else {
      // 用户没有个人空间，跳转到创建空间页面
      router.replace('/add_space')
      message.warn('请先创建空间')
    }
  } else {
    message.error('加载空间失败' + res.data.message)
  }
}
// 页面加载时检查用户空间
onMounted(() =>{
  checkUserSpace()
})
</script>

<style scoped>
#mySpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
