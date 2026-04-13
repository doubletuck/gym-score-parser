package com.doubletuck.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class VirtiusScore {

  private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  private String scoreUrl;
  private String sessionId;
  private String meetName;
  private LocalDateTime meetDate;
  private String meetLocation;
  private boolean isWag = true;
  private ExportStatus exportStatus = ExportStatus.NOT_PROCESSED;
  private String exportFilename;
  private LocalDateTime exportDate;
  private String exportMessage;

  public enum ExportStatus {
    NOT_PROCESSED,
    ERROR,
    EXPORTED,
    SKIPPED
  }

  public String generateFileName() {
    if (meetDate == null || sessionId == null || meetName == null) {
      throw new IllegalArgumentException(
          "Missing one or more of the required fields to generate a file name: meetDate, sessionId, meetName");
    }

    return String.join("_",
        getMeetDate().format(formatter),
        "V",
        getSessionId(),
        isWag() ? "WAG" : "MAG",
        getMeetName().replaceAll(("[/\\\\\\s-#'&()@:]+"), ""));
  }
}
