<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">{{ route.query?.id ? '修改' : '创建' }}{{ SPACE_TYPE_MAP[spaceType] }}</h2>
    <!-- 空间信息表单 -->
    <a-form layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item label="空间名称" name="spaceName">
        <a-input v-model:value="spaceForm.spaceName" placeholder="请输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
          allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading" style="width: 100%">提交</a-button>
      </a-form-item>
    </a-form>
    <!-- 空间级别介绍-->
    <a-card title="空间级别介绍">
      <a-typography-paragraph>
        <span style="font-weight: bold">* 目前仅支持普通版, 如需升级空间, <a href="javascript:;">请联系管理员</a></span>
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        <p style="font-weight: bold">{{ spaceLevel.text }}:</p>
        大小: {{ formatSize(spaceLevel.maxSize) }}<br />
        数量: {{ spaceLevel.maxCount }} 张图片
      </a-typography-paragraph>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { addSpaceUsingPost, getSpaceVoByIdUsingGet, listSpaceLevelUsingGet, updateSpaceUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import router from '@/router'
import { useRoute } from 'vue-router'
import { SPACE_LEVEL_OPTIONS, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space.ts'
import { formatSize } from '@/utils'

const spaceLevelList = ref<API.SpaceLevel[]>([])
const space = ref<API.SpaceVO>()
const spaceForm = reactive<API.SpaceAddRequest | API.SpaceUpdateRequest>({})
const loading = ref(false)

const route = useRoute()

// 空间类别
const spaceType = computed(() => {
  if (route.query?.type) {
    return Number(route.query.type)
  }
  return SPACE_TYPE_ENUM.PRIVATE
})
//获取空间级别列表
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别失败')
  }
}
onMounted(() => {
  fetchSpaceLevelList()
})

//获取老数据
const getOldSpace = async () => {
  //获取数据
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      // 回显表单
      spaceForm.spaceName = data.spaceName
      spaceForm.spaceLevel = data.spaceLevel
    }
  }
}
onMounted(() => {
  getOldSpace()
})

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  const spaceId = space.value?.id
  loading.value = true
  let res
  if (spaceId) {
    // 更新
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...spaceForm,
    })
  } else {
    // 新增
    res = await addSpaceUsingPost({
      ...spaceForm,
      spaceType: spaceType.value,
    })
  }
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    // 跳转到空间详情页
    await router.push({
      path: `/space/${res.data.data}`,
    })
  } else {
    message.error('操作失败' + res.data.message)
  }
  loading.value = false
}
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
