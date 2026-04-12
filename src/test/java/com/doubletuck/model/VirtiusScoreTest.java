package com.doubletuck.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VirtiusScoreTest {

  private VirtiusScore buildScore(String meetName, LocalDateTime meetDate, String sessionId, boolean isWag) {
    VirtiusScore score = new VirtiusScore();
    score.setMeetName(meetName);
    score.setMeetDate(meetDate);
    score.setSessionId(sessionId);
    score.setWag(isWag);
    return score;
  }

  @Test
  void generateFileName_wagMeet_includesWagSuffix() {
    VirtiusScore score = buildScore("State Championships", LocalDateTime.of(2025, 1, 4, 9, 0), "12345", true);
    assertThat(score.generateFileName()).isEqualTo("20250104_V_12345_WAG_StateChampionships");
  }

  @Test
  void generateFileName_magMeet_includesMagSuffix() {
    VirtiusScore score = buildScore("State Championships", LocalDateTime.of(2025, 1, 4, 9, 0), "12345", false);
    assertThat(score.generateFileName()).isEqualTo("20250104_V_12345_MAG_StateChampionships");
  }

  @Test
  void generateFileName_stripsForwardSlash() {
    VirtiusScore score = buildScore("East/West Invitational", LocalDateTime.of(2025, 3, 15, 10, 0), "99", true);
    assertThat(score.generateFileName()).isEqualTo("20250315_V_99_WAG_EastWestInvitational");
  }

  @Test
  void generateFileName_stripsBackslash() {
    VirtiusScore score = buildScore("East\\West Invitational", LocalDateTime.of(2025, 3, 15, 10, 0), "99", true);
    assertThat(score.generateFileName()).isEqualTo("20250315_V_99_WAG_EastWestInvitational");
  }

  @Test
  void generateFileName_stripsSpaces() {
    VirtiusScore score = buildScore("Spring   Open", LocalDateTime.of(2025, 4, 1, 8, 0), "77", true);
    assertThat(score.generateFileName()).isEqualTo("20250401_V_77_WAG_SpringOpen");
  }

  @Test
  void generateFileName_stripsDash() {
    VirtiusScore score = buildScore("Mid-Season Classic", LocalDateTime.of(2025, 6, 10, 9, 0), "55", true);
    assertThat(score.generateFileName()).isEqualTo("20250610_V_55_WAG_MidSeasonClassic");
  }

  @Test
  void generateFileName_stripsSpecialChars() {
    VirtiusScore score = buildScore("Meet #1 (WAG) & More @ Home: 2025", LocalDateTime.of(2025, 2, 20, 12, 0), "42", true);
    assertThat(score.generateFileName()).isEqualTo("20250220_V_42_WAG_Meet1WAGMoreHome2025");
  }

  @Test
  void generateFileName_stripsApostrophe() {
    VirtiusScore score = buildScore("Women's Open", LocalDateTime.of(2025, 5, 5, 9, 0), "33", false);
    assertThat(score.generateFileName()).isEqualTo("20250505_V_33_MAG_WomensOpen");
  }

  @Test
  void generateFileName_nullMeetDate_throwsIllegalArgumentException() {
    VirtiusScore score = buildScore("State Meet", null, "123", true);
    assertThatThrownBy(score::generateFileName)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("meetDate");
  }

  @Test
  void generateFileName_nullSessionId_throwsIllegalArgumentException() {
    VirtiusScore score = buildScore("State Meet", LocalDateTime.of(2025, 1, 1, 9, 0), null, true);
    assertThatThrownBy(score::generateFileName)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sessionId");
  }

  @Test
  void generateFileName_nullMeetName_throwsIllegalArgumentException() {
    VirtiusScore score = buildScore(null, LocalDateTime.of(2025, 1, 1, 9, 0), "123", true);
    assertThatThrownBy(score::generateFileName)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("meetName");
  }

  @Test
  void generateFileName_dateFormattedAsYyyyMmDd() {
    VirtiusScore score = buildScore("Open", LocalDateTime.of(2024, 12, 9, 0, 0), "1", true);
    assertThat(score.generateFileName()).startsWith("20241209_");
  }

  @Test
  void defaultExportStatus_isNotProcessed() {
    VirtiusScore score = new VirtiusScore();
    assertThat(score.getExportStatus()).isEqualTo(VirtiusScore.ExportStatus.NOT_PROCESSED);
  }

  @Test
  void defaultIsWag_isTrue() {
    VirtiusScore score = new VirtiusScore();
    assertThat(score.isWag()).isTrue();
  }
}
