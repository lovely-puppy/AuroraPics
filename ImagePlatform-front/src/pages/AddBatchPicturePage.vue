<template>
  <div id="addBatchPicturePage">
    <h2 style="margin-bottom: 16px">
      批量创建
    </h2>
    <!-- 图片信息表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="关键词" name="searchText">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear />
      </a-form-item>
      <a-form-item label="抓取数量" name="fetchCount">
        <a-input-number v-model:value="formData.fetchCount" placeholder="请输入抓取数量" style="min-width: 180px" :min="1" :max="30" allow-clear />
      </a-form-item>
      <a-form-item label="名称前缀" name="namePrefix">
        <a-auto-complete v-model:value="formData.namePrefix" placeholder="请输入图片名称前缀,自动补充序号" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">开始创建</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  uploadPictureByBatchUsingPost
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

const formData = reactive<API.PictureUploadByBatchRequest>({
  fetchCount: 10
})

const loading = ref(false)

const router = useRouter()

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  try {
    loading.value = true
    const res = await uploadPictureByBatchUsingPost({
      ...formData,
    })
    if (res.data.code === 0 && res.data.data) {
      console.log(res);
      message.success(`创建成功, 共 ${res.data.data} 条`)
      // 跳转到主页
      await router.push({
        path: `/`,
      })
    } else {
      message.error('添加失败' + res.data.message)
    }
  } catch (error) {
    message.error('添加失败' + error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#addBatchPicturePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
