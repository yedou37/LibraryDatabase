package library;

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
import org.springframework.web.bind.annotation.RestController;

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "utils", "entities", "queries" })
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // 启动 spring boot 应用
            SpringApplication.run(Main.class, args);
            log.info("Starting application...");
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Failed to start application");
        }
    }

}
