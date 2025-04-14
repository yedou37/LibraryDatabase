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
      图书信息查询
      <el-input
        v-model="toSearch"
        :prefix-icon="Search"
        style="
          width: 15vw;
          min-width: 150px;
          margin-left: 30px;
          margin-right: 30px;
          float: right;
        "
        clearable
        placeholder="输入关键词搜索"
        @input="handleSearch"
      />
    </div>

    <!-- 展开查询条件按钮 -->
    <div style="text-align: center; margin-top: 20px">
      <el-button type="primary" @click="toggleQueryForm">{{
        isQueryFormVisible ? "收起查询条件" : "展开查询条件"
      }}</el-button>
      <el-button type="warning" @click="clearQueryConditions"
        >清空查询条件</el-button
      >
      <el-button type="success" @click="showAddBookDialog">添加图书</el-button>
      <el-button type="info" @click="showBatchImportDialog"
        >批量导入图书</el-button
      >

      <el-button type="primary" @click="showBorrowDialog">借书</el-button>
    </div>

    <!-- 查询条件 -->
    <div
      v-if="isQueryFormVisible"
      style="width: 80%; margin: 0 auto; padding-top: 2vh"
    >
      <el-form :model="queryConditions" label-width="120px">
        <el-form-item label="图书分类">
          <el-input
            v-model="queryConditions.category"
            placeholder="输入图书分类"
          />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="queryConditions.title" placeholder="输入书名" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="queryConditions.press" placeholder="输入出版社" />
        </el-form-item>
        <el-form-item label="出版年份范围">
          <el-col :span="11">
            <el-input-number
              v-model="queryConditions.minPublishYear"
              :min="1000"
              :max="2100"
              placeholder="最小出版年份"
            />
          </el-col>
          <el-col class="line" :span="2">-</el-col>
          <el-col :span="11">
            <el-input-number
              v-model="queryConditions.maxPublishYear"
              :min="1000"
              :max="2100"
              placeholder="最大出版年份"
            />
          </el-col>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="queryConditions.author" placeholder="输入作者" />
        </el-form-item>
        <el-form-item label="价格范围">
          <el-col :span="11">
            <el-input-number
              v-model="queryConditions.minPrice"
              :min="0"
              :max="10000"
              placeholder="最低价格"
            />
          </el-col>
          <el-col class="line" :span="2">-</el-col>
          <el-col :span="11">
            <el-input-number
              v-model="queryConditions.maxPrice"
              :min="0"
              :max="10000"
              placeholder="最高价格"
            />
          </el-col>
        </el-form-item>
        <el-form-item label="排序字段">
          <el-select
            v-model="queryConditions.sortBy"
            placeholder="选择排序字段"
          >
            <el-option label="图书ID" value="BOOK_ID" />
            <el-option label="书名" value="TITLE" />
            <el-option label="作者" value="AUTHOR" />
            <el-option label="出版年份" value="PUBLISH_YEAR" />
            <el-option label="价格" value="PRICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序顺序">
          <el-select
            v-model="queryConditions.sortOrder"
            placeholder="选择排序顺序"
          >
            <el-option label="升序" value="ASC" />
            <el-option label="降序" value="DESC" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="isLoading" @click="QueryBooks"
            >查询</el-button
          >
        </el-form-item>
      </el-form>
    </div>

    <!-- 错误提示 -->
    <div v-if="hasError" style="color: red; text-align: center; margin: 20px 0">
      查询失败，请检查查询条件或稍后再试
    </div>

    <!-- 结果表格 -->
    <el-table
      v-if="isShow"
      :data="filteredTableData"
      height="600"
      :default-sort="{
        prop: queryConditions.sortBy.toLowerCase(),
        order: queryConditions.sortOrder.toLowerCase() + '-order',
      }"
      :table-layout="'auto'"
      style="
        width: 100%;
        margin-left: 50px;
        margin-top: 30px;
        margin-right: 50px;
        max-width: 80vw;
      "
    >
      <el-table-column prop="bookId" label="图书ID" sortable />
      <el-table-column prop="title" label="书名" sortable />
      <el-table-column prop="author" label="作者" sortable />
      <el-table-column prop="category" label="分类" sortable />
      <el-table-column prop="press" label="出版社" sortable />
      <el-table-column prop="publishYear" label="出版年份" sortable />
      <el-table-column prop="price" label="价格" sortable>
        <template #default="scope">
          {{ scope.row.price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" sortable>
        <template #default="scope">
          {{ scope.row.stock }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button
            size="mini"
            type="primary"
            @click="showModifyBookDialog(scope.row)"
            >修改</el-button
          >
          <el-button
            size="mini"
            type="danger"
            @click="confirmDeleteBook(scope.row.bookId)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 无数据提示 -->
    <div
      v-if="isShow && tableData.length === 0"
      style="text-align: center; margin: 50px 0"
    >
      没有找到相关图书信息
    </div>

    <!-- 添加图书对话框 -->
    <el-dialog title="添加图书" v-model="addBookDialogVisible" width="30%">
      <el-form :model="newBook" label-width="120px">
        <el-form-item label="图书分类">
          <el-input v-model="newBook.category" placeholder="输入图书分类" />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="newBook.title" placeholder="输入书名" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="newBook.press" placeholder="输入出版社" />
        </el-form-item>
        <el-form-item label="出版年份">
          <el-input-number
            v-model="newBook.publishYear"
            :min="1000"
            :max="2100"
            placeholder="输入出版年份"
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="newBook.author" placeholder="输入作者" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number
            v-model="newBook.price"
            :min="0"
            :max="10000"
            placeholder="输入价格"
          />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number
            v-model="newBook.stock"
            :min="0"
            :max="10000"
            placeholder="输入库存"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addBookDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="isLoading" @click="addBook"
            >确 定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 借书对话框 -->
    <el-dialog title="借书" v-model="borrowDialogVisible" width="30%">
      <el-form :model="borrowInfo" label-width="120px">
        <el-form-item label="借书卡卡号">
          <el-input v-model="borrowInfo.cardId" placeholder="输入借书卡卡号" />
        </el-form-item>
        <el-form-item label="书目编号">
          <el-input v-model="borrowInfo.bookId" placeholder="输入书目编号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="borrowDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="isLoading" @click="borrowBook"
            >确 定</el-button
          >
        </span>
      </template>
    </el-dialog>
    <!-- 批量导入图书对话框 -->
    <el-dialog
      title="批量导入图书"
      v-model="batchImportDialogVisible"
      width="50%"
    >
      <el-form label-width="120px">
        <el-form-item label="粘贴图书数据">
          <el-input
            type="textarea"
            :rows="10"
            v-model="batchBooksData"
            placeholder="请粘贴图书数据，每行一个JSON对象"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchImportDialogVisible = false">取 消</el-button>
          <el-button
            type="primary"
            :loading="isLoading"
            @click="batchImportBooks"
            >确 定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 修改图书对话框 -->
    <el-dialog title="修改图书" v-model="modifyBookDialogVisible" width="30%">
      <el-form :model="currentBook" label-width="120px">
        <el-form-item label="图书分类">
          <el-input v-model="currentBook.category" placeholder="输入图书分类" />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="currentBook.title" placeholder="输入书名" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="currentBook.press" placeholder="输入出版社" />
        </el-form-item>
        <el-form-item label="出版年份">
          <el-input-number
            v-model="currentBook.publishYear"
            :min="1000"
            :max="2100"
            placeholder="输入出版年份"
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="currentBook.author" placeholder="输入作者" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number
            v-model="currentBook.price"
            :min="0"
            :max="10000"
            placeholder="输入价格"
          />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number
            v-model="currentBook.stock"
            :min="0"
            :max="10000"
            placeholder="输入库存"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="modifyBookDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="isLoading" @click="modifyBook"
            >确 定</el-button
          >
        </span>
      </template>
    </el-dialog>
  </el-scrollbar>
</template>

<script>
import axios from "axios";
import { Search } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";

export default {
  data() {
    return {
      isShow: false, // 结果表格展示状态
      isLoading: false, // 加载状态
      hasError: false, // 是否显示错误提示
      tableData: [], // 初始数据为空
      toSearch: "", // 待搜索内容(对查询到的结果进行搜索)
      queryConditions: {
        category: null,
        title: null,
        press: null,
        minPublishYear: null,
        maxPublishYear: null,
        author: null,
        minPrice: null,
        maxPrice: null,
        sortBy: "BOOK_ID",
        sortOrder: "ASC",
      },
      isQueryFormVisible: false, // 控制查询条件表单的显示和隐藏
      Search,
      addBookDialogVisible: false, // 添加图书对话框显示状态
      modifyBookDialogVisible: false, // 修改图书对话框显示状态
      newBook: {
        // 添加图书时使用的数据对象
        bookId: null,
        category: "",
        title: "",
        press: "",
        publishYear: null,
        author: "",
        price: null,
        stock: null,
      },
      currentBook: {}, // 当前需要修改的图书对象
      originalStock: null, // 原始库存数量
      borrowDialogVisible: false, // 借书对话框显示状态
      borrowInfo: {
        cardId: null,
        bookId: null,
      },
      batchImportDialogVisible: false, // 批量导入图书对话框显示状态
      batchBooksData: "", // 批量导入的图书数据
    };
  },
  computed: {
    filteredTableData() {
      // 搜索结果实时响应
      return this.tableData.filter((record) => {
        if (this.toSearch === "") return true; // 搜索结果实时响应
        return (
          record.title.includes(this.toSearch) ||
          record.author.includes(this.toSearch) ||
          record.press.includes(this.toSearch)
        );
      });
    },
  },
  methods: {
    async QueryBooks() {
      this.isLoading = true;
      this.hasError = false;
      this.tableData = [];

      try {
        // 将查询条件作为参数传递
        const response = await axios.get("/api/book", {
          params: this.queryConditions,
        });
        this.tableData = response.data.results;
        this.isShow = true;
      } catch (error) {
        console.error("查询失败:", error);
        this.hasError = true;
        ElMessage.error("查询失败，请检查查询条件或稍后再试");
      } finally {
        this.isLoading = false;
      }
    },
    formatDate(unixTimestamp) {
      if (unixTimestamp === 0) {
        return "未归还";
      }
      // 创建一个新的 Date 对象
      const date = new Date(unixTimestamp * 1000);
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
    handleSearch() {
      // 搜索结果实时响应
      this.$nextTick(() => {
        this.filteredTableData; // 触发计算属性重新计算
      });
    },
    toggleQueryForm() {
      // 切换查询条件表单的显示状态
      this.isQueryFormVisible = !this.isQueryFormVisible;
    },
    async addBook() {
      this.isLoading = true;
      this.hasError = false;

      try {
        const response = await axios.post("/api/book/add", this.newBook);
        if (response.data.ok) {
          ElMessage.success("图书添加成功");
          this.QueryBooks(); // 重新查询图书信息
        } else {
          ElMessage.error(response.data.message);
        }
      } catch (error) {
        console.error("添加图书失败:", error);
        this.hasError = true;
        ElMessage.error("添加图书失败，请重试");
      } finally {
        this.isLoading = false;
        this.addBookDialogVisible = false; // 关闭对话框
        this.resetNewBook(); // 重置添加图书的表单
      }
    },
    async deleteBook(bookId) {
      this.isLoading = true;
      this.hasError = false;

      try {
        const response = await axios.delete(`/api/book/delete/${bookId}`);
        if (response.data.ok) {
          ElMessage.success("图书删除成功");
          this.QueryBooks(); // 重新查询图书信息
        } else {
          ElMessage.error(response.data.message);
        }
      } catch (error) {
        console.error("删除图书失败:", error);
        this.hasError = true;
        ElMessage.error("删除图书失败，请重试");
      } finally {
        this.isLoading = false;
      }
    },
    async modifyBook() {
      this.isLoading = true;
      this.hasError = false;

      try {
        const response = await axios.put("/api/book/modify", this.currentBook);
        if (response.data.ok) {
          ElMessage.success("图书信息修改成功");
          // 计算库存变化
          const deltaStock = this.currentBook.stock - this.originalStock;
          if (deltaStock !== 0) {
            await this.incBookStock(this.currentBook.bookId, deltaStock);
          }
          this.QueryBooks(); // 重新查询图书信息
        } else {
          ElMessage.error(response.data.message);
        }
      } catch (error) {
        console.error("修改图书信息失败:", error);
        this.hasError = true;
        ElMessage.error("修改图书信息失败，请重试");
      } finally {
        this.isLoading = false;
        this.modifyBookDialogVisible = false; // 关闭对话框
      }
    },
    resetNewBook() {
      this.newBook = {
        // 重置添加图书的表单
        bookId: null,
        category: "",
        title: "",
        press: "",
        publishYear: null,
        author: "",
        price: null,
        stock: null,
      };
    },
    showAddBookDialog() {
      this.resetNewBook(); // 重置添加图书的表单
      this.addBookDialogVisible = true; // 打开添加图书对话框
    },
    showModifyBookDialog(book) {
      this.currentBook = { ...book }; // 深拷贝图书信息
      this.originalStock = book.stock; // 记录原始库存数量
      this.modifyBookDialogVisible = true; // 打开修改图书对话框
    },
    confirmDeleteBook(bookId) {
      ElMessageBox.confirm("确定要删除该图书吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          this.deleteBook(bookId);
        })
        .catch(() => {
          ElMessage.info("已取消删除");
        });
    },
    clearQueryConditions() {
      this.queryConditions = {
        category: null,
        title: null,
        press: null,
        minPublishYear: null,
        maxPublishYear: null,
        author: null,
        minPrice: null,
        maxPrice: null,
        sortBy: "BOOK_ID",
        sortOrder: "ASC",
      };
      this.handleSearch(); // 重新搜索
    },
    async incBookStock(bookId, deltaStock) {
      this.isLoading = true;
      this.hasError = false;

      try {
        const response = await axios.post(
          `/api/book/incStock/${bookId}/${deltaStock}`
        );
        if (response.data.ok) {
          ElMessage.success("库存修改成功");
        } else {
          ElMessage.error(response.data.message);
        }
      } catch (error) {
        console.error("修改库存失败:", error);
        this.hasError = true;
        ElMessage.error("修改库存失败，请重试");
      } finally {
        this.isLoading = false;
      }
    },
    showBorrowDialog() {
      this.borrowInfo = {
        cardId: null,
        bookId: null,
      };
      this.borrowDialogVisible = true; // 打开借书对话框
    },

    async borrowBook() {
      this.isLoading = true;
      this.hasError = false;

      try {
        const response = await axios.post("/api/book/borrow", this.borrowInfo);
        if (response.data.ok) {
          ElMessage.success("借书成功");
          this.borrowDialogVisible = false; // 关闭借书对话框
          this.QueryBooks(); // 重新查询图书信息
        } else {
          ElMessage.error(response.data.message);
        }
      } catch (error) {
        console.error("借书失败:", error);
        this.hasError = true;
        ElMessage.error("借书失败，请重试");
      } finally {
        this.isLoading = false;
      }
    },

    showBatchImportDialog() {
      this.batchBooksData = ""; // 重置批量导入数据
      this.batchImportDialogVisible = true; // 打开批量导入对话框
    },
    async batchImportBooks() {
      this.isLoading = true;
      this.hasError = false;

      try {
        const lines = this.batchBooksData
          .split("\n")
          .map((line) => line.trim())
          .filter((line) => line);
        const books = lines
          .map((item) => {
            try {
              const book = JSON.parse(item);
              // 验证每本书的基本字段
              if (
                !book.category ||
                !book.title ||
                !book.press ||
                !book.publishYear ||
                !book.author ||
                !book.price ||
                !book.stock
              ) {
                throw new Error(`缺少必需字段: ${item}`);
              }
              return book;
            } catch (e) {
              console.error("解析JSON失败:", e);
              ElMessage.error(`解析JSON失败: ${e.message}`);
              return null;
            }
          })
          .filter((item) => item !== null);

        if (books.length === 0) {
          ElMessage.error("没有有效的图书数据");
          this.isLoading = false;
          return;
        }

        // 发送包含 books 数组的对象
        const response = await axios.post("/api/book/batchAdd", books);
        if (response.data.ok) {
          ElMessage.success("图书批量导入成功");
          this.QueryBooks(); // 重新查询图书信息
        } else {
          ElMessage.error(`批量导入图书失败: ${response.data.message}`);
        }
      } catch (error) {
        console.error("批量导入图书失败:", error);
        this.hasError = true;
        ElMessage.error("批量导入图书失败，请检查数据格式或稍后再试");
      } finally {
        this.isLoading = false;
        this.batchImportDialogVisible = false; // 关闭批量导入对话框
      }
    },
  },
  created() {
    // 初始时查询所有图书
    this.QueryBooks();
  },
};
</script>

<style scoped>
.line {
  text-align: center;
}
</style>
