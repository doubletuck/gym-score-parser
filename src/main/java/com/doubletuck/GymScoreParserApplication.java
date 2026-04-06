package com.doubletuck;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.ExportTrackingFileWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.parser.VirtiusMeetSessionsParser;
import com.doubletuck.utils.AppProperties;

public class GymScoreParserApplication {

  private static final Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

  public static void main(String[] args) {
    AppProperties appProps = AppProperties.getInstance();
    String exportDirProperty = appProps.getExportDataDirectory();
    String exportTrackingFilenameProperty = appProps.getExportTrackingFilename();

    Path exportDirPath = Path.of(exportDirProperty);
    if (!Files.isDirectory(exportDirPath)) {
      logger.error("Export directory '{}' does not exist or is not a directory. Exiting export processing.", exportDirProperty);
      return;
    }
    Path exportTrackingFilePath = Path.of(exportDirProperty, exportTrackingFilenameProperty);

    logger.info("Begin bulk export of meet scores found on Virtius.");
    VirtiusMeetSessionsParser sessionsParser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = sessionsParser.getSessionList();
    logger.info("{} sessions found on Virtius. Begin score of the sessions.", virtiusScoreList.size());

    VirtiusMeetScoreParser scoreParser = new VirtiusMeetScoreParser(exportDirProperty);
    scoreParser.setMeetSessionList(virtiusScoreList);
    scoreParser.export();
    List<VirtiusScore> processedVirtiusScoreList = scoreParser.getMeetSessionList();

    logger.info("Writing export processing information to {}.", exportTrackingFilenameProperty);
    ExportTrackingFileWriter trackingFileWriter = new ExportTrackingFileWriter(exportTrackingFilePath);
    trackingFileWriter.updateFile(processedVirtiusScoreList);

    logger.info("Bulk export processing is finished.");
  }
}