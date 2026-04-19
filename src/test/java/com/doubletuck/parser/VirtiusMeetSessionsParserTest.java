package com.doubletuck.parser;

import com.doubletuck.model.DisciplineCategory;
import com.doubletuck.model.VirtiusScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for VirtiusMeetSessionsParser focusing on HTML parsing logic.
 * parseSessions is private; accessed via reflection. No browser is started.
 */
class VirtiusMeetSessionsParserTest {

  private VirtiusMeetSessionsParser parser;
  private Method parseSessions;

  @BeforeEach
  void setUp() throws Exception {
    parser = new VirtiusMeetSessionsParser();
    parseSessions = VirtiusMeetSessionsParser.class.getDeclaredMethod("parseSessions", String.class);
    parseSessions.setAccessible(true);
  }

  // --- error cases ---

  @Test
  void parseSessions_nullPageSource_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> parseSessions.invoke(parser, (Object) null))
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseSessions_emptyPageSource_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> parseSessions.invoke(parser, ""))
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  // --- WAG session ---

  @Test
  void parseSessions_singleWagSession_extractsMeetName() throws Exception {
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("State Championships", "Sat 1/04/2025 @ 9:00 AM",
        "https://virti.us/session?s=111", false)));
    List<VirtiusScore> sessions = getViewSessions();
    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).getMeetName()).isEqualTo("State Championships");
  }

  @Test
  void parseSessions_singleWagSession_extractsSessionId() throws Exception {
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("Open", "Sat 1/04/2025 @ 9:00 AM",
        "https://virti.us/session?s=42", false)));
    assertThat(getViewSessions().get(0).getSessionId()).isEqualTo("42");
  }

  @Test
  void parseSessions_singleWagSession_extractsScoreUrl() throws Exception {
    String url = "https://virti.us/session?s=123";
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("Open", "Sat 1/04/2025 @ 9:00 AM", url, false)));
    assertThat(getViewSessions().get(0).getScoreUrl()).isEqualTo(url);
  }

  @Test
  void parseSessions_noMensTag_isWag() throws Exception {
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("Open", "Sat 1/04/2025 @ 9:00 AM",
        "https://virti.us/session?s=1", false)));
    assertThat(getViewSessions().get(0).getDiscipline()).isEqualTo(DisciplineCategory.WAG);
  }

  @Test
  void parseSessions_singleWagSession_extractsMeetDate() throws Exception {
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("Open", "Sat 1/04/2025 @ 9:00 AM",
        "https://virti.us/session?s=1", false)));
    VirtiusScore score = getViewSessions().get(0);
    assertThat(score.getMeetDate().getYear()).isEqualTo(2025);
    assertThat(score.getMeetDate().getMonthValue()).isEqualTo(1);
    assertThat(score.getMeetDate().getDayOfMonth()).isEqualTo(4);
  }

  // --- MAG session ---

  @Test
  void parseSessions_mensTagPresent_isMag() throws Exception {
    parseSessions.invoke(parser, buildHtml(wagHeroDiv("Open", "Mon 2/10/2025 @ 10:00 AM",
        "https://virti.us/session?s=2", true)));
    assertThat(getViewSessions().get(0).getDiscipline()).isEqualTo(DisciplineCategory.MAG);
  }

  // --- fallback URL rewriting ---

  @Test
  void parseSessions_viewScoresOnlyLink_rewrittenToSessionUrl() throws Exception {
    String html = buildHtml(scoresOnlyHeroDiv("Open", "Mon 2/10/2025 @ 10:00 AM",
        "https://scores.virti.us/?s=88"));
    parseSessions.invoke(parser, html);
    List<VirtiusScore> sessions = getViewSessions();
    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).getScoreUrl()).isEqualTo("https://virti.us/session?s=88");
  }

  @Test
  void parseSessions_viewScoresOnlyLink_sessionIdExtracted() throws Exception {
    parseSessions.invoke(parser, buildHtml(scoresOnlyHeroDiv("Open", "Mon 2/10/2025 @ 10:00 AM",
        "https://scores.virti.us/?s=88")));
    assertThat(getViewSessions().get(0).getSessionId()).isEqualTo("88");
  }

  // --- multiple sessions ---

  @Test
  void parseSessions_multipleSessions_allExtracted() throws Exception {
    String html = buildHtml(
        wagHeroDiv("Meet A", "Sat 1/04/2025 @ 9:00 AM", "https://virti.us/session?s=1", false),
        wagHeroDiv("Meet B", "Sun 2/02/2025 @ 10:00 AM", "https://virti.us/session?s=2", false),
        wagHeroDiv("Meet C", "Mon 3/03/2025 @ 11:00 AM", "https://virti.us/session?s=3", true)
    );
    parseSessions.invoke(parser, html);
    List<VirtiusScore> sessions = getViewSessions();
    assertThat(sessions).hasSize(3);
    assertThat(sessions).extracting(VirtiusScore::getMeetName)
        .containsExactly("Meet A", "Meet B", "Meet C");
  }

  // --- missing URL ---

  @Test
  void parseSessions_heroWithNoLink_isSkipped() throws Exception {
    String html = buildHtml(noLinkHeroDiv("Unnamed Meet", "Sat 1/04/2025 @ 9:00 AM"));
    parseSessions.invoke(parser, html);
    assertThat(getViewSessions()).isEmpty();
  }

  // --- no hero divs ---

  @Test
  void parseSessions_noHeroDivs_returnsEmptyList() throws Exception {
    parseSessions.invoke(parser, "<html><body><p>No meets here</p></body></html>");
    assertThat(getViewSessions()).isEmpty();
  }

  // --- reflection helpers ---

  @SuppressWarnings("unchecked")
  private List<VirtiusScore> getViewSessions() throws Exception {
    Field field = VirtiusMeetSessionsParser.class.getDeclaredField("viewSessions");
    field.setAccessible(true);
    return (List<VirtiusScore>) field.get(parser);
  }

  private String buildHtml(String... heroDivs) {
    StringBuilder sb = new StringBuilder("<html><body>");
    for (String div : heroDivs) {
      sb.append(div);
    }
    sb.append("</body></html>");
    return sb.toString();
  }

  /** Builds a heroMessage div with a "View Session" link. Set isMag=true to add the mens tagPill. */
  private String wagHeroDiv(String meetName, String meetDate, String sessionUrl, boolean isMag) {
    String mensTag = isMag ? "<span class=\"tagPill mens\">Men's</span>" : "";
    return "<div class=\"heroMessage\">" +
        "<p class=\"matchTitle\">" + meetName + "</p>" +
        "<p class=\"matchDate\">" + meetDate + "</p>" +
        mensTag +
        "<a href=\"" + sessionUrl + "\">View Session</a>" +
        "</div>";
  }

  /** Builds a heroMessage div with a "View Scores ONLY" link (fallback scenario). */
  private String scoresOnlyHeroDiv(String meetName, String meetDate, String scoresUrl) {
    return "<div class=\"heroMessage\">" +
        "<p class=\"matchTitle\">" + meetName + "</p>" +
        "<p class=\"matchDate\">" + meetDate + "</p>" +
        "<a href=\"" + scoresUrl + "\">View Scores ONLY</a>" +
        "</div>";
  }

  /** Builds a heroMessage div with no usable link. */
  private String noLinkHeroDiv(String meetName, String meetDate) {
    return "<div class=\"heroMessage\">" +
        "<p class=\"matchTitle\">" + meetName + "</p>" +
        "<p class=\"matchDate\">" + meetDate + "</p>" +
        "</div>";
  }
}
