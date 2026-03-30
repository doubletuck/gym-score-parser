package com.doubletuck.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doubletuck.model.VirtiusScore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VirtiusMeetSessionsParser extends AbstractWebParser {

    private final static Logger logger = LoggerFactory.getLogger(VirtiusMeetSessionsParser.class);
    private static final String URL = "https://virti.us/";

    private List<VirtiusScore> viewSessions = new ArrayList<>();

    public List<VirtiusScore> getSessionList() {
        try {
            initializeWebDriver();
            String pageSource = getPageSource();
            parseSessions(pageSource);
        } catch (Exception e) {
            logger.error("Error fetching or parsing the web page: ", e);
        } finally {
            closeWebDriver();
        }
        return viewSessions;
    }

    private String getPageSource() throws Exception {

        logger.debug("Getting the list of meets on Virtius by fetching URL: {}", URL);
        driver.get(URL);

        // Wait for JavaScript to render (simple wait)
        Thread.sleep(3000);

        // Scroll to load all dynamic content
        long lastHeight = 0;
        long scrollPause = 1000; // milliseconds
        int maxScrollAttempts = 25;
        int scrollAttempts = 0;

        logger.debug("Meets on Virtius is displayed as an infinite scroll. Scroll to load all content available on the page. Max scroll attempts will be: {}.", maxScrollAttempts);
        while (scrollAttempts < maxScrollAttempts) {
            // Get the current scroll height
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

        logger.debug("Scrolling complete. Total scroll attempts: {}", scrollAttempts);

        // Get the fully rendered page source
        String pageSource = driver.getPageSource();
        System.out.println("Page loaded. HTML size: " + pageSource.length() + " characters");

        return pageSource;
    }

    private void parseSessions(String pageSource) {

        Document document = Jsoup.parse(pageSource);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
                "EEE M/dd/yyyy '@' h:mm a",
                Locale.ENGLISH
        );

        // Find all divs with class "heroMessage"
        Elements heroMessages = document.select("div.heroMessage");
        logger.info("Expecting {} meets since that is the total 'heroMessage' div count", heroMessages.size());

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

            // If the viewSessionLink is null, then look for "View Scores ONLY" link since a few instances
            // reverse the score and session links. Replace a portion of View Scores URL so that it points
            // to the session url:
            // https://scores.virti.us/?s=xxxx -> https://virti.us/session?s=xxxx
            String scoreUrl = "";
            if (viewSessionLink == null) {
                Element viewScoresLink = heroDiv.selectFirst("a:contains(View Scores ONLY)");
                if (viewScoresLink != null) {
                    scoreUrl = viewScoresLink.attr("href");
                    scoreUrl = scoreUrl.replace("scores.virti.us/", "virti.us/session");
                }
            } else {
                scoreUrl = viewSessionLink.attr("href");
            }

            // Check if there's a "Mens" tag pill. If it doesn't exist, then it's WAG.
            Element mensTag = heroDiv.selectFirst("span.tagPill.mens");
            boolean isWag = mensTag == null; // If no mens tag, then it's WAG

            logger.info("Found {} meet {} on {}: {}", (isWag ? "WAG" : "MAG"), meetName, meetDate, scoreUrl);

            // Only add if we have a valid URL
            if (!scoreUrl.isEmpty()) {
                    VirtiusScore session = new VirtiusScore();
                    session.setScoreUrl(scoreUrl);
                    session.setSessionId(scoreUrl.substring(scoreUrl.indexOf("s=") + 2));
                    session.setMeetName(meetName);
                    session.setWag(isWag);
                    if (meetDate != null && !meetDate.trim().isEmpty()) {
                        session.setMeetDate(LocalDateTime.parse(meetDate, dateFormatter));
                    }
                    viewSessions.add(session);
            } else {
                logger.warn("The score URL for meet {} on {} is missing", meetName, meetDate);
            }
        }
    }
}
