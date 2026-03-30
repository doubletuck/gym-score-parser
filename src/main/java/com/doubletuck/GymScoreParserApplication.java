package com.doubletuck;

import com.doubletuck.model.VirtiusScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.doubletuck.parser.VirtiusMeetSessionsParser;

import java.util.List;

@SpringBootApplication
public class GymScoreParserApplication {

    private final static Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(GymScoreParserApplication.class, args);

        VirtiusMeetSessionsParser parser = new VirtiusMeetSessionsParser();
        List<VirtiusScore> virtiusScoreList = parser.getSessionList();
        for (VirtiusScore score : virtiusScoreList) {
            logger.info("{}", score);
        }

        context.close();
        System.exit(0);
    }
}
