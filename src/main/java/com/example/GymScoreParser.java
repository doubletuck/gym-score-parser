package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

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
            String pageSource = getPageSource();
            parseSessions(pageSource);
            printSessions();

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
}
