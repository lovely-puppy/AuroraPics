<template>
  <div id="deepseekPage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        placeholder="请输入问题"
        v-model:value="searchParams.searchText"
        enter-button="深度思考"
        size="large"
        @search="handleSearch"
      />
    </div>

    <!-- 结果展示区 -->
    <div class="result-container">
      <a-spin :spinning="loading">
        <!-- 错误提示 -->
        <a-alert
          v-if="errorMessage"
          :message="errorMessage"
          type="error"
          show-icon
          closable
        />

        <!-- 结果内容 -->
        <template v-if="resultData">
          <div class="section">
            <h3 class="section-title">推理过程</h3>
            <div class="content-box reasoning">
              {{ resultData.reasoningContent || '无详细推理记录' }}
            </div>
          </div>

          <div class="section">
            <h3 class="section-title">最终结论</h3>
            <div class="content-box answer">
              {{ resultData.content || '未生成有效回答' }}
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <a-empty
          v-else-if="!loading"
          description="输入问题开始深度研究"
        />
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getDeepSeekResultUsingPost } from '@/api/deepSeekController'
import { message } from 'ant-design-vue'

// 响应数据
const resultData = ref<API.DeepSeekResultResponse>()
const errorMessage = ref('')
const loading = ref(false)

// 搜索参数
const searchParams = reactive<API.DeepSeekResultRequest>({
  searchText: '',
})

// 获取数据
const fetchData = async () => {
  // 校验参数
  if (!searchParams.searchText?.trim()) {
    return
  }
  loading.value = true
  try {
    const res = await getDeepSeekResultUsingPost({
      searchText: searchParams.searchText
    })

    if (res.data.code === 0) {
      resultData.value = res.data.data
    } else {
      errorMessage.value = res.data.message || '请求失败'
    }
  } catch (e) {
    errorMessage.value = e.message || '服务异常'
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  // 搜索内容不能为空
  if (!searchParams.searchText?.trim()) {
    message.warning('请输入问题')
    return
  }
  // 清空旧数据
  errorMessage.value = ''
  resultData.value = undefined

  fetchData()
}

// 初始化加载
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#deepseekPage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.search-bar {
  max-width: 800px;
  margin: 0 auto 32px;
}

.result-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.section {
  margin: 24px 0;
}

.section-title {
  color: #1d3557;
  border-left: 4px solid #457b9d;
  padding-left: 12px;
  margin-bottom: 16px;
}

.content-box {
  padding: 16px;
  border-radius: 4px;
  line-height: 1.8;
}

.reasoning {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  white-space: pre-wrap;
}

.answer {
  background: #e7f5ff;
  border: 1px solid #74c0fc;
}
</style>
