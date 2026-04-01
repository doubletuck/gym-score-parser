# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build
mvn clean package

# Run via Maven
mvn spring-boot:run

# Run via compiled JAR
java -jar target/gym-score-parser-1.0.0.jar

# Run quietly in background
mvn spring-boot:run -q 2>&1 &
```

There are no tests in this project. The Spring Boot test dependency exists but no test classes are defined.

## Architecture

This is a headless Spring Boot CLI application — it has no web endpoints. On startup, `main` directly instantiates `VirtiusMeetSessionsParser`, calls `getSessionList()`, logs the results, and exits.

Both parser classes extend `AbstractWebParser`, which manages the headless Chrome WebDriver lifecycle (`initializeWebDriver()` / `closeWebDriver()`).

### Classes

#### VirtiusMeetSessionsParser
Uses Selenium WebDriver and JSoup to navigate to `https://virti.us/`. `getSessionList()` initiates parsing: it scrolls the infinite-scroll page to load all content, then parses `div.heroMessage` blocks to extract meet name, date, WAG/MAG flag, and session URL. Looks for "View Session" links first; falls back to "View Scores ONLY" links (rewriting `scores.virti.us/` → `virti.us/session`). Returns a list of `VirtiusScore` objects.

#### VirtiusMeetScoreParser
Takes a list of `VirtiusScore` objects. `export()` navigates to each session URL, clicks the STATS button, intercepts the clipboard write via injected JavaScript (`navigator.clipboard.writeText` override), captures the TSV data, converts it to CSV, and writes it to `data/{generateFileName()}.csv`. Updates each session's `exportStatus` to `EXPORTED` or `ERROR`.

**Key details:**
- `VirtiusScore.exportStatus` tracks state: `NOT_PROCESSED` → `EXPORTED` or `ERROR`
- Both parser classes extend `AbstractWebParser` and spin up their own Chrome WebDriver instances
- Java 21, Spring Boot 4.0.0, Maven
