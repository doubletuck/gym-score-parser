package com.doubletuck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.doubletuck.parser.GymScoreParser;
// import com.doubletuck.parser.GymScoreClipboardParser;

@SpringBootApplication
public class GymScoreParserApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(GymScoreParserApplication.class, args);

        // Get the parser bean and run it
        GymScoreParser parser = context.getBean(GymScoreParser.class);

        // GymScoreClipboardParser parser = context.getBean(GymScoreClipboardParser.class);
        parser.parseAndPrint();

        // Exit the application after processing
        context.close();
        System.exit(0);
    }
}
