<template>
  <div class="picture-list">
    <!--图片列表-->
    <a-list :grid="{ gutter: 16, xs: 1, sm: 2, md: 4, lg: 4, xl: 6, xxl: 3 }" :loading="loading" :data-source="dataList">
      <template #renderItem="{ item: picture }">
        <!-- 单张图片 -->
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a-card hoverable @click="handleClickPicture(picture)">
            <template #cover>
              <img style="height: 200px; object-fit: cover" :alt="picture.name" :src="picture.thumbnailUrl ?? picture.url" loading="lazy" />
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <template v-if="showOp" #actions>
              <a-space @click="(e) => doSearch(picture, e)">
                <SearchOutlined />
                搜索
              </a-space>
              <a-space @click="(e) => doShare(picture, e)">
                <ShareAltOutlined />
                分享
              </a-space>
              <a-space v-if="canEdit" @click="(e) => doEdit(picture, e)">
                <edit-outlined />
                编辑
              </a-space>
              <a-space v-if="canDelete" @click="(e) => doDelete(picture, e)">
                <delete-outlined />
                删除
              </a-space>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
// 数据
import { useRouter } from 'vue-router'
import { DeleteOutlined, EditOutlined, SearchOutlined, ShareAltOutlined } from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import ShareModal from '@/components/ShareModal.vue'

interface Props {
  dataList?: API.PictureVO
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  canEdit?: boolean
  canDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
// 跳转至图片详情
const handleClickPicture = (picture) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 编辑
const doEdit = (picture, e) => {
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除
const doDelete = async (picture, e) => {
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 让外层刷新
    props?.onReload()
  } else {
    message.error('删除失败')
  }
}

// 分享弹窗引用
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()

// 分享
const doShare = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}

// 搜索
const doSearch = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  window.open(`/search_picture?pictureId=${picture.id}`)
}
</script>

<style scoped></style>
