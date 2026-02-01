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

@Component
public class GymScoreParser {

    private static final String URL = "https://virti.us/";

    public void parseAndPrint() {
        WebDriver driver = null;
        try {
            // Setup ChromeDriver
            WebDriverManager.chromedriver().setup();

            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run in headless mode (no GUI)
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--start-maximized");
            options.addArguments(
                    "--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // Initialize WebDriver
            driver = new ChromeDriver(options);

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

            // Parse with JSoup
            Document document = Jsoup.parse(pageSource);

            // Find all anchor tags
            Elements links = document.select("a");
            System.out.println("Total anchor tags found: " + links.size());

            // Collect all "View Session" links
            java.util.List<String> viewSessionLinks = new java.util.ArrayList<>();
            for (Element link : links) {
                String text = link.text().trim();
                String href = link.attr("href");

                if (text.contains("View Session") && !href.isEmpty()) {
                    viewSessionLinks.add(href);
                }
            }

            // Print all View Session links found
            if (!viewSessionLinks.isEmpty()) {
                System.out.println("\n=== All 'View Session' Links Found ===");
                for (int i = 0; i < viewSessionLinks.size(); i++) {
                    System.out.println((i + 1) + ". " + viewSessionLinks.get(i));
                }
                System.out.println("\nTotal 'View Session' links found: " + viewSessionLinks.size());
            } else {
                System.out.println("\nNote: 'View Session' links not found on the page.");
            }

        } catch (Exception e) {
            System.err.println("Error fetching or parsing the web page:");
            e.printStackTrace();
        } finally {
            // Close the WebDriver
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
