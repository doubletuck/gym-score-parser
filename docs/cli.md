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
Exports all scores that are listed on [Virtius](https://virti.us/) web page. All export processing information is recorded in the `exportTrackingFilename` CSV file. 

### Options
| Option | Required or optional | Description |
| --- | --- | --- |
| --export-directory directory | Optional | The directory in which the export data is written. If not provided, then defaults to the `export.data.directory` value in the `application.properties` file. |
| --export-tracking-filename filename | Optional | The name of the file that is used to track the exports. If not provided, then defaults to the `export.tracking-filename` value in the `application.properties` file. The file will be stored in the `export-directory`. |
| --overwrite-export-files | Optional | Its existence indicates that existing exported files can be overwritten. If not provided, then meet scores that are already downloaded will be skipped. This is the default behavior. |


### Processing

- Exports the scores for all meets found on the [Virtius](https://virti.us/) page.
- Each meet score is exported into the `--export-directory` if provided. Otherwise, this value defaults to the `export.data.directory` value from `application.properties`.
- The status of each export is tracked in the `--export-tracking-filename` file if provided. Otherwise, the value defaults to the `export.tracking-filename` value from `application.properties`.
- Call `getSessionList` from VirtiusMeetSessionsParser to get a list of all the current sessions on the Virtius page.
