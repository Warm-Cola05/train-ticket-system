<template>
  <a-layout-sider width="200" style="background: #fff">
    <a-menu
        v-model:selectedKeys="selectedKeys2"
        mode="inline"
        :style="{ height: '100%', borderRight: 0 }"
    >
      <!-- a-menu-item自带to属性，内部自动集成router，不用写router-link -->
      <a-menu-item key="/welcome" to="/welcome">
        <coffee-outlined /> &nbsp;欢迎
      </a-menu-item>
      <a-menu-item key="/passenger" to="/passenger">
        <user-outlined /> &nbsp;乘车人管理
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script>
import { UserOutlined, CoffeeOutlined } from '@ant-design/icons-vue';
import { defineComponent, ref } from 'vue';

export default defineComponent({
  name: "the-sider-view",
  components: {
    UserOutlined,
    CoffeeOutlined
  },
  setup() {
    // 初始选中和第一个菜单key保持一致 /welcome
    const selectedKeys2 = ref(['/welcome']);
    return {
      selectedKeys2
    };
  }
});
</script>

<style scoped>

</style>
