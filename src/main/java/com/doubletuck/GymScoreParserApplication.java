package com.doubletuck;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.ExportTrackingFileWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.parser.VirtiusMeetSessionsParser;
import com.doubletuck.utils.AppProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GymScoreParserApplication {

  private static final Logger logger = LoggerFactory.getLogger(GymScoreParserApplication.class);

  public static void main(String[] args) {
    GymScoreParserApplication app = new GymScoreParserApplication();
    app.setExportDataDirectory(AppProperties.getInstance().getExportDataDirectory());
    app.setExportTrackingFilename(AppProperties.getInstance().getExportTrackingFilename());

    app.bulkExportScores();
  }

  private String exportDataDirectory;
  private String exportTrackingFilename;
  private boolean overwriteExportedFiles;
  
  private GymScoreParserApplication() {
  }
  
  private void bulkExportScores() {
    
    Path exportDirectoryPath = Path.of(exportDataDirectory);
    if (!Files.isDirectory(exportDirectoryPath)) {
      logger.error("Export directory '{}' does not exist or is not a directory. Exiting export processing.",
      exportDataDirectory);
      return;
    }
    
    Path exportTrackingFilePath = Path.of(exportDataDirectory, exportTrackingFilename);
    ExportTrackingFileWriter trackingFileWriter = new ExportTrackingFileWriter(exportTrackingFilePath);

    logger.info("Begin bulk export of meet scores found on Virtius.");
    VirtiusMeetSessionsParser sessionsParser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = sessionsParser.getSessionList();
    logger.info("{} sessions found on Virtius. Begin score of the sessions.", virtiusScoreList.size());

    if (!overwriteExportedFiles) {
      List<VirtiusScore> exportedSessions = trackingFileWriter.getRowsWithExportedStatus();
      Set<String> exportedSessionIds = exportedSessions.stream()
          .map(VirtiusScore::getSessionId)
          .collect(Collectors.toSet());
      virtiusScoreList.removeIf(s -> exportedSessionIds.contains(s.getSessionId()));
    }

    VirtiusMeetScoreParser scoreParser = new VirtiusMeetScoreParser(exportDirectoryPath, virtiusScoreList);
    virtiusScoreList = scoreParser.export();

    logger.info("Writing export processing information to {}.", exportTrackingFilename);
    trackingFileWriter.updateFile(virtiusScoreList);

    logger.info("Bulk export processing is finished.");
  }
}