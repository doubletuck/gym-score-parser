package com.doubletuck.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

public class VirtiusExportStatusWriter {

  private static final Logger logger = LoggerFactory.getLogger(VirtiusExportStatusWriter.class);

  private static final Path OUTPUT_FILE = Path.of("data/meet_scores_export_status.csv");

  private static final String[] HEADERS = {
      "meet_date",
      "meet_name",
      "score_site",
      "session_id",
      "score_url",
      "is_wag",
      "export_status",
      "export_filename",
      "export_date",
      "export_message"
  };

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public void create(List<VirtiusScore> sessions) {
    try {
      Files.createDirectories(OUTPUT_FILE.getParent());
    } catch (IOException e) {
      logger.error("An error when creating");
      return;
    }

    try (
      BufferedWriter writer = Files.newBufferedWriter(
            OUTPUT_FILE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
      CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get());
    ) {
      int rowCount = 0;
      for (VirtiusScore session : sessions) {
        List<String> row = buildRow(session);
        printer.printRecord(row);
        rowCount++;
      }
      printer.flush();
      logger.info("Wrote {} records to {}.", rowCount, OUTPUT_FILE);
    } catch (IOException e) {
      logger.error("Error upserting export status CSV", e);
    }
  }

  private List<String> buildRow(VirtiusScore session) {
    return List.of(
        session.getMeetDate() == null ? "" : session.getMeetDate().format(DATE_FORMATTER),
        asNonNullString(session.getMeetName()),
        "Virtius",
        asNonNullString(session.getSessionId()),
        asNonNullString(session.getScoreUrl()),
        session.isWag() ? "WAG" : "MAG",
        asNonNullString(session.getExportStatus()),
        asNonNullString(session.getExportFilename()),
        session.getExportDate() == null ? "" : session.getExportDate().format(DATE_FORMATTER),
        asNonNullString(session.getExportMessage())
        );
  }

  private String asNonNullString(Object value) {
    return value == null ? "" : value.toString();
  }
}