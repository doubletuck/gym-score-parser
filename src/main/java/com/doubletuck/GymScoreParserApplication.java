package com.doubletuck;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.ExportTrackingFileWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.parser.VirtiusMeetSessionsParser;

public class GymScoreParserApplication {

  private static final Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

  public static void main(String[] args) throws IOException {
    Properties props = new Properties();
    try (InputStream in = GymScoreParserApplication.class.getClassLoader()
        .getResourceAsStream("application.properties")) {
      if (in != null) {
        props.load(in);
      }
    }

    
    String exportDir = props.getProperty("export.data.directory", "data");
    String exportTrackingFilename = props.getProperty("export.tracking-filename", "meet_scores_export_status.csv");

    logger.info("Begin bulk export of meet scores found on Virtius.");
    VirtiusMeetSessionsParser sessionsParser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = sessionsParser.getSessionList();
    logger.info("{} sessions found on Virtius. Begin score of the sessions.", virtiusScoreList.size());

    VirtiusMeetScoreParser scoreParser = new VirtiusMeetScoreParser(exportDir);
    scoreParser.setMeetSessionList(virtiusScoreList);
    scoreParser.export();
    List<VirtiusScore> processedVirtiusScoreList = scoreParser.getMeetSessionList();

    logger.info("Writing export processing information to {}.", exportTrackingFilename);
    ExportTrackingFileWriter trackingFileWriter = new ExportTrackingFileWriter(exportDir, exportTrackingFilename);
    trackingFileWriter.updateFile(processedVirtiusScoreList);

    logger.info("Bulk export processing is finished.");
  }
}