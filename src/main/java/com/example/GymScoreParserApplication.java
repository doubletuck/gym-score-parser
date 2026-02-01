package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class GymScoreParserApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(GymScoreParserApplication.class, args);

        // Get the parser bean and run it
        GymScoreParser parser = context.getBean(GymScoreParser.class);
        parser.parseAndPrint();

        // Exit the application after processing
        context.close();
        System.exit(0);
    }
}
