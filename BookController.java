
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import entities.Book;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final LibraryManagementSystem library;

    public BookController() {
        try {
            // 解析连接配置
            ConnectConfig conf = new ConnectConfig();
            // 创建数据库连接器
            DatabaseConnector connector = new DatabaseConnector(conf);
            // 连接数据库
            boolean connStatus = connector.connect();
            if (!connStatus) {
                throw new SQLException("Failed to connect to database.");
            }
            // 初始化图书管理系统
            this.library = new LibraryManagementSystemImpl(connector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the controller.", e);
        }
    }

    @GetMapping("/query")
    public BookQueryResults queryBooks(@RequestBody BookQueryConditions queryConditions) {
        try {
            // 查询图书
            ApiResult result = library.queryBook(queryConditions);
            if (!result.ok) {
                throw new RuntimeException("Failed to query books: " + result.message);
            }
            return (BookQueryResults) result.payload;
        } catch (Exception e) {
            throw new RuntimeException("Error querying books.", e);
        }
    }

    // 你可以根据需要添加其他的方法，例如添加图书、删除图书等

    // 示例：添加图书
    @GetMapping("/add")
    public ApiResult addBook(@RequestBody Book book) {
        try {
            // 添加图书
            return library.storeBook(book);
        } catch (Exception e) {
            throw new RuntimeException("Error adding book.", e);
        }
    }
}
