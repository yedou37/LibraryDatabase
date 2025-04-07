import utils.ConnectConfig;
import utils.DatabaseConnector;
import utils.RandomData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.*;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.util.Assert;

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            LibraryManagementSystem lms = new LibraryManagementSystemImpl(connector);
            ApiResult restartRes = lms.resetDatabase();
            if (!restartRes.ok)
                log.severe("failed to reset database" + restartRes.message.toString());
            Scanner scanner = new Scanner(System.in);
            String command = "";
            while (!(command == "exit")) {
                System.out.println(
                        "请输入命令 (storeRandomBook, storeBook, registerCard, borrowBook, returnBook, showBorrowHistory, showCards, resetDatabase, exit): ");
                command = scanner.nextLine().trim().toLowerCase();
                switch (command) {
                    case "storeRamdombook": {
                        Book book = RandomData.randomBook();
                        ApiResult storeRes = lms.storeBook(book);
                        if (storeRes.ok) {
                            log.info("Success to store book. " + storeRes.toString());
                        } else {
                            log.severe("Failed to store book. " + storeRes.message.toString());
                        }
                        break;
                    }
                    case "storebook": {
                        Book book = new Book();
                        System.out.println("请输入书名: ");
                        book.setTitle(scanner.nextLine().trim());
                        System.out.println("请输入作者: ");
                        book.setAuthor(scanner.nextLine().trim());
                        System.out.println("请输入出版社: ");
                        book.setPress(scanner.nextLine().trim());
                        System.out.println("请输入出版年份: ");
                        book.setPublishYear(Integer.parseInt(scanner.nextLine().trim()));
                        System.out.println("请输入价格: ");
                        book.setPrice(Double.parseDouble(scanner.nextLine().trim()));
                        System.out.println("请输入分类: ");
                        book.setCategory(scanner.nextLine().trim());
                        System.out.println("请输入库存: ");
                        book.setStock(Integer.parseInt(scanner.nextLine().trim()));
                        ApiResult storeRes = lms.storeBook(book);
                        if (storeRes.ok) {
                            log.info("Success to store book. " + storeRes.toString());
                        } else {
                            log.severe("Failed to store book. " + storeRes.message.toString());
                        }
                        break;
                    }
                    case "registercard": {
                        Card card = new Card();
                        System.out.println("请输入卡号: ");
                        card.setCardId(Integer.parseInt(scanner.nextLine().trim()));
                        System.out.println("请输入姓名: ");
                        card.setName(scanner.nextLine().trim());
                        System.out.println("请输入部门: ");
                        card.setDepartment(scanner.nextLine().trim());
                        System.out.println("请输入卡类型 (0: 学生卡, 1: 教职工卡): ");
                        int type = Integer.parseInt(scanner.nextLine().trim());
                        if (type == 0) {
                            card.setType(Card.CardType.Student);
                        } else if (type == 1) {
                            card.setType(Card.CardType.Teacher);
                        } else {
                            log.severe("Invalid card type.");
                            break;
                        }
                        ApiResult registerRes = lms.registerCard(card);
                        if (registerRes.ok) {
                            log.info("Success to register card. " + registerRes.toString());
                        } else {
                            log.severe("Failed to register card. " + registerRes.message.toString());
                        }
                    }
                    case "showcards": {
                        ApiResult cardsRes = lms.showCards();
                        if (cardsRes.ok) {
                            log.info("所有借阅卡信息: " + cardsRes.toString());
                        } else {
                            log.severe("获取借阅卡信息失败. " + cardsRes.message.toString());
                        }
                        break;
                    }

                }

            }
            // release database connection handler
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

}
