// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getDeepSeekResult POST /api/deepseek/search */
export async function getDeepSeekResultUsingPost(body: API.DeepSeekResultRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseDeepSeekResultResponse_>('/api/deepseek/search', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
