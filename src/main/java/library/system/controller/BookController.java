package library.system.controller;

import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import entities.Book;
import library.system.LibraryManagementSystem;
import library.system.LibraryManagementSystemImpl;

import java.sql.SQLException;

@RestController
@RequestMapping("/books")
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

    @PostMapping("/add")
    public ApiResult addBook(@RequestBody Book book) {
        try {
            // 添加图书
            return library.storeBook(book);
        } catch (Exception e) {
            throw new RuntimeException("Error adding book.", e);
        }
    }

    @DeleteMapping("/delete/{bookId}")
    public ApiResult deleteBook(@PathVariable int bookId) {
        try {
            // 删除图书
            return library.removeBook(bookId);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting book.", e);
        }
    }

    @PutMapping("/modify")
    public ApiResult modifyBook(@RequestBody Book book) {
        try {
            // 修改图书信息
            return library.modifyBookInfo(book);
        } catch (Exception e) {
            throw new RuntimeException("Error modifying book.", e);
        }
    }

    @GetMapping("/reset")
    public ApiResult resetDatabase() {
        try {
            // 重置数据库
            return library.resetDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Error resetting database.", e);
        }
    }
}
