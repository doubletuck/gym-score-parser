# Meet Score Export Command Line Interface

## Usage

```
java -jar gym-score-parser.jar <command> [options]
java -jar gym-score-parser.jar --help
java -jar gym-score-parser.jar <command> --help
```

Or, for convenience, use the [gym-score-parser](../bin/gym-score-parser) script. It wraps the abovementioned jar call into a script.
```
bin/gym-score-parser <command>
```

---

## Commands

### `generate-tracking-file`

Generates a CSV tracking file that lists all the meets found on the Virtius home page. The tracking file records each meet and its export status. It is used by `bulk-export-scores` to determine which meets have already been exported.

### Implementation Status
Not yet completed

#### Options

| Option | Required | Description |
| --- | --- | --- |
| `--export-directory <directory>` | Optional | Directory where the tracking file is written. Defaults to `export.data.directory` in `application.properties`. |
| `--export-tracking-filename <filename>` | Optional | Name of the tracking file. Defaults to `export.tracking-filename` in `application.properties`. The file is stored in `--export-directory`. |
| `--overwrite-tracking-file` | Optional | If present, an existing tracking file is overwritten and all data within it is cleared. Otherwise, the file is updated with new meet information. |

#### Examples

```
# Use all defaults
generate-tracking-file

# Write the tracking file to a specific directory
generate-tracking-file --export-directory /data/exports

# Use a custom tracking filename
generate-tracking-file --export-tracking-filename 2026-meets.csv

# Overwrite an existing tracking file
generate-tracking-file --overwrite-tracking-file

# Combine options
generate-tracking-file --export-directory /data/exports --export-tracking-filename 2026-meets.csv --overwrite-tracking-file
```

---

### `bulk-export-scores`

Exports scores for all meets listed on the [Virtius](https://virti.us/) home page. Each meet's scores are written to a separate CSV file. Export status for every meet is recorded in the tracking file.

Meets that have already been exported (as recorded in the tracking file) are skipped by default. Use `--overwrite-export-files` to re-export them.

#### Options

| Option | Required | Description |
| --- | --- | --- |
| `--export-directory <directory>` | Optional | Directory where exported score files are written. Defaults to `export.data.directory` in `application.properties`. |
| `--export-tracking-filename <filename>` | Optional | Name of the file used to track export status. Defaults to `export.tracking-filename` in `application.properties`. The file is stored in `--export-directory`. |
| `--overwrite-export-files` | Optional | If present, meets that were previously exported are re-exported and their files are overwritten. If omitted, previously exported meets are skipped. |

#### Processing

1. Reads the tracking file to determine which meets have already been exported.
2. Calls `VirtiusMeetSessionsParser.getSessionList()` to retrieve all current meets from the Virtius page.
3. Skips meets already marked as exported (unless `--overwrite-export-files` is set).
4. Exports scores for each remaining meet to a CSV file in `--export-directory`.
5. Updates the tracking file with the export status of each processed meet.

#### Examples

```
# Use all defaults
bulk-export-scores

# Write exports to a specific directory
bulk-export-scores --export-directory /data/exports

# Use a custom tracking filename
bulk-export-scores --export-tracking-filename 2026-meets.csv

# Re-export all meets, overwriting existing files
bulk-export-scores --overwrite-export-files

# Combine options
bulk-export-scores --export-directory /data/exports --export-tracking-filename 2026-meets.csv --overwrite-export-files
```

---

### `export-scores`

Exports scores for one or more specific Virtius sessions by session ID. Each session's scores are written to a separate CSV file. Export status for every session is recorded in the tracking file.

Sessions that have already been exported (as recorded in the tracking file) are skipped by default. Use `--overwrite-export-files` to re-export them.

#### Options

| Option | Required | Description |
| --- | --- | --- |
| `--sessions <session-ids>` | Required | One or more session IDs separated by commas. |
| `--export-directory <directory>` | Optional | Directory where exported score files are written. Defaults to `export.data.directory` in `application.properties`. |
| `--export-tracking-filename <filename>` | Optional | Name of the file used to track export status. Defaults to `export.tracking-filename` in `application.properties`. The file is stored in `--export-directory`. |
| `--overwrite-export-files` | Optional | If present, sessions that were previously exported are re-exported and their files are overwritten. If omitted, previously exported sessions are skipped. |

#### Examples

```
# Export a single session
export-scores --sessions 12345

# Export multiple sessions
export-scores --sessions 12345,67890,11111

# Write exports to a specific directory
export-scores --sessions 12345 --export-directory /data/exports

# Use a custom tracking filename
export-scores --sessions 12345 --export-tracking-filename 2026-meets.csv

# Re-export sessions, overwriting existing files
export-scores --sessions 12345,67890 --overwrite-export-files

# Combine options
export-scores --sessions 12345,67890 --export-directory /data/exports --export-tracking-filename 2026-meets.csv --overwrite-export-files
```