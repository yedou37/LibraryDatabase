package library.system;

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.ConnectConfig;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            // 检查是否已存在相同属性的书
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM book WHERE category = ? AND title = ? AND press = ? AND author = ? AND publish_year = ?");
            checkStmt.setString(1, book.getCategory());
            checkStmt.setString(2, book.getTitle());
            checkStmt.setString(3, book.getPress());
            checkStmt.setString(4, book.getAuthor());
            checkStmt.setInt(5, book.getPublishYear());

            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                // 如果已存在相同属性的书，返回失败结果
                return new ApiResult(false, "Book already exists in the library system.");
            }

            PreparedStatement insertStmt = conn
                    .prepareStatement(
                            "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, book.getCategory());
            insertStmt.setString(2, book.getTitle());
            insertStmt.setString(3, book.getPress());
            insertStmt.setInt(4, book.getPublishYear());
            insertStmt.setString(5, book.getAuthor());
            insertStmt.setDouble(6, book.getPrice());
            insertStmt.setInt(7, book.getStock());

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 1) {
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    book.setBookId(generatedKeys.getInt(1));
                }
                commit(conn);
                return new ApiResult(true, "Book stored successfully");
            } else {
                rollback(conn);
                return new ApiResult(false, "Failed to store book");
            }

        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT stock FROM book WHERE book_id = ?");
            checkStmt.setInt(1, bookId);
            ResultSet res = checkStmt.executeQuery();
            if (!res.next()) {
                rollback(conn);
                return new ApiResult(false, "Invalid book id");
            }
            int curStock = res.getInt("stock");
            int newStock = curStock + deltaStock;
            if (newStock < 0) {
                rollback(conn);
                return new ApiResult(false, "Not enough stock");
            }
            PreparedStatement stmt = conn
                    .prepareStatement("UPDATE book SET stock = ? WHERE book_id = ?");
            stmt.setInt(1, newStock);
            stmt.setInt(2, bookId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 1) {
                commit(conn);
                return new ApiResult(true, "Book stored successfully");
            } else {
                rollback(conn);
                return new ApiResult(false, "Failed to store book");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, "Failed to store book");
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?");
            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            for (Book book : books) {
                // 检查书籍是否已经存在
                checkStmt.setString(1, book.getCategory());
                checkStmt.setString(2, book.getTitle());
                checkStmt.setString(3, book.getPress());
                checkStmt.setInt(4, book.getPublishYear());
                checkStmt.setString(5, book.getAuthor());
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                rs.close();

                if (count > 0) {
                    throw new Exception("Book already exists in the library system." + book.getTitle());
                }
                // 插入新书籍
                insertStmt.setString(1, book.getCategory());
                insertStmt.setString(2, book.getTitle());
                insertStmt.setString(3, book.getPress());
                insertStmt.setInt(4, book.getPublishYear());
                insertStmt.setString(5, book.getAuthor());
                insertStmt.setDouble(6, book.getPrice());
                insertStmt.setInt(7, book.getStock());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            for (Book book : books) {
                if (generatedKeys.next()) {
                    book.setBookId(generatedKeys.getInt(1));
                }
            }
            commit(conn);
            // 执行批量插入

        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Books stored successfully");
    }

    @Override
    public ApiResult removeBook(int bookId) {

        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // 检查 borrow 表中是否存在 book_id 对应的且 return_time = 0 的记录
            PreparedStatement checkStmt = conn
                    .prepareStatement("SELECT COUNT(*) FROM borrow WHERE book_id = ? AND return_time = 0");
            checkStmt.setInt(1, bookId);
            ResultSet checkRs = checkStmt.executeQuery();
            checkRs.next();
            int count = checkRs.getInt(1);

            if (count > 0) {
                throw new Exception("Book with ID " + bookId + " cannot be removed because it has not been returned.");
            }

            // 如果没有未归还的记录，则删除书籍
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM book WHERE book_id = ?");
            stmt.setInt(1, bookId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("Book with ID " + bookId + " not found.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Book removed successfully");
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            String sql = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            stmt.setDouble(6, book.getPrice());
            stmt.setInt(7, book.getBookId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("Book with ID " + book.getBookId() + " not found.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Book information modified successfully");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        List<Book> resultBooks = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM book WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (conditions.getCategory() != null) {
                sqlBuilder.append(" AND category = ?");
                params.add(conditions.getCategory());
            }
            if (conditions.getTitle() != null) {
                sqlBuilder.append(" AND title LIKE ?");
                params.add("%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                sqlBuilder.append(" AND press LIKE ?");
                params.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                sqlBuilder.append(" AND publish_year >= ?");
                params.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                sqlBuilder.append(" AND publish_year <= ?");
                params.add(conditions.getMaxPublishYear());
            }
            if (conditions.getAuthor() != null) {
                sqlBuilder.append(" AND author LIKE ?");
                params.add("%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                sqlBuilder.append(" AND price >= ?");
                params.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                sqlBuilder.append(" AND price <= ?");
                params.add(conditions.getMaxPrice());
            }

            // 如果没有指定排序字段，则默认按 book_id 升序排序
            if (conditions.getSortBy() == null) {
                conditions.setSortBy(Book.SortColumn.BOOK_ID);
                conditions.setSortOrder(SortOrder.ASC);
            }

            sqlBuilder.append(" ORDER BY ").append(conditions.getSortBy().toString().toLowerCase());
            sqlBuilder.append(" ").append(conditions.getSortOrder().toString());
            sqlBuilder.append(", book_id ASC");
            PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                }
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book();
                book.setBookId(rs.getInt("book_id"));
                book.setCategory(rs.getString("category"));
                book.setTitle(rs.getString("title"));
                book.setPress(rs.getString("press"));
                book.setPublishYear(rs.getInt("publish_year"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));
                book.setStock(rs.getInt("stock"));
                resultBooks.add(book);
            }

            BookQueryResults results = new BookQueryResults(resultBooks);
            commit(conn);
            return new ApiResult(true, "Query book successfully", results);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // 检查book表中对应的书目stock是否不为0
            PreparedStatement checkStockStmt = conn.prepareStatement(
                    "SELECT stock FROM book WHERE book_id = ? FOR UPDATE",
                    Statement.RETURN_GENERATED_KEYS);
            checkStockStmt.setInt(1, borrow.getBookId());
            ResultSet resultSet = checkStockStmt.executeQuery();

            if (!resultSet.next() || resultSet.getInt("stock") == 0) {
                throw new Exception("Book is out of stock.");
            }

            // 检查读者是否已经借了同一本书且未归还
            PreparedStatement checkBorrowStmt = conn.prepareStatement(
                    "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0 FOR UPDATE",
                    Statement.RETURN_GENERATED_KEYS);
            checkBorrowStmt.setInt(1, borrow.getCardId());
            checkBorrowStmt.setInt(2, borrow.getBookId());
            resultSet = checkBorrowStmt.executeQuery();

            if (resultSet.next()) {
                throw new Exception("You have already borrowed this book and have not returned it yet.");
            }
            borrow.resetBorrowTime(); // 设置借书时间为当前时间
            borrow.setReturnTime(0); // 设置还书时间为0
            PreparedStatement borrowStmt = conn.prepareStatement(
                    "INSERT INTO borrow (card_id, book_id, borrow_time, return_time) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);

            borrowStmt.setInt(1, borrow.getCardId());
            borrowStmt.setInt(2, borrow.getBookId());
            borrowStmt.setLong(3, borrow.getBorrowTime());
            borrowStmt.setLong(4, borrow.getReturnTime());

            int affectedRows = borrowStmt.executeUpdate();
            if (affectedRows == 1) {
                boolean updateStock = incBookStock(borrow.getBookId(), -1).ok ? true : false;
                if (updateStock) {
                    commit(conn);
                    return new ApiResult(true, "Book borrowed successfully");
                } else {
                    throw new Exception("Failed to update book stock.");
                }

            } else {
                throw new Exception("Failed to borrow book.");
            }

        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // 检查是否有对应的借阅记录且未归还
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0 AND borrow_time = ? FOR UPDATE");
            checkStmt.setInt(1, borrow.getCardId());
            checkStmt.setInt(2, borrow.getBookId());
            checkStmt.setLong(3, borrow.getBorrowTime());
            ResultSet resultSet = checkStmt.executeQuery();

            if (!resultSet.next()) {
                throw new Exception("No matching borrow record found or the book has already been returned.");
            }

            // 从结果集中获取借书时间
            long borrowTimeFromDB = resultSet.getLong("borrow_time");
            if (borrow.getReturnTime() == 0) {
                borrow.resetReturnTime();
            }

            // 检查归还时间是否晚于或等于借书时间
            if (borrow.getReturnTime() <= borrowTimeFromDB) {
                throw new Exception("Return time should be later than or equal to borrow time.");
            }
            // 更新borrow表中的归还时间
            PreparedStatement returnStmt = conn.prepareStatement(
                    "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND borrow_time = ?");
            returnStmt.setLong(1, borrow.getReturnTime());
            returnStmt.setInt(2, borrow.getCardId());
            returnStmt.setInt(3, borrow.getBookId());
            returnStmt.setLong(4, borrow.getBorrowTime());

            int affectedRows = returnStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("Failed to update return time.");
            }

            // 增加book表中对应的书籍库存
            incBookStock(borrow.getBookId(), 1);
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Book returned successfully");
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        List<BorrowHistories.Item> borrowItems = new ArrayList<>();

        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM borrow WHERE card_id = ?");
            sqlBuilder.append(" ORDER BY borrow_time DESC, book_id ASC");

            PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString());
            stmt.setInt(1, cardId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                long borrowTime = rs.getLong("borrow_time");
                long returnTime = rs.getLong("return_time");

                // 获取书的信息
                PreparedStatement bookStmt = conn.prepareStatement("SELECT * FROM book WHERE book_id = ?");
                bookStmt.setInt(1, bookId);
                ResultSet bookRs = bookStmt.executeQuery();

                Book book = new Book();
                if (bookRs.next()) {
                    book.setBookId(bookRs.getInt("book_id"));
                    book.setCategory(bookRs.getString("category"));
                    book.setTitle(bookRs.getString("title"));
                    book.setPress(bookRs.getString("press"));
                    book.setPublishYear(bookRs.getInt("publish_year"));
                    book.setAuthor(bookRs.getString("author"));
                    book.setPrice(bookRs.getDouble("price"));
                    book.setStock(bookRs.getInt("stock"));
                }
                Borrow borrow = new Borrow(bookId, cardId);
                borrow.setBorrowTime(borrowTime);
                borrow.setReturnTime(returnTime);
                borrowItems.add(new BorrowHistories.Item(cardId, book, borrow));
            }

            BorrowHistories borrowHistories = new BorrowHistories(borrowItems);
            commit(conn);
            return new ApiResult(true, "Borrow history retrieved successfully", borrowHistories);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO card (name, department, type) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, card.getName());
            insertStmt.setString(2, card.getDepartment());
            insertStmt.setString(3, card.getType().getStr());

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 1) {
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    card.setCardId(generatedKeys.getInt(1));
                }
                commit(conn);
                return new ApiResult(true, "Card registered successfully");
            } else {
                rollback(conn);
                return new ApiResult(false, "Failed to register card");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // 检查是否有未归还的书
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM borrow WHERE card_id = ? AND return_time = 0");
            checkStmt.setInt(1, cardId);
            ResultSet resultSet = checkStmt.executeQuery();

            if (resultSet.next()) {
                throw new Exception("Cannot remove the card as there are books that have not been returned.");
            }

            // 删除借书卡
            PreparedStatement removeStmt = conn.prepareStatement("DELETE FROM card WHERE card_id = ?");
            removeStmt.setInt(1, cardId);

            int affectedRows = removeStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("Card with ID " + cardId + " not found.");
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Card removed successfully");
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        List<Card> cards = new ArrayList<>();

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card ORDER BY card_id ASC");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int cardId = rs.getInt("card_id");
                String name = rs.getString("name");
                String department = rs.getString("department");
                Card.CardType type = Card.CardType.values(rs.getString("type"));

                Card card = new Card(cardId, name, department, type);
                cards.add(card);
            }

            CardList cardList = new CardList(cards);
            commit(conn);
            return new ApiResult(true, "Cards retrieved successfully", cardList);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM card WHERE card_id = ? FOR UPDATE");
            checkStmt.setInt(1, card.getCardId());
            ResultSet checkRes = checkStmt.executeQuery();
            if (!checkRes.next()) {
                throw new Exception("Card with ID " + card.getCardId() + " not found.");
            }
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?");
            updateStmt.setString(1, card.getName());
            updateStmt.setString(2, card.getDepartment());
            updateStmt.setString(3, card.getType().getStr());
            updateStmt.setInt(4, card.getCardId());
            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("Failed to update card information.");
            }
            commit(conn);
            return new ApiResult(true, "Card information updated successfully");
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

}
