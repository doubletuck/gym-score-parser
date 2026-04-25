# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build (produces target/gym-score-parser.jar)
mvn clean package

# Run the JAR (requires a subcommand)
java -jar target/gym-score-parser.jar --help
java -jar target/gym-score-parser.jar bulk-export-scores
java -jar target/gym-score-parser.jar export-scores --sessions <id1,id2,...>
java -jar target/gym-score-parser.jar generate-tracking-file
```

Run tests with `mvn test`. Test classes exist under `src/test/` for `ExportTrackingFileWriter`, `VirtiusMeetScoreParser`, `VirtiusMeetSessionsParser`, and `VirtiusScore`.

## Architecture

This is a headless CLI application built with **picocli** and **logback** — it has no web endpoints and does not use Spring Boot. `GymScoreParserApplication.main` delegates to picocli's `CommandLine`, which dispatches to one of three subcommands based on the argument provided.

Both parser classes extend `AbstractWebParser`, which manages the headless Chrome WebDriver lifecycle (`initializeWebDriver()` / `closeWebDriver()`).

### Commands

#### BulkExportScoresCommand (`bulk-export-scores`)
Scrapes the full Virtius session list via `VirtiusMeetSessionsParser`, then exports scores for all sessions via `VirtiusMeetScoreParser`. Supports `--export-directory`, `--export-tracking-filename`, and `--overwrite-export-files` options. Skips already-exported sessions unless `--overwrite-export-files` is set. Updates the tracking file after export and logs a processing summary.

#### ExportScoresCommand (`export-scores`)
Exports scores for one or more specific sessions provided via `--sessions <id1,id2,...>`. Same options as `bulk-export-scores`. Constructs `VirtiusScore` objects directly from the provided session IDs without scraping the Virtius home page.

#### GenerateTrackingFileCommand (`generate-tracking-file`)
Stub command — not yet implemented. Intended to generate a CSV tracking file listing all meets on the Virtius home page.

### Classes

#### VirtiusMeetSessionsParser
Uses Selenium WebDriver and JSoup to navigate to `https://virti.us/`. `getSessionList()` initiates parsing: it scrolls the infinite-scroll page to load all content, then parses `div.heroMessage` blocks to extract meet name, date, WAG/MAG flag, and session URL. Looks for "View Session" links first; falls back to "View Scores ONLY" links (rewriting `scores.virti.us/` → `virti.us/session`). Returns a list of `VirtiusScore` objects.

#### VirtiusMeetScoreParser
Takes a list of `VirtiusScore` objects and an export directory path. `export()` navigates to each session URL, clicks the INFO button to extract meet title, date, and location, then clicks the STATS button, intercepts the clipboard write via injected JavaScript (`navigator.clipboard.writeText` override), captures the TSV data, converts it to CSV, and writes it to `{exportDataDirectory}/{generateFileName()}.csv`. Updates each session's `exportStatus` to `EXPORTED` or `ERROR`. Restarts the WebDriver every 50 sessions to prevent memory accumulation.

#### ExportTrackingFileWriter
Reads and writes a CSV tracking file that records export status for each session. `getRowsWithExportedStatus()` returns previously exported sessions (used to skip re-exports). `updateFile()` appends or updates rows with the latest export results.

#### VirtiusScore (model)
Tracks state for a single meet session. Fields: `scoreUrl`, `sessionId`, `meetName`, `meetDate`, `meetLocation`, `discipline`, `exportStatus`, `exportFilename`, `exportDate`, `exportMessage`. `exportStatus` tracks state: `NOT_PROCESSED` → `EXPORTED` or `ERROR`. `generateFileName()` produces a filename from `meetDate`, `sessionId`, `discipline`, and `meetName`.

#### DisciplineCategory (enum)
Values: `WAG`, `MAG`, `UNK`. Defaults to `UNK` on a new `VirtiusScore`.

#### AppProperties
Singleton that loads `application.properties`. Provides `getExportDataDirectory()` and `getExportTrackingFilename()` used as defaults when CLI options are not provided.

**Key details:**
- `VirtiusScore.exportStatus` tracks state: `NOT_PROCESSED` → `EXPORTED` or `ERROR`
- Both parser classes extend `AbstractWebParser` and spin up their own Chrome WebDriver instances
- Java 21, picocli 4.7.6, logback 1.5.18, Maven

## Testing Rules
- JUnit 5 for all tests; test class naming: `{ClassName}Test.java`
- Every public method needs at least one unit test
- Parser tests must use local HTML fixtures in `src/test/resources/` — never hit live URLs
- Run `mvn test` and confirm passing after every change

## Documentation Rules
- Update Javadoc if any method signature or behavior changes
- Update the Architecture section in this file if a new class is added
- Update Build and Run Commands if CLI options change

## After Every Change
- Run `mvn test` — all tests must pass
- Update this file if architecture or commands changed
