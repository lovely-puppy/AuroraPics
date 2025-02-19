import axios from "axios"
import { message } from 'ant-design-vue'

//创建axios实例
const myAxios = axios.create({
  baseURL: '',
  //  baseURL: "http://localhost:8080",
  timeout: 120000,
  withCredentials: true
})

// Add a request interceptor
axios.interceptors.request.use(function (config) {
  // Do something before request is sent
  return config;
}, function (error) {
  // Do something with request error
  return Promise.reject(error);
});

// Add a response interceptor
axios.interceptors.response.use(function (response) {
  const {data} = response
  if (data.code === 40100) {
    if (!response.request.requestURL.includes('/user/get/login') && !window.location.pathname.includes('/user/login')) {
      message.warning("请先登录").then(r => console.log(r))
      window.location.href = `/user/login?redirect=${window.location.href}`
    }
  }
  return response;
}, function (error) {
  // Any status codes that falls outside the range of 2xx cause this function to trigger
  // Do something with response error
  return Promise.reject(error);
});

export default myAxios;
