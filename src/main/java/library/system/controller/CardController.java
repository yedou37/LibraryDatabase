package library.system.controller;

import entities.Card;
import library.system.LibraryManagementSystem;
import library.system.LibraryManagementSystemImpl;
import queries.ApiResult;
import queries.CardList;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/card")
public class CardController {

    private final LibraryManagementSystem library;

    public CardController() {
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

    @GetMapping
    public CardList showCards() {
        try {
            // 查询所有借书证
            ApiResult result = library.showCards();
            if (!result.ok) {
                throw new RuntimeException("Failed to query cards: " + result.message);
            }
            return (CardList) result.payload;
        } catch (Exception e) {
            throw new RuntimeException("Error querying cards.", e);
        }
    }

    @PostMapping
    public ApiResult registerCard(@RequestBody Card card) {
        try {
            // 注册一个新的借书证
            return library.registerCard(card);
        } catch (Exception e) {
            throw new RuntimeException("Error adding card.", e);
        }
    }

    @PutMapping
    public ApiResult modifyCardInfo(@RequestBody Card card) {
        try {
            // 修改借书证信息
            library.removeCard(card.getCardId());
            return library.registerCard(card);
        } catch (Exception e) {
            throw new RuntimeException("Error modifying card.", e);
        }
    }

    @DeleteMapping("/{cardId}")
    public ApiResult removeCard(@PathVariable int cardId) {
        try {
            // 移除借书证
            return library.removeCard(cardId);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting card.", e);
        }
    }
}
