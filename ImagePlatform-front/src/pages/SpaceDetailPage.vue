<template>
  <div id="spaceDetailPage">
    <!-- 空间基本信息 -->
    <a-flex justify="space-between" style="margin-bottom: 12px">
      <h2>
        {{ space.spaceName }}（{{ SPACE_TYPE_MAP[space.spaceType] }}）
        <img
          v-if="space.spaceLevel > 0"
          src="https://www.codefather.cn/static/vip.a1ea732e.svg"
          alt="SAG img"
          style="margin: 0 5px 5px; width: 25px; height: 25px" />
        <span style="color: lightblue">{{ SPACE_LEVEL_MAP[space.spaceLevel] }}</span>
      </h2>
      <a-space size="middle">
        <a-button v-if="canUploadPicture" type="primary" :href="`/add_picture?spaceId=${id}`"> + 创建图片</a-button>
        <a-button v-if="canManageSpaceUser" type="primary" ghost :icon="h(BarChartOutlined)" :href="`/space_analyze?spaceId=${id}`">
          空间分析
        </a-button>
        <a-button v-if="canEditPicture" :icon="h(EditOutlined)" @click="doBatchEdit"> 批量编辑</a-button>
        <a-button v-if="canManageSpaceUser && SPACE_TYPE_ENUM.TEAM === space.spaceType" type="primary" ghost :icon="h(TeamOutlined)" :href="`/spaceUserManage/${id}`"> 成员管理 </a-button>
        <a-tooltip :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
          <a-progress type="circle" :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)" :size="42" />
        </a-tooltip>
      </a-space>
    </a-flex>
    <PictureSearchForm :onSearch="onSearch" />
    <!-- 图片列表 -->
    <PictureList
      :canEdit="canEditPicture"
      :canDelete="canDeletePicture"
      style="margin-top: 20px"
      :dataList="dataList"
      :loading="loading"
      :showOp="true"
      :onReload="fetchData" />
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
      @change="onPageChange" />
    <BatchEditPictureModal ref="batchEditPictureModalRef" :spaceId="id" :pictureList="dataList" :onSuccess="onBatchEditPictureSuccess" />
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import { SPACE_LEVEL_MAP, SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '../constants/space.ts'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { BarChartOutlined, EditOutlined, TeamOutlined } from '@ant-design/icons-vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'

const props = defineProps<{
  id: string | number
}>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// ----------------获取空间详情------------------
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
    } else {
      message.error('获取空间详情失败', res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间详情失败', error.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})
// ----------------获取图片列表------------------
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchData()
}

// 分享弹窗引用
const batchEditPictureModalRef = ref()

// 批量编辑成功后，刷新数据
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开批量编辑弹窗
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

// 空间id改变时， 必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  margin-bottom: 16px;
}
</style>
