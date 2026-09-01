<template>
  <a-layout-sider width="200" style="background: #fff">
    <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        :style="{ height: '100%', borderRight: 0 }"
        @click="handleClick"
    >
      <a-menu-item key="/welcome">
        <coffee-outlined /> &nbsp;欢迎
      </a-menu-item>
      <a-menu-item key="/passenger">
        <user-outlined /> &nbsp;乘车人管理
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script>
import { UserOutlined, CoffeeOutlined } from '@ant-design/icons-vue';
import {defineComponent, ref, watch} from 'vue';
import router from "@/router";

export default defineComponent({
  name: "the-sider-view",
  components: {
    UserOutlined,
    CoffeeOutlined
  },
  setup() {
    // 初始选中和第一个菜单key保持一致 /welcome
    const selectedKeys = ref([]);

    watch(()=>router.currentRoute.value.path,(newValue)=>{
      console.log('watch', newValue);
      selectedKeys.value = [];
      selectedKeys.value.push(newValue);
    }, {immediate: true});

    // a-menu-item 不支持 to 属性，需要手动调用 router.push 跳转
    const handleClick = ({ key }) => {
      router.push(key);
    };
    return {
      selectedKeys,
      handleClick
    };
  }
});
</script>

<style scoped>

</style>
