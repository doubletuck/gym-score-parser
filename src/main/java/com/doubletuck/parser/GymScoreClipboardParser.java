package com.doubletuck.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.GymScoreVirtius;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GymScoreClipboardParser {

    private final static Logger logger = LoggerFactory.getLogger(GymScoreClipboardParser.class);

    private WebDriver driver = null;
    @Setter
    private List<GymScoreVirtius> meetSessionList = new ArrayList<GymScoreVirtius>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public GymScoreClipboardParser() {
    }

    public GymScoreClipboardParser(List<GymScoreVirtius> meetSessionList) {
        this.meetSessionList = meetSessionList;
    }

    public void export() {

        if (meetSessionList.isEmpty()) {
            logger.info("No Virtius meet sessions where provided, therefore exiting the exports processing.");
            return;
        }

        try {
            initializeWebDriver();
            for (GymScoreVirtius currentSession : meetSessionList) {
                logger.info("Start processing the Virtius meet scores export for session: {}", currentSession);

                String scoreText = extractScores(currentSession.getScoreUrl());
                if (scoreText != null) {
                    try {
                        Path exportFile = Path.of("data/" + generateFileName(currentSession));
                        writeTsvAsCsv(scoreText, exportFile);
                        currentSession.setExportFileName(exportFile.getFileName().toString());
                        currentSession.setExportStatus(GymScoreVirtius.ExportStatus.EXPORTED);
                        logger.info("Completed processing the Virtius meet scores export for session: {}", currentSession);
                    } catch (Exception e) {
                        currentSession.setExportStatus(GymScoreVirtius.ExportStatus.ERROR);
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

    private String generateFileName(GymScoreVirtius currentSession) {
        return String.join("_",
                        currentSession.getMeetDate().format(formatter),
                        currentSession.getSessionId(),
                        currentSession.isWag() ? "WAG" : "MAG",
                        currentSession.getMeetName().replaceAll(("[/\\\\\\s-]+"), "")) +
                ".csv";
    }

    private void initializeWebDriver() {
        if (this.driver != null) {
            return;
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--start-maximized");
        options.addArguments(
                "--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        this.driver = new ChromeDriver(options);
    }

    private void closeWebDriver() {
        if (this.driver != null) {
            this.driver.quit();
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
