package com.doubletuck.parser;

import com.doubletuck.model.DisciplineCategory;
import com.doubletuck.model.VirtiusScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTrackingFileWriterTest {

  @TempDir
  Path tempDir;

  private Path trackingFile;
  private ExportTrackingFileWriter writer;

  @BeforeEach
  void setUp() {
    trackingFile = tempDir.resolve("tracking.csv");
    writer = new ExportTrackingFileWriter(trackingFile);
  }

  // --- readFile ---

  @Test
  void readFile_fileDoesNotExist_returnsEmptyList() {
    List<VirtiusScore> result = writer.readFile();
    assertThat(result).isEmpty();
  }

  // --- writeFile / readFile round-trip ---

  @Test
  void writeAndReadFile_preservesMeetName() {
    VirtiusScore score = wagScore("12345", "Spring Open");
    writer.writeFile(List.of(score));
    List<VirtiusScore> result = writer.readFile();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMeetName()).isEqualTo("Spring Open");
  }

  @Test
  void writeAndReadFile_preservesSessionId() {
    VirtiusScore score = wagScore("99", "Open");
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getSessionId()).isEqualTo("99");
  }

  @Test
  void writeAndReadFile_preservesScoreUrl() {
    VirtiusScore score = wagScore("42", "Open");
    score.setScoreUrl("https://virti.us/session?s=42");
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getScoreUrl()).isEqualTo("https://virti.us/session?s=42");
  }

  @Test
  void writeAndReadFile_preservesDiscipline() {
    VirtiusScore wagScore = wagScore("1", "WAG Meet");
    wagScore.setDiscipline(DisciplineCategory.WAG);
    VirtiusScore magScore = wagScore("2", "MAG Meet");
    magScore.setDiscipline(DisciplineCategory.MAG);
    VirtiusScore unkScore = wagScore("3", "UNK Meet");
    unkScore.setDiscipline(DisciplineCategory.UNK);

    writer.writeFile(List.of(wagScore, magScore, unkScore));
    List<VirtiusScore> result = writer.readFile();

    assertThat(result.get(0).getDiscipline()).isEqualTo(DisciplineCategory.WAG);
    assertThat(result.get(1).getDiscipline()).isEqualTo(DisciplineCategory.MAG);
    assertThat(result.get(2).getDiscipline()).isEqualTo(DisciplineCategory.UNK);
  }

  @Test
  void writeAndReadFile_preservesExportStatus() {
    VirtiusScore score = wagScore("5", "Open");
    score.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getExportStatus()).isEqualTo(VirtiusScore.ExportStatus.EXPORTED);
  }

  @Test
  void writeAndReadFile_preservesMeetDate() {
    LocalDateTime date = LocalDateTime.of(2025, 3, 15, 9, 30);
    VirtiusScore score = wagScore("7", "Spring Open");
    score.setMeetDate(date);
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getMeetDate()).isEqualTo(date);
  }

  @Test
  void writeAndReadFile_preservesExportDate() {
    LocalDateTime exportDate = LocalDateTime.of(2025, 4, 1, 14, 0);
    VirtiusScore score = wagScore("8", "Open");
    score.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);
    score.setExportDate(exportDate);
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getExportDate()).isEqualTo(exportDate);
  }

  @Test
  void writeAndReadFile_preservesExportFilename() {
    VirtiusScore score = wagScore("10", "Open");
    score.setExportFilename("20250101_V_10_WAG_Open.csv");
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getExportFilename()).isEqualTo("20250101_V_10_WAG_Open.csv");
  }

  @Test
  void writeAndReadFile_preservesExportMessage() {
    VirtiusScore score = wagScore("11", "Open");
    score.setExportStatus(VirtiusScore.ExportStatus.ERROR);
    score.setExportMessage("Timeout occurred");
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getExportMessage()).isEqualTo("Timeout occurred");
  }

  @Test
  void writeAndReadFile_nullMeetDate_roundTripsAsNull() {
    VirtiusScore score = wagScore("20", "Open");
    score.setMeetDate(null);
    writer.writeFile(List.of(score));
    assertThat(writer.readFile().get(0).getMeetDate()).isNull();
  }

  @Test
  void writeFile_multipleSessionsPreservesOrder() {
    VirtiusScore s1 = wagScore("1", "Alpha");
    VirtiusScore s2 = wagScore("2", "Beta");
    VirtiusScore s3 = wagScore("3", "Gamma");
    writer.writeFile(List.of(s1, s2, s3));
    List<VirtiusScore> result = writer.readFile();
    assertThat(result).extracting(VirtiusScore::getSessionId).containsExactly("1", "2", "3");
  }

  @Test
  void writeFile_emptyList_producesFileWithHeaderOnly() {
    writer.writeFile(List.of());
    List<VirtiusScore> result = writer.readFile();
    assertThat(result).isEmpty();
  }

  // --- updateFile ---

  @Test
  void updateFile_newSession_isAdded() {
    VirtiusScore existing = wagScore("1", "Existing");
    writer.writeFile(List.of(existing));

    VirtiusScore newSession = wagScore("2", "New");
    writer.updateFile(List.of(newSession));

    List<VirtiusScore> result = writer.readFile();
    assertThat(result).extracting(VirtiusScore::getSessionId).containsExactlyInAnyOrder("1", "2");
  }

  @Test
  void updateFile_existingSessionId_isOverwritten() {
    VirtiusScore original = wagScore("42", "Original Name");
    original.setExportStatus(VirtiusScore.ExportStatus.NOT_PROCESSED);
    writer.writeFile(List.of(original));

    VirtiusScore updated = wagScore("42", "Updated Name");
    updated.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);
    writer.updateFile(List.of(updated));

    List<VirtiusScore> result = writer.readFile();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMeetName()).isEqualTo("Updated Name");
    assertThat(result.get(0).getExportStatus()).isEqualTo(VirtiusScore.ExportStatus.EXPORTED);
  }

  @Test
  void updateFile_fileDoesNotExist_writesNewSessions() {
    VirtiusScore score = wagScore("55", "New Meet");
    writer.updateFile(List.of(score));

    List<VirtiusScore> result = writer.readFile();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSessionId()).isEqualTo("55");
  }

  // --- getRowsWithExportedStatus ---

  @Test
  void getRowsWithExportedStatus_returnsOnlyExported() {
    VirtiusScore exported = wagScore("1", "Exported Meet");
    exported.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);

    VirtiusScore error = wagScore("2", "Error Meet");
    error.setExportStatus(VirtiusScore.ExportStatus.ERROR);

    VirtiusScore notProcessed = wagScore("3", "Pending Meet");
    notProcessed.setExportStatus(VirtiusScore.ExportStatus.NOT_PROCESSED);

    writer.writeFile(List.of(exported, error, notProcessed));

    List<VirtiusScore> result = writer.getRowsWithExportedStatus();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSessionId()).isEqualTo("1");
  }

  @Test
  void getRowsWithExportedStatus_noneExported_returnsEmptyList() {
    VirtiusScore score = wagScore("1", "Pending");
    score.setExportStatus(VirtiusScore.ExportStatus.NOT_PROCESSED);
    writer.writeFile(List.of(score));

    assertThat(writer.getRowsWithExportedStatus()).isEmpty();
  }

  @Test
  void getRowsWithExportedStatus_fileDoesNotExist_returnsEmptyList() {
    assertThat(writer.getRowsWithExportedStatus()).isEmpty();
  }

  // --- helpers ---

  private VirtiusScore wagScore(String sessionId, String meetName) {
    VirtiusScore score = new VirtiusScore();
    score.setSessionId(sessionId);
    score.setMeetName(meetName);
    score.setScoreUrl("https://virti.us/session?s=" + sessionId);
    score.setDiscipline(DisciplineCategory.WAG);
    score.setExportStatus(VirtiusScore.ExportStatus.NOT_PROCESSED);
    return score;
  }
}
