# Meet Score Export Command Line Interface

## Generate Tracking File
### Command Name
generate-tracking-file

### Description
Generates a CSV file that lists all the meets that are on the Virtius home page.

### Options
| Option | Required or optional | Description |
| --- | --- | --- |
| --export-directory directory | Optional | The directory where the tracking file is written. If not provided, then defaults to the `export.data.directory` value in the `application.properties` file. |
| --export-tracking-filename filename | Optional | The name of the file that is used to track the exports. If not provided, then defaults to the `export.tracking-filename` value in the `application.properties` file. The file will be stored in the `export-directory`. |
| --overwrite-tracking-file | Optional | If present, then indicates that if the tracking file already exists, then it should be overwritten and all data within it will be cleared. Otherwise, the file is updated with new information. |

### Examples
#### Example: Use defaults
```
generate-tracking-file
```

#### Example: Specify tracking file name
```
generate-tracking-file --export-tracking-filename 2026-meet-scores-list.csv
```

## Bulk Export Scores
### Command Name
bulk-export-scores

### Description
Exports all scores that are listed on Virtius home page. If no criteria are specified, then all scores from the sessions listed on the hope page are exported. If criteria are specified, then only the meet scores that meet the criteria are exported. All export information is saved in a tracking file. 

### Options
| Option | Required or optional | Description |
| --- | --- | --- |
| --export-directory directory | Optional | The directory in which the export data is written. If not provided, then defaults to the `export.data.directory` value in the `application.properties` file. |
| --export-tracking-filename filename | Optional | The name of the file that is used to track the exports. If not provided, then defaults to the `export.tracking-filename` value in the `application.properties` file. The file will be stored in the `export-directory`. |
| --min-meet-date date | Optional | A date in the format of YYYYMMDD. Will only export meets that occur on or after this date. If no value is given, then no begin range is considered. | 
| --max-meet-date date | Optional | A date in the format of YYYYMMDD. Will only export meets that occur on or before this date. If no value is given, then no end range is considered. |
| --overwrite-tracking-file | Optional | If present, then indicates that if the tracking file already exists, then it should be overwritten and all data within it will be cleared. Otherwise, the file is updated with new information. |
| --overwrite-export-files | Optional | If present, then indicates that if the export file already exists, then it should be overwritten. Otherwise, do not overwrite it. |

### Processing

Setting variables from the CLI:
- Set `--export-directory` value to the `exportDirectory` variable if it is provided. Otherwise, use the `export.data.directory` value from `application.properties`.
- Set `--export-tracking-filename` value to the `exportTrackingFilename` variable if it is provided. Otherwise, use the `export.tracking-filename` value from `application.properties`.
- Convert `--min-meet-date` to a LocalDateTime if it is provided and set it to the `minMeetDate` variable. If it's not a valid date, then log the error and exit processing.
- Convert `--max-meet-date` to a LocalDateTime if it is provided and set it to the `maxMeetDate` variable. If it's not a valid date, then log the error and exit processing.
- If `--overwrite-tracking-file` is provided, then set the `overwriteTrackingFile` variable to `true`. Otherwise, set the variable to `false`.
- If `--overwrite-export-files` is provided, then set the `overwriteExportFiles` variable to `true`. Otherwise, set the variable to `false`.

Validating the variables:
- The value from `--export-directory` exists. If not, then log an error and exit.
- The value from `--min-meet-date` can be converted into a valid date and it not after `--max-meet-date`.
- The value from `--max-meet-date` can be converted into a valid date and it is not before `--min-meet-date`.

Running the logic:
- 



