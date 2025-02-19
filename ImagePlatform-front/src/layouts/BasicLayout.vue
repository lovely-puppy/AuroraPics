<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <global-header />
      </a-layout-header>
      <a-layout>
        <GlobalSider class="sider" />
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
      <a-layout-footer class="footer">
        <span class="author">极光云图 AuroraPic by wws</span>
        <a href="https://github.com/lovely-puppy/AuroraPics" style="color:#60acc5"><GithubOutlined/> 网站源码</a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
import { GithubOutlined } from '@ant-design/icons-vue'
</script>

<style scoped>
#basicLayout .header {
  background: white;
  color: unset;
  margin-bottom: 1px;
  padding-inline: 20px;
}

#basicLayout .footer {
  background: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  min-height: 50px;
}
#basicLayout .footer .author{
  text-align: center;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
}
#basicLayout .footer a{
  text-align: right;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 50px;
  font-size: 12px;
}

#basicLayout .content {
  padding: 28px;
  background: linear-gradient(#fefefe, #fff);
  margin-bottom: 30px;
}

#basicLayout .sider {
  background-color: #ffffff;
  border-right: 1px solid #eee;
  padding-top: 20px;
}

#basicLayout :deep(.ant-menu--root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}
</style>
