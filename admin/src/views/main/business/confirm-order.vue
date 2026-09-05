<template>
  <p>
    <a-space>
      <a-button type="primary" @click="handleQuery()">刷新</a-button>
      <a-button type="primary" @click="onAdd">新增</a-button>
    </a-space>
  </p>
  <a-table :dataSource="confirmOrders"
           :columns="columns"
           :pagination="pagination"
           @change="handleTableChange"
           :loading="loading">
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'operation'">
        <a-space>
          <a-popconfirm
              title="删除后不可恢复，确认删除?"
              @confirm="onDelete(record)"
              ok-text="确认" cancel-text="取消">
            <a style="color: red">删除</a>
          </a-popconfirm>
          <a @click="onEdit(record)">编辑</a>
        </a-space>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <span v-for="item in CONFIRM_ORDER_STATUS_ARRAY" :key="item.code">
          <span v-if="item.code === record.status">
            {{item.desc}}
          </span>
        </span>
      </template>
    </template>
  </a-table>
  <a-modal v-model:visible="visible" title="订单信息" @ok="handleOk"
           ok-text="确认" cancel-text="取消">
    <a-form :model="confirmOrder" :label-col="{span: 4}" :wrapper-col="{ span: 20 }">
      <a-form-item label="会员id">
        <a-input-number v-model:value="confirmOrder.memberId" :min="0" />
      </a-form-item>
      <a-form-item label="日期">
        <a-date-picker v-model:value="confirmOrder.date" valueFormat="YYYY-MM-DD" placeholder="请选择日期" />
      </a-form-item>
      <a-form-item label="车次编号">
        <a-input v-model:value="confirmOrder.trainCode" />
      </a-form-item>
      <a-form-item label="出发站">
        <a-input v-model:value="confirmOrder.start" />
      </a-form-item>
      <a-form-item label="到达站">
        <a-input v-model:value="confirmOrder.end" />
      </a-form-item>
      <a-form-item label="余票ID">
        <a-input-number v-model:value="confirmOrder.dailyTrainTicketId" :min="0" />
      </a-form-item>
      <a-form-item label="车票">
        <a-textarea v-model:value="confirmOrder.tickets" :rows="4"
                    placeholder='JSON，如 [{"passengerName":"张三","seatType":"2","seat":"A"}]' />
      </a-form-item>
      <a-form-item label="订单状态">
        <a-select v-model:value="confirmOrder.status" placeholder="请选择订单状态">
          <a-select-option v-for="item in CONFIRM_ORDER_STATUS_ARRAY" :key="item.code" :value="item.code">
            {{item.desc}}
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import {notification} from "ant-design-vue";
import axios from "axios";

export default defineComponent({
  name: "confirm-order-view",
  setup() {
    const CONFIRM_ORDER_STATUS_ARRAY = window.CONFIRM_ORDER_STATUS_ARRAY;
    const visible = ref(false);
    let confirmOrder = ref({
      id: undefined,
      memberId: undefined,
      date: undefined,
      trainCode: undefined,
      start: undefined,
      end: undefined,
      dailyTrainTicketId: undefined,
      tickets: undefined,
      status: undefined,
      createTime: undefined,
      updateTime: undefined,
    });
    const confirmOrders = ref([]);
    // 分页的三个属性名是固定的
    const pagination = ref({
      total: 0,
      current: 1,
      pageSize: 10,
    });
    let loading = ref(false);
    const columns = [
    {
      title: '会员id',
      dataIndex: 'memberId',
      key: 'memberId',
    },
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
    },
    {
      title: '车次编号',
      dataIndex: 'trainCode',
      key: 'trainCode',
    },
    {
      title: '出发站',
      dataIndex: 'start',
      key: 'start',
    },
    {
      title: '到达站',
      dataIndex: 'end',
      key: 'end',
    },
    {
      title: '余票ID',
      dataIndex: 'dailyTrainTicketId',
      key: 'dailyTrainTicketId',
    },
    {
      title: '车票',
      dataIndex: 'tickets',
      key: 'tickets',
    },
    {
      title: '订单状态',
      dataIndex: 'status',
      key: 'status',
    },
    {
      title: '操作',
      dataIndex: 'operation'
    }
    ];

    const onAdd = () => {
      confirmOrder.value = {};
      visible.value = true;
    };

    const onEdit = (record) => {
      confirmOrder.value = window.Tool.copy(record);
      visible.value = true;
    };

    const onDelete = (record) => {
      axios.delete("/business/admin/confirm-order/delete/" + record.id).then((response) => {
        const data = response.data;
        if (data.success) {
          notification.success({description: "删除成功！"});
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize,
          });
        } else {
          notification.error({description: data.message});
        }
      });
    };

    const handleOk = () => {
      axios.post("/business/admin/confirm-order/save", confirmOrder.value).then((response) => {
        let data = response.data;
        if (data.success) {
          notification.success({description: "保存成功！"});
          visible.value = false;
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize
          });
        } else {
          notification.error({description: data.message});
        }
      });
    };

    const handleQuery = (param) => {
      if (!param) {
        param = {
          page: 1,
          size: pagination.value.pageSize
        };
      }
      loading.value = true;
      axios.get("/business/admin/confirm-order/query-list", {
        params: {
          page: param.page,
          size: param.size
        }
      }).then((response) => {
        loading.value = false;
        let data = response.data;
        if (data.success) {
          confirmOrders.value = data.content.list;
          // 设置分页控件的值
          pagination.value.current = param.page;
          pagination.value.total = data.content.total;
        } else {
          notification.error({description: data.message});
        }
      });
    };

    const handleTableChange = (page) => {
      // console.log("看看自带的分页参数都有啥：" + JSON.stringify(page));
      pagination.value.pageSize = page.pageSize;
      handleQuery({
        page: page.current,
        size: page.pageSize
      });
    };

    onMounted(() => {
      handleQuery({
        page: 1,
        size: pagination.value.pageSize
      });
    });

    return {
      CONFIRM_ORDER_STATUS_ARRAY,
      confirmOrder,
      visible,
      confirmOrders,
      pagination,
      columns,
      handleTableChange,
      handleQuery,
      loading,
      onAdd,
      handleOk,
      onEdit,
      onDelete,
    };
  },
});
</script>
