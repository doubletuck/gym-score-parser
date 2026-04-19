package com.doubletuck.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.ExportTrackingFileWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.parser.VirtiusMeetSessionsParser;
import com.doubletuck.utils.AppProperties;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "bulk-export-scores",
    description = "Exports all scores listed on the Virtius page.",
    mixinStandardHelpOptions = true
)
public class BulkExportScoresCommand implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(BulkExportScoresCommand.class);

  @Option(names = "--export-directory",
      description = "Directory where exported files are written. Defaults to the export.data.directory value in application.properties.")
  private String exportDirectory;

  @Option(names = "--export-tracking-filename",
      description = "Filename for the export tracking CSV. Defaults to the export.tracking-filename value in application.properties.")
  private String exportTrackingFilename;

  @Option(names = "--overwrite-export-files",
      description = "If present, existing exported files are overwritten. Otherwise, already-exported meets are skipped.")
  private boolean overwriteExportedFiles;

  @Override
  public void run() {
    AppProperties props = AppProperties.getInstance();
    if (exportDirectory == null) {
      exportDirectory = props.getExportDataDirectory();
    }
    if (exportTrackingFilename == null) {
      exportTrackingFilename = props.getExportTrackingFilename();
    }

    Path exportDirectoryPath = Path.of(exportDirectory);
    if (!Files.isDirectory(exportDirectoryPath)) {
      logger.info("Export directory '{}' does not exist. Creating the directory.", exportDirectory);
      try {
        Files.createDirectories(exportDirectoryPath);
        logger.info("Export directory '{}' created.", exportDirectory);
      } catch (IOException e) {
        logger.error("Export directory '{}' could not be created. Exiting export processing.", exportDirectory, e);
        return;
      }
    }

    Path exportTrackingFilePath = Path.of(exportDirectory, exportTrackingFilename);
    ExportTrackingFileWriter trackingFileWriter = new ExportTrackingFileWriter(exportTrackingFilePath);

    Instant startTime = Instant.now();
    logger.info("Bulk score export processing beginning at {}.", startTime);

    VirtiusMeetSessionsParser sessionsParser = new VirtiusMeetSessionsParser();
    List<VirtiusScore> virtiusScoreList = sessionsParser.getSessionList();
    logger.info("{} Virtius sessions found.", virtiusScoreList.size());
    
    int initialSessionCount = virtiusScoreList.size();
    if (!overwriteExportedFiles) {
      List<VirtiusScore> exportedSessions = trackingFileWriter.getRowsWithExportedStatus();
      Set<String> exportedSessionIds = exportedSessions.stream()
          .map(VirtiusScore::getSessionId)
          .collect(Collectors.toSet());
      virtiusScoreList.removeIf(s -> exportedSessionIds.contains(s.getSessionId()));
      int skipSessionsCount = initialSessionCount - virtiusScoreList.size();
      if (skipSessionsCount > 0) {
        logger.info("{} Virtius sessions already exported. Skip processing for those sessions.", skipSessionsCount);
      }
    }

    if (virtiusScoreList.size() > 0) {
      VirtiusMeetScoreParser scoreParser = new VirtiusMeetScoreParser(exportDirectoryPath, virtiusScoreList);
      virtiusScoreList = scoreParser.export();
      trackingFileWriter.updateFile(virtiusScoreList);
      logger.info("Wrote export processing information to {}.", exportTrackingFilename);
    }


    Instant endTime = Instant.now();
    Duration duration = Duration.between(startTime, endTime);

    Map<VirtiusScore.ExportStatus, Long> statusCounts = virtiusScoreList.stream()
        .collect(Collectors.groupingBy(VirtiusScore::getExportStatus, Collectors.counting()));
    logger.info("Export processing summary - Total sessions found: {}", initialSessionCount);
    logger.info("Export processing summary - Total sessions processed: {}", virtiusScoreList.size());
    for (VirtiusScore.ExportStatus status : VirtiusScore.ExportStatus.values()) {
      logger.info("Export processing summary - Sessions {} count: {}", status, statusCounts.getOrDefault(status, 0L));
    }
    logger.info("Export processing summary - Processing begin time: {}", startTime);
    logger.info("Export processing summary - Processing end time: {}", endTime);
    logger.info("Export processing summary - Processing duration: {}m {}s", duration.toMinutesPart(), duration.toSecondsPart());
    logger.info("Export processing summary - Finished");
  }
}