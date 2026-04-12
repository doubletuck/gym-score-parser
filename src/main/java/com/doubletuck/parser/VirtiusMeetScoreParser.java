package com.doubletuck.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

import lombok.Getter;
import lombok.NonNull;

/**
 * Reads and exports the scores that are hosted on the Virti.us website.
 *
 * When using the parser, a list of VirtiusScore objects is passed in and stored
 * as
 * meetSessionList. Each VirtiusScore object has a scoreUrl that indicates the
 * meet
 * session url, and from there the scores can be found and exported to file.
 */
public class VirtiusMeetScoreParser extends AbstractWebParser {

  private final static Logger logger = LoggerFactory.getLogger(VirtiusMeetScoreParser.class);

  private final Path exportDataDirectory;

  @Getter
  private List<VirtiusScore> meetSessionList = new ArrayList<VirtiusScore>();

  public VirtiusMeetScoreParser(Path exportDataDirectory, List<VirtiusScore> meetSessionList) {
    this.exportDataDirectory = exportDataDirectory;
    this.meetSessionList = meetSessionList;
  }

  public List<VirtiusScore> export() {

    try {
      logger.info("Initiate the download of {} Virtius meet sessions.", this.meetSessionList.size());
      initializeWebDriver();
      for (VirtiusScore currentSession : meetSessionList) {
        if (currentSession.getScoreUrl() == null || currentSession.getScoreUrl().isEmpty()) {
          logger.warn(
              "Cannot process the session because the scoreUrl is missing. Skipping and moving to the next item. {}",
              currentSession);
          continue;
        }

        logger.info("Begin extracting scores for session {}.", currentSession.getScoreUrl());

        String scoreText = extractScores(currentSession.getScoreUrl());
        if (scoreText != null) {
          try {
            Path exportFile = Path.of(exportDataDirectory.toString(), currentSession.generateFileName() + ".csv");
            writeTsvAsCsv(scoreText, exportFile);
            currentSession.setExportFilename(exportFile.getFileName().toString());
            currentSession.setExportDate(LocalDateTime.now());
            currentSession.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);
            logger.info("Exported score data for session {} to {}: {}", currentSession.getScoreUrl(),
                currentSession.getExportFilename(), currentSession);
          } catch (Exception e) {
            currentSession.setExportStatus(VirtiusScore.ExportStatus.ERROR);
            currentSession.setExportMessage(e.getMessage());
            logger.error("Error exporting score data for session {}.", currentSession, e);
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error fetching or parsing the web page.", e);
    } finally {
      closeWebDriver();
    }
    return meetSessionList;
  }

  @SuppressWarnings("null")
  private String extractScores(@NonNull String sessionUrl) {
    String exportedText = null;

    try {
      logger.trace("{} - Invoke the web driver and go to the url.", sessionUrl);
      driver.get(sessionUrl);

      logger.trace("{} - Pause for 10 seconds.", sessionUrl);
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

      logger.trace("{} - Click on the 'STATS' button", sessionUrl);
      WebElement statsButton = wait.until(
          ExpectedConditions.elementToBeClickable(
              By.xpath("//div[@class='triggerPanelRow']//button[contains(text(), 'STATS')]")));
      statsButton.click();

      logger.trace("{} - Wait for the modal 'class name = modal-content' to return", sessionUrl);
      WebElement modalContent = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.className("modal-content")));

      logger.trace(
          "{} - Generate javascript script to intercept the clipboard text that is generated from clicking the STATS button",
          sessionUrl);
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("""
              window.__copiedText = null;
              if (!navigator.clipboard.__intercepted) {
                const originalWriteText = navigator.clipboard.writeText;
                navigator.clipboard.writeText = function(text) {
                  window.__copiedText = text;
                  return Promise.resolve();
                };
                navigator.clipboard.__intercepted = true;
              }
          """);

      logger.trace("{} - Find export button and click it. Button is in element with class name = 'exportData'.",
          sessionUrl);
      WebElement exportDataDiv = modalContent.findElement(By.className("exportData"));
      WebElement exportButton = exportDataDiv.findElement(By.tagName("button"));
      exportButton.click();

      logger.trace("{} - Give the javascript a moment to run. Sleep for 500 milliseconds.", sessionUrl);
      Thread.sleep(500);

      logger.trace("{} - Retrieve the intercepted text.", sessionUrl);
      exportedText = (String) js.executeScript(
          "return window.__copiedText;");

      if (exportedText != null && !exportedText.isBlank()) {
        logger.trace("{} - Export data:\n{}", sessionUrl, exportedText);
      } else {
        logger.info("{} - No export data captured.", sessionUrl);
      }

    } catch (Exception e) {
      logger.error("Error extracting scores from the Virtius clipboard", e);
    }

    return exportedText;
  }

  private void writeTsvAsCsv(String tsvText, Path outputFile) throws IOException {
    List<String> lines = tsvText.lines().toList();

    if (lines.isEmpty()) {
      throw new IllegalArgumentException("No TSV data provided");
    }

    int columnCount = lines.get(0).split("\t", -1).length;

    StringBuilder csv = new StringBuilder();

    for (String line : lines) {
      String[] fields = line.split("\t", -1);

      // Normalize column count
      if (fields.length < columnCount) {
        String[] padded = new String[columnCount];
        System.arraycopy(fields, 0, padded, 0, fields.length);
        for (int i = fields.length; i < columnCount; i++) {
          padded[i] = "";
        }
        fields = padded;
      }

      for (int i = 0; i < fields.length; i++) {
        csv.append(escapeCsv(fields[i]));
        if (i < fields.length - 1) {
          csv.append(",");
        }
      }
      csv.append("\n");
    }

    Files.writeString(
        outputFile,
        csv.toString(),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }

    boolean mustQuote = value.contains(",")
        || value.contains("\"")
        || value.contains("\n");

    String escaped = value.replace("\"", "\"\"");

    return mustQuote ? "\"" + escaped + "\"" : escaped;
  }
}
