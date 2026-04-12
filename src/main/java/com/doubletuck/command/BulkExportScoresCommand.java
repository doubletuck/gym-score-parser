package com.doubletuck.command;

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
      logger.error("Export directory '{}' does not exist or is not a directory. Exiting export processing.",
          exportDirectory);
      return;
    }

    Path exportTrackingFilePath = Path.of(exportDirectory, exportTrackingFilename);
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