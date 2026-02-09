package com.doubletuck.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GymScoreClipboardParser {

    private WebDriver driver = null;

    public void parseAndPrint() {
        try {
            initializeWebDriver();
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

    private void extractScores(String sessionUrl) {
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
            String exportedText = (String) js.executeScript(
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
    }
}
