package com.doubletuck.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import com.doubletuck.model.GymScoreVirtius;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class GymScoreParser {

    private static final String URL = "https://virti.us/";
    private WebDriver driver = null;
    private List<GymScoreVirtius> viewSessions = new ArrayList<>();

    public void parseAndPrint() {
        try {
            initializeWebDriver();
            // String pageSource = getPageSource();
            // parseSessions(pageSource);
            // printSessions();
            extractScores("https://virti.us/session?s=5j0yATMveQ");
        } catch (Exception e) {
            System.err.println("Error fetching or parsing the web page:");
            e.printStackTrace();
        } finally {
            closeWebDriver();
        }
    }

    private void initializeWebDriver() {
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

    private String getPageSource() throws Exception {

        System.out.println("Fetching URL: " + URL);
        driver.get(URL);

        // Wait for JavaScript to render (simple wait)
        Thread.sleep(3000);

        // Scroll to load all dynamic content
        System.out.println("Scrolling to load all content...");
        long lastHeight = 0;
        long scrollPause = 1000; // milliseconds
        int maxScrollAttempts = 20;
        int scrollAttempts = 0;

        while (scrollAttempts < maxScrollAttempts) {
            // Get current scroll height
            long currentHeight = (Long) ((JavascriptExecutor) driver)
                    .executeScript("return document.documentElement.scrollHeight");

            if (currentHeight == lastHeight) {
                // No new content loaded, reached the end
                break;
            }

            lastHeight = currentHeight;
            // Scroll down by the full height
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("window.scrollTo(0, document.documentElement.scrollHeight);");

            // Wait for content to load
            Thread.sleep(scrollPause);
            scrollAttempts++;
        }

        System.out.println("Scrolling complete. Total scroll attempts: " + scrollAttempts);

        // Get the fully rendered page source
        String pageSource = driver.getPageSource();
        System.out.println("Page loaded. HTML size: " + pageSource.length() + " characters");

        return pageSource;
    }

    private void parseSessions(String pageSource) {

        Document document = Jsoup.parse(pageSource);

        // Find all divs with class "heroMessage"
        Elements heroMessages = document.select("div.heroMessage");
        System.out.println("Total 'heroMessage' divs found: " + heroMessages.size());

        // Collect all gym score sessions from heroMessage divs
        for (Element heroDiv : heroMessages) {
            // Extract meet name from matchTitle
            Element matchTitle = heroDiv.selectFirst("p.matchTitle");
            String meetName = matchTitle != null ? matchTitle.text().trim() : "";

            // Extract meet date from matchDate
            Element matchDate = heroDiv.selectFirst("p.matchDate");
            String meetDate = matchDate != null ? matchDate.text().trim() : "";

            // Find the View Session link within this div
            Element viewSessionLink = heroDiv.selectFirst("a:contains(View Session)");
            String scoreUrl = viewSessionLink != null ? viewSessionLink.attr("href") : "";

            // Check if there's a "Mens" tag pill (if yes, it's not WAG)
            Element mensTag = heroDiv.selectFirst("span.tagPill.mens");
            boolean isWag = mensTag == null; // If no mens tag, then it's WAG

            // Only add if we have a valid URL
            if (!scoreUrl.isEmpty()) {
                GymScoreVirtius session = new GymScoreVirtius();
                session.setScoreUrl(scoreUrl);
                session.setMeetName(meetName);
                session.setMeetDate(meetDate);
                session.setWag(isWag);
                viewSessions.add(session);
            }
        }
    }

    private void printSessions() {
        if (!viewSessions.isEmpty()) {
            System.out.println("\n=== All Gym Sessions Found ===");
            for (int i = 0; i < viewSessions.size(); i++) {
                GymScoreVirtius session = viewSessions.get(i);
                System.out.println((i + 1) + ". " + session);
            }
            System.out.println("\nTotal sessions found: " + viewSessions.size());
        } else {
            System.out.println("\nNote: No gym sessions found on the page.");
        }
    }

    private void extractScores(String sessionUrl) {
        try {
            System.out.println("\n=== Extracting Scores ===");
            System.out.println("Navigating to session URL: " + sessionUrl);
            driver.get(sessionUrl);

            // Wait for the page to load
            Thread.sleep(2000);

            // Find and click the STATS button in the triggerPanelRow
            System.out.println("Looking for STATS button...");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement statsButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[@class='triggerPanelRow']//button[contains(text(), 'STATS')]")));

            System.out.println("Found STATS button. Clicking...");
            statsButton.click();

            // Wait for the modal to appear
            System.out.println("Waiting for modal to appear...");
            WebElement modalContent = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("modal-content")));

            // Wait a bit for modal to fully render
            Thread.sleep(1000);

            System.out.println("Looking for export button in modal...");
            WebElement exportDataDiv = modalContent.findElement(By.className("exportData"));
            WebElement exportButton = exportDataDiv.findElement(By.tagName("button"));

            System.out.println("Found export button. Clicking to copy data...");
            exportButton.click();

            // Wait a moment for the clipboard to be updated
            Thread.sleep(1000);

            String clipboardContent = readClipboard();

            if (clipboardContent != null && !clipboardContent.isEmpty()) {
                // System.out.println("\n=== Clipboard Content ===");
                // System.out.println(clipboardContent);
                // System.out.println("=== End of Clipboard Content ===\n");
                Path outputFile = Path.of("gym-scores.csv");
                writeTsvAsCsv(clipboardContent, outputFile);
                System.out.println("CSV written to: " + outputFile.toAbsolutePath());
            } else {
                System.out.println("Failed to read clipboard content");
            }
        } catch (Exception e) {
            System.err.println("Error extracting scores:");
            e.printStackTrace();
        }
    }

    private String readClipboard() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeAsyncScript(
                    "var callback = arguments[arguments.length - 1];" +
                            "navigator.clipboard.readText().then(function(text) {" +
                            "  callback(text);" +
                            "}).catch(function(err) {" +
                            "  callback('');" +
                            "});");

            return result != null ? result.toString() : "";
        } catch (Exception e) {
            System.err.println("Error reading clipboard via JavaScript:");
            e.printStackTrace();
            return "";
        }
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
