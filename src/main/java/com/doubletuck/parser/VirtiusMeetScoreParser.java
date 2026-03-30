package com.doubletuck.parser;

import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class VirtiusMeetScoreParser extends AbstractWebParser {

    private final static Logger logger = LoggerFactory.getLogger(VirtiusMeetScoreParser.class);

    @Setter
    private List<VirtiusScore> meetSessionList = new ArrayList<VirtiusScore>();

    public VirtiusMeetScoreParser() {
    }

    public VirtiusMeetScoreParser(List<VirtiusScore> meetSessionList) {
        this.meetSessionList = meetSessionList;
    }

    public void export() {

        if (meetSessionList.isEmpty()) {
            logger.info("No Virtius meet sessions where provided, therefore exiting the exports processing.");
            return;
        }

        try {
            initializeWebDriver();
            for (VirtiusScore currentSession : meetSessionList) {
                logger.info("Start processing the Virtius meet scores export for session: {}", currentSession);

                String scoreText = extractScores(currentSession.getScoreUrl());
                if (scoreText != null) {
                    try {
                        Path exportFile = Path.of("data/" + currentSession.generateFileName() + ".csv");
                        writeTsvAsCsv(scoreText, exportFile);
                        currentSession.setExportFileName(exportFile.getFileName().toString());
                        currentSession.setExportStatus(VirtiusScore.ExportStatus.EXPORTED);
                        logger.info("Completed processing the Virtius meet scores export for session: {}", currentSession);
                    } catch (Exception e) {
                        currentSession.setExportStatus(VirtiusScore.ExportStatus.ERROR);
                        currentSession.setExportMessage(e.getMessage());
                        logger.error("Error processing the Virtius meet scores export for session: {}", currentSession, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching or parsing the web page.", e);
        } finally {
            closeWebDriver();
        }
    }


    private String extractScores(String sessionUrl) {
        String exportedText = null;

        try {
            System.out.println("\n=== Extracting Scores ===");
            System.out.println("Navigating to session URL: " + sessionUrl);
            driver.get(sessionUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Click STATS button
            WebElement statsButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[@class='triggerPanelRow']//button[contains(text(), 'STATS')]")));
            statsButton.click();

            // Wait for modal
            WebElement modalContent = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("modal-content")));

            // ---------------------------------------------------------
            // IMPORTANT PART: Intercept clipboard writes
            // ---------------------------------------------------------
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

            // Click export button
            WebElement exportDataDiv = modalContent.findElement(By.className("exportData"));
            WebElement exportButton = exportDataDiv.findElement(By.tagName("button"));
            exportButton.click();

            // Give JS a moment to run
            Thread.sleep(500);

            // Retrieve intercepted text
            exportedText = (String) js.executeScript(
                    "return window.__copiedText;");

            if (exportedText != null && !exportedText.isBlank()) {
                System.out.println("\n=== Exported Data ===");
                System.out.println(exportedText);
                System.out.println("=== End Exported Data ===\n");
            } else {
                System.out.println("No export data captured.");
            }

        } catch (Exception e) {
            System.err.println("Error extracting scores:");
            e.printStackTrace();
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
