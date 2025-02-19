<template>
  <div id="supportAuthor">
    <div  style="display: inline-block;text-align: center;line-height: 48px;font-size: 36px; margin: auto">{{ msg }}</div>
    <div style="display: inline-block;height: 48px;text-align: center;line-height: 48px;" ><a href="https://github.com/lovely-puppy" target="_blank">（联系我）</a></div>

    <div style="margin-bottom: 16px;"></div>
    <a-image style="width: 350px" src="src/assets/support.jpg" alt=""></a-image>
  </div>
</template>

<script setup lang="ts">
const msg = '蟹蟹老板支持!'
</script>

<style scoped>
#supportAuthor {
  color: red;
  text-align: center;
  margin: 120px;
}
</style>
