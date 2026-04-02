package com.doubletuck;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.VirtiusExportStatusWriter;
import com.doubletuck.parser.VirtiusMeetSessionsParser;

@SpringBootApplication
public class GymScoreParserApplication {

  private final static Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(GymScoreParserApplication.class, args);

    VirtiusMeetSessionsParser parser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = parser.getSessionList();
    logger.info("Virtius scores found: {}", virtiusScoreList.size());

    logger.info("Writing to an export file.");
    VirtiusExportStatusWriter writer = new VirtiusExportStatusWriter();
    writer.writeFile(virtiusScoreList);
    logger.info("Export completed.");

    context.close();
    System.exit(0);
  }
}
