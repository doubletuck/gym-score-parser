package com.doubletuck.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

public class VirtiusExportStatusWriter {

  private static final Logger logger = LoggerFactory.getLogger(VirtiusExportStatusWriter.class);

  private static final Path OUTPUT_FILE = Path.of("data/meet_scores_export_status.csv");

  private enum Headers {
    MEET_DATE,
    MEET_NAME,
    SCORE_SITE,
    SESSION_ID,
    SCORE_URL,
    WAG_MAG,
    EXPORT_STATUS,
    EXPORT_FILENAME,
    EXPORT_TIMESTAMP,
    EXPORT_MESSAGE
  }

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public void writeFile(List<VirtiusScore> sessions) {
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
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180.builder()
            .setHeader(Headers.class)
            .setRecordSeparator("\n")
            .get());) {
      int rowCount = 0;
      for (VirtiusScore session : sessions) {
        String[] row = buildRow(session);
        printer.printRecord((Object[]) row);
        rowCount++;
      }
      printer.flush();
      logger.info("Wrote {} records to {}.", rowCount, OUTPUT_FILE);
    } catch (IOException e) {
      logger.error("Error upserting export status CSV", e);
    }
  }

  private String[] buildRow(VirtiusScore session) {
    String[] row = new String[Headers.values().length];
    row[Headers.MEET_DATE.ordinal()] = session.getMeetDate() == null ? ""
        : session.getMeetDate().format(DATE_FORMATTER);
    row[Headers.MEET_NAME.ordinal()] = asNonNullString(session.getMeetName());
    row[Headers.SCORE_SITE.ordinal()] = "Virtius";
    row[Headers.SESSION_ID.ordinal()] = asNonNullString(session.getSessionId());
    row[Headers.SCORE_URL.ordinal()] = asNonNullString(session.getScoreUrl());
    row[Headers.WAG_MAG.ordinal()] = session.isWag() ? "WAG" : "MAG";
    row[Headers.EXPORT_STATUS.ordinal()] = asNonNullString(session.getExportStatus());
    row[Headers.EXPORT_FILENAME.ordinal()] = asNonNullString(session.getExportFilename());
    row[Headers.EXPORT_TIMESTAMP.ordinal()] = session.getExportDate() == null ? ""
        : session.getExportDate().format(DATE_FORMATTER);
    row[Headers.EXPORT_MESSAGE.ordinal()] = asNonNullString(session.getExportMessage());
    return row;
  }

  public List<VirtiusScore> readFile() {

    ArrayList<VirtiusScore> virtiusScoreList = new ArrayList<>();

    if (!Files.exists(OUTPUT_FILE)) {
      logger.error("Cannot read the Virtius scores export file {} because it does not exist.", OUTPUT_FILE.toString());
      return virtiusScoreList;
    }

    try (
        BufferedReader reader = Files.newBufferedReader(OUTPUT_FILE);
        CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180.builder()
            .setSkipHeaderRecord(true)
            .setRecordSeparator("\n")
            .get());) {

      for (CSVRecord row : parser) {
        VirtiusScore score = new VirtiusScore();
        String meetDateStr = row.get(Headers.MEET_DATE.ordinal());
        if (!meetDateStr.isEmpty()) {
          score.setMeetDate(LocalDateTime.parse(meetDateStr, DATE_FORMATTER));
        }
        score.setMeetName(row.get(Headers.MEET_NAME.ordinal()));
        score.setSessionId(row.get(Headers.SESSION_ID.ordinal()));
        score.setScoreUrl(row.get(Headers.SCORE_URL.ordinal()));
        score.setWag("WAG".equals(row.get(Headers.WAG_MAG.ordinal())));
        String exportStatusStr = row.get(Headers.EXPORT_STATUS.ordinal());
        if (!exportStatusStr.isEmpty()) {
          score.setExportStatus(VirtiusScore.ExportStatus.valueOf(exportStatusStr));
        }
        score.setExportFilename(row.get(Headers.EXPORT_FILENAME.ordinal()));
        String exportDateStr = row.get(Headers.EXPORT_TIMESTAMP.ordinal());
        if (!exportDateStr.isEmpty()) {
          score.setExportDate(LocalDateTime.parse(exportDateStr, DATE_FORMATTER));
        }
        score.setExportMessage(row.get(Headers.EXPORT_MESSAGE.ordinal()));
        virtiusScoreList.add(score);
      }

    } catch (Exception e) {
      logger.error("An error occurred when reading the Virtius score export status file {}: ", OUTPUT_FILE, e);
    }

    return virtiusScoreList;
  }

  private String asNonNullString(Object value) {
    return value == null ? "" : value.toString();
  }
}