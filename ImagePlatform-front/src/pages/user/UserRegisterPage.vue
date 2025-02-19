<template>
  <div id="userRegisterPage">
    <h2 class="title">云图库 - 用户注册</h2>
    <div class="description">智能协同云图库</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号!' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码!' },
          { min: 6, message: '密码长度不能低于6位!' },
        ]">
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码!' },
          { min: 6, message: '密码长度不能低于6位!' },
        ]">
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>

      <div class="tips">
        已有账号?
        <RouterLink to="/user/login">立即登录</RouterLink>
      </div>

      <a-form-item>
        <a-button type="primary" style="width: 100%" html-type="submit">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { userLoginUsingPost, userRegisterUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import router from '@/router'
import { message } from 'ant-design-vue'

//用户接受表单输入值
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const loginUserStore = useLoginUserStore()

/**
 * 表单提交
 * @param values
 */
const handleSubmit = async (values: any) => {
  //校验两次密码是否一致
  if (values.userPassword !== values.checkPassword) {
    message.error('两次密码不一致')
    return
  }
  const res = await userRegisterUsingPost(values)
  //注册成功，跳转到登录页
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败, ' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 16px;
}

.description {
  text-align: center;
  color: #bbb;
  font-size: 14px;
  margin-bottom: 16px;
}

.tips {
  color: #bbbbbb;
  text-align: right;
  font-size: 12px;
  margin-bottom: 16px;
}
</style>
