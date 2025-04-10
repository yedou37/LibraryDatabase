package library.system.controller;

import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import queries.BorrowHistories;
import queries.CardList;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import entities.Book;
import entities.Card;
import library.system.LibraryManagementSystem;
import library.system.LibraryManagementSystemImpl;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class BookController {
    @Resource
    private LibraryManagementSystem library;
    private static final Logger log = Logger.getLogger(BookController.class.getName());

    @PostConstruct
    public void init() {
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
        resetDatabase();

    }

    public BookController() {
        init();
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

    // 新增Card相关接口
    @PostMapping("/card")
    public ApiResult registerCard(@RequestBody Card card) {
        try {
            // 新建借书证
            return library.registerCard(card);
        } catch (Exception e) {
            throw new RuntimeException("Error registering card.", e);
        }
    }

    @DeleteMapping("/card/{cardId}")
    public ApiResult removeCard(@PathVariable int cardId) {
        try {
            // 删除借书证
            return library.removeCard(cardId);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting card.", e);
        }
    }

    @GetMapping("/card")
    public CardList showCards() {

        log.info("showCards");
        try {
            // 查询所有借书证
            ApiResult result = library.showCards();
            if (!result.ok) {
                throw new RuntimeException("Failed to show cards: " + result.message);
            }
            return (CardList) result.payload;
        } catch (Exception e) {
            throw new RuntimeException("Error showing cards.", e);

        }

    }

    @PutMapping("/card")
    public boolean modifyCard(@RequestBody Card card) {
        try {

            ApiResult resModify = library.modifyCardInfo(card);
            if (!resModify.ok) {
                throw new RuntimeException("Failed to modify card: " + resModify.message);
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Error modifying card.", e);
        }
    }

    @GetMapping("/borrow/{cardId}")
    public List<BorrowHistories.Item> showBorrowHis(@PathVariable int cardId) {
        try {
            // 查询借阅历史
            ApiResult result = library.showBorrowHistory(cardId);
            if (!result.ok) {
                throw new RuntimeException("Failed to show borrow history: " + result.message);
            }
            BorrowHistories res = (BorrowHistories) result.payload;
            return res.getItems();
        } catch (Exception e) {
            throw new RuntimeException("Error showing borrow history.", e);
        }
    }
}