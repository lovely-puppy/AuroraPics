<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="260px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">极光云图 AuroraPics</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick" />
      </a-col>
      <!-- 用户信息展示栏 -->
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <div @click="showDrawer" style="cursor: pointer">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </div>
            <template>
              <a-drawer
                v-model:open="open"
                class="custom-class"
                root-class-name="root-class-name"
                :root-style="{ color: 'blue' }"
                style="color: black; cursor: default"
                width="250px"
                title="个人中心"
                placement="right"
                :drawer-width="200"
                @after-open-change="afterOpenChange">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                <span style="font-weight: bold">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
                <a-menu v-model:selectedKeys="current" @click="doMenuClick">
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <PoweroffOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </a-drawer>
            </template>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, PoweroffOutlined, UserOutlined } from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { logoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

console.log(loginUserStore.loginUser.userAvatar)

// 未经处理的菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '添加图片',
    title: '添加图片',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: '/support',
    label: '支持作者',
    title: '支持作者',
  },
]

// 根据权限过滤菜单栏
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 只有登录用户才能看到 /add_picture 页面
    if (menu?.key?.startsWith('/add_picture')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser.id || !loginUser.userAccount) {
        return false
      }
    }
    // 管理员才能看到 /admin 开头的菜单
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const items = computed(() => filterMenus(originItems))

const router = useRouter()
//路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}
//当前要高亮的菜单项
const current = ref<string[]>([])
//监听路由变化,更新高亮菜单
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

//实现抽屉效果
const open = ref<boolean>(false)

const afterOpenChange = (bool: boolean) => {
  console.log('open', bool)
}

const showDrawer = () => {
  open.value = true
}
//实现退出登录
const doLogout = async () => {
  const res = await logoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push({
      path: '/user/login',
    })
  } else {
    message.error('退出登录失败' + res.data.message)
  }
}
</script>

<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

.logo {
  height: 40px;
}

.custom-class {
  min-width: 100px;
  width: 200px;
}
</style>
