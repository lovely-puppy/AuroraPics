import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'

//是否为首次获取登录用户
let firstFetchLoginUser: boolean = true

/**
 * 全局权限校验，每次切换时都会执行
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新时, 首次刷新时，能等待后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toURL = to.fullPath
  // 可以自己定义权限校验规则
  if (toURL.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限').then((r) => console.log(r))
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  if (toURL.startsWith('/add_picture')) {
    if (loginUser.userName === '未登录') {
      message.warning('请先登录').then((r) => console.log(r))
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  if (toURL.startsWith('/deepseek')) {
    if (loginUser.userName === '未登录') {
      message.warning('请先登录').then((r) => console.log(r))
      next(`/user/login?redirect=${to.fullPath}`)
    }
  }
  next() //放行
})
