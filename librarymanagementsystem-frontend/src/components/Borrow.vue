<template>
  <el-scrollbar height="100%" style="width: 100%">
    <!-- 标题和搜索框 -->
    <div
      style="
        margin-top: 20px;
        margin-left: 40px;
        font-size: 2em;
        font-weight: bold;
      "
    >
      借书记录查询
    </div>

    <!-- 查询框 -->
    <div style="width: 30%; margin: 0 auto; padding-top: 5vh">
      <el-input
        v-model="toQuery"
        style="display: inline"
        placeholder="输入借书证ID"
      ></el-input>
      <el-button
        style="margin-left: 10px"
        type="primary"
        :loading="isLoading"
        @click="QueryBorrows"
        >查询</el-button
      >
    </div>

    <!-- 错误提示 -->
    <div v-if="hasError" style="color: red; text-align: center; margin: 20px 0">
      查询失败，请检查借书证ID或稍后再试
    </div>

    <!-- 结果表格 -->
    <el-table
      v-if="isShow"
      :data="filteredTableData"
      height="600"
      :default-sort="{ prop: 'borrowTime', order: 'ascending' }"
      :table-layout="'auto'"
      style="
        width: 100%;
        margin-left: 50px;
        margin-top: 30px;
        margin-right: 50px;
        max-width: 80vw;
      "
    >
      <el-table-column prop="cardId" label="借书证ID" />
      <el-table-column prop="bookId" label="图书ID" sortable />
      <el-table-column label="借出时间" sortable>
        <template #default="scope">
          {{ formatDate(scope.row.borrowTime) }}
        </template>
      </el-table-column>
      <el-table-column label="归还时间" sortable>
        <template #default="scope">
          {{ formatDate(scope.row.returnTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button
            v-if="scope.row.returnTime === 0"
            size="mini"
            type="primary"
            @click="
              returnBook(
                scope.row.bookId,
                scope.row.cardId,
                scope.row.borrowTime
              )
            "
            >还书</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 无数据提示 -->
    <div
      v-if="isShow && tableData.length === 0"
      style="text-align: center; margin: 50px 0"
    >
      没有找到相关借书记录
    </div>
  </el-scrollbar>
</template>

<script>
import axios from "axios";
import { Search } from "@element-plus/icons-vue";

export default {
  data() {
    return {
      isShow: false, // 结果表格展示状态
      isLoading: false, // 加载状态
      hasError: false, // 是否显示错误提示
      tableData: [
        {
          // 初始示例数据
          cardId: 1,
          bookId: 1,
          borrowTime: "2024.03.04 21:48",
          returnTime: 0,
        },
      ],
      toQuery: "", // 待查询内容(对某一借书证号进行查询)
      Search,
    };
  },
  computed: {
    filteredTableData() {
      // 搜索结果实时响应
      return this.tableData.filter((record) => {
        if (this.toQuery === "") return true; // 搜索结果实时响应
        return (
          String(record.bookId).includes(this.toQuery) ||
          String(record.borrowTime).includes(this.toQuery) ||
          String(record.returnTime).includes(this.toQuery)
        );
      });
    },
  },
  methods: {
    async QueryBorrows() {
      this.isLoading = true;
      this.hasError = false;
      this.tableData = [];

      try {
        // 将 cardID 作为路径变量传递
        const response = await axios.get(`/api/borrow/${this.toQuery}`);
        this.tableData = response.data;
        this.isShow = true;
      } catch (error) {
        console.error("查询失败:", error);
        this.hasError = true;
      } finally {
        this.isLoading = false;
      }
    },
    formatDate(unixTimestamp) {
      if (unixTimestamp === 0) {
        return "未归还";
      }
      // 创建一个新的 Date 对象
      const date = new Date(unixTimestamp);
      // 获取日期和时间并格式化
      return date.toLocaleString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
      });
    },
    async returnBook(bookId, cardId, borrowTime) {
      this.isLoading = true;
      this.hasError = false;

      try {
        // 向后端发送还书请求
        const response = await axios.post("/api/book/return", {
          bookId,
          cardId,
          borrowTime,
        });
        if (response.data.ok) {
          this.$message.success("还书成功");
          this.QueryBorrows(); // 刷新借书记录
        } else {
          this.$message.error(response.data.message);
        }
      } catch (error) {
        console.error("还书失败:", error);
        this.hasError = true;
        this.$message.error("还书失败，请重试");
      } finally {
        this.isLoading = false;
      }
    },
  },
};
</script>
