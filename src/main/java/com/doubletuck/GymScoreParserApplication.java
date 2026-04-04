package com.doubletuck;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.VirtiusExportStatusWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.parser.VirtiusMeetSessionsParser;

@SpringBootApplication
public class GymScoreParserApplication {

  private final static Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

  @Autowired
  private VirtiusExportStatusWriter writer;

  @Autowired
  private VirtiusMeetScoreParser scoreParser;

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(GymScoreParserApplication.class, args);

    GymScoreParserApplication app = context.getBean(GymScoreParserApplication.class);
    app.run(context);
  }

  public void run(ConfigurableApplicationContext context) {
    VirtiusMeetSessionsParser sessionsParser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = sessionsParser.getSessionList();
    logger.info("Virtius scores found: {}", virtiusScoreList.size());

    LocalDateTime someDate = LocalDateTime.now().minusMonths(2);
    logger.info("Exporting meets from before {}", someDate);
    List<VirtiusScore> sessionsNeedingExport = writer.filterOutExportedSessions(virtiusScoreList, someDate);
    scoreParser.setMeetSessionList(sessionsNeedingExport);
    scoreParser.export();
    sessionsNeedingExport = scoreParser.getMeetSessionList();

    writer.updateFile(sessionsNeedingExport);
    logger.info("Export processing is finished");

    context.close();
    System.exit(0);
  }
}
