package com.doubletuck.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;
import com.doubletuck.parser.ExportTrackingFileWriter;
import com.doubletuck.parser.VirtiusMeetScoreParser;
import com.doubletuck.utils.AppProperties;

import ch.qos.logback.classic.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "export-scores", 
    description = "Exports the sessions provideed.", 
    mixinStandardHelpOptions = true)
public class ExportScoresCommand implements Runnable {
  
  private static final Logger logger = (Logger) LoggerFactory.getLogger(ExportScoresCommand.class);

  @Option(names = "--sessions",
      required = true,
      description = "One or more session ids, each separated by a comma")
  private String sessionIds;

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

    logger.info("Begin Virtius session export of meet scores.");

    List<VirtiusScore> virtiusScoreList = asVirtiusScoreList();

    if (!overwriteExportedFiles) {
      int initialSessionCount = virtiusScoreList.size();
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

    VirtiusMeetScoreParser scoreParser = new VirtiusMeetScoreParser(exportDirectoryPath, virtiusScoreList);
    virtiusScoreList = scoreParser.export();

    logger.info("Writing export processing information to {}.", exportTrackingFilename);
    trackingFileWriter.updateFile(virtiusScoreList);

    logger.info("End Virtius session export of meet scores.");
  }

  List<VirtiusScore> asVirtiusScoreList() {
    List<VirtiusScore> scoreList = new ArrayList<>();

    for (String sessionId : sessionIds.split(",")) {
      sessionId = sessionId.trim();
      VirtiusScore score = new VirtiusScore();
      score.setSessionId(sessionId);
      score.setScoreUrl("https://virti.us/session?s=" + sessionId);
      scoreList.add(score);
    }

    return scoreList;
  }
}
