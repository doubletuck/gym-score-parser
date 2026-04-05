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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

public class ExportTrackingFileWriter {

  private static final Logger logger = LoggerFactory.getLogger(ExportTrackingFileWriter.class);

  private final String exportDataDirectory;
  private final String exportTrackingFilename;

  public ExportTrackingFileWriter(String exportDataDirectory, String exportTrackingFilename) {
    this.exportDataDirectory = exportDataDirectory;
    this.exportTrackingFilename = exportTrackingFilename;
  }

  private Path getOutputFilePath() {
    return Path.of(exportDataDirectory, exportTrackingFilename);
  }

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

  public void updateFile(List<VirtiusScore> sessions) {
    List<VirtiusScore> fileSessions = this.readFile();

    Map<String, VirtiusScore> sessionMap = new LinkedHashMap<>();
    for (VirtiusScore session : fileSessions) {
      sessionMap.put(session.getSessionId(), session);
    }

    for (VirtiusScore session : sessions) {
      sessionMap.put(session.getSessionId(), session);
    }

    writeFile(new ArrayList<>(sessionMap.values()));
  }

  public void writeFile(List<VirtiusScore> sessions) {
    try {
      Files.createDirectories(getOutputFilePath().getParent());
    } catch (IOException e) {
      logger.error("An error when creating");
      return;
    }

    try (
        BufferedWriter writer = Files.newBufferedWriter(
            getOutputFilePath(),
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
      logger.info("Wrote {} records to {}.", rowCount, getOutputFilePath());
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

    if (!Files.exists(getOutputFilePath())) {
      logger.error("The file {} does not exist.",
          getOutputFilePath().toString());
      return virtiusScoreList;
    }

    try (
        BufferedReader reader = Files.newBufferedReader(getOutputFilePath());
        CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180.builder()
            .setHeader(Headers.class)
            .setSkipHeaderRecord(true)
            .setRecordSeparator("\n")
            .get());) {

      for (CSVRecord row : parser) {
        VirtiusScore score = new VirtiusScore();
        String meetDateStr = row.get(Headers.MEET_DATE);
        if (!meetDateStr.isEmpty()) {
          score.setMeetDate(LocalDateTime.parse(meetDateStr, DATE_FORMATTER));
        }
        score.setMeetName(row.get(Headers.MEET_NAME));
        score.setSessionId(row.get(Headers.SESSION_ID));
        score.setScoreUrl(row.get(Headers.SCORE_URL));
        score.setWag("WAG".equals(row.get(Headers.WAG_MAG)));
        String exportStatusStr = row.get(Headers.EXPORT_STATUS);
        if (!exportStatusStr.isEmpty()) {
          score.setExportStatus(VirtiusScore.ExportStatus.valueOf(exportStatusStr));
        }
        score.setExportFilename(row.get(Headers.EXPORT_FILENAME));
        String exportDateStr = row.get(Headers.EXPORT_TIMESTAMP);
        if (!exportDateStr.isEmpty()) {
          score.setExportDate(LocalDateTime.parse(exportDateStr, DATE_FORMATTER));
        }
        score.setExportMessage(row.get(Headers.EXPORT_MESSAGE));
        virtiusScoreList.add(score);
      }

    } catch (Exception e) {
      logger.error("An error occurred when reading the Virtius score export status file {}: ", getOutputFilePath(), e);
    }

    return virtiusScoreList;
  }

  private String asNonNullString(Object value) {
    return value == null ? "" : value.toString();
  }
}