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

This is a headless Spring Boot CLI application — it has no web endpoints. On startup it retrieves the `GymScoreParser` bean, calls `parseAndPrint()`, then exits.

### Classes

#### VirtiusMeetSessionsParser
**VirtiusMeetSessionsParser** is a class that uses Selenium WebDriver and JSoup to navigate to `https://virti.us/`.  
The `getSessionList` method initiates the parsing.
The parsing looks for the "View Session" or "View Scores ONLY" urls that are in the `div` blocks with the class of `heroMessage`.
The data in the div block is stored as a `VirtiusScore` object instance.
A list of `VirtiusScore` objects is returned.

#### VirtiusScoreParser
1. The class takes in a list of `VirtiusScore` instance objects. Each `VirtiusScore` objects contain a sessionUrl.
2. The class uses Selenium WebDriver and JSoup to navigate to the sessionUrl.
3. Clicks the "STATS" and intercepts the clipboard write via injected JavaScript (`navigator.clipboard.writeText` override) and captures the TSV data.
4. The TSV data and is converted to CSV and written to a file.


**Key details:**
- `VirtiusScore.exportStatus` tracks state: `NOT_PROCESSED` → `EXPORTED` or `ERROR`
- Both parser classes spin up their own Chrome WebDriver instances
- Java 21, Spring Boot 4.0.0, Maven
