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

            Set<Book> bookSet = new HashSet<>();
            while (bookSet.size() < 50) {
                bookSet.add(RandomData.randomBook());
            }
            List<Book> bookList = new ArrayList<>(bookSet);
            ApiResult res = lms.storeBook(bookList);
            if (res.ok) {
                log.info("Success to store books. " + res.message.toString());
            } else {
                log.severe("Failed to store books. " + res.message.toString());
            }

            List<Card> cardList = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                Card c = new Card();
                c.setName(String.format("User%05d", i));
                c.setDepartment(RandomData.randomDepartment());
                c.setType(Card.CardType.random());
                cardList.add(c);
                if (lms.registerCard(c).ok) {
                    log.info("Success to register card. " + c.toString());
                } else {
                    log.severe("Failed to register card. " + c.toString());
                }
            }
            List<Borrow> borrowList = new ArrayList<>();
            PriorityQueue<Long> mills = new PriorityQueue<>();
            for (int i = 0; i < 100 * 2; i++) {
                mills.add(RandomData.randomTime());
            }
            for (int i = 0; i < 100;) {
                Book b = bookList.get(RandomUtils.nextInt(0, 50));
                if (b.getStock() == 0) {
                    continue;
                }
                i++;
                Card c = cardList.get(RandomUtils.nextInt(0, 50));
                Borrow r = new Borrow();
                r.setCardId(c.getCardId());
                r.setBookId(b.getBookId());
                r.setBorrowTime(mills.poll());
                r.setReturnTime(mills.poll());
                if (lms.borrowBook(r).ok) {
                    log.info("Success to borrow book. " + r.toString());
                } else {
                    log.severe("Failed to borrow book. " + r.toString());
                }

                if (lms.returnBook(r).ok) {
                    log.info("Success to return book. " + r.toString());
                } else {
                    log.severe("Failed to return book. " + r.toString());
                }

                borrowList.add(r);
            }
            /*
             * if (restartRes.ok) {
             * log.info("OK");
             * }
             * Book book = new Book("Computer Science", "Introduction to Algorithms",
             * "MIT Press", 2009, "CLRS", 36.8,
             * 100);
             * ApiResult storeRes = lms.storeBook(book);
             * if (storeRes.ok) {
             * log.info("Success to store book. " + storeRes.message.toString());
             * }
             * BookQueryConditions conditions = new BookQueryConditions();
             * conditions.setCategory("Computer Science");
             * ApiResult result = lms.queryBook(conditions);
             * if (result.ok) {
             * log.info("Success to query books. " + result.payload.toString());
             * BookQueryResults queryResults = (BookQueryResults) result.payload;
             * List<Book> books = (List<Book>) queryResults.getResults();
             * log.info("Books: " + books.get(0).toString());
             * } else {
             * log.severe("Failed to query books. " + result.message);
             * }
             */
            // release database connection handler
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
