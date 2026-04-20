# Gym Score Parser

Fetches and parses web pages to extract NCAA gymnastics scores.

## Configuration
The [application.properties](src/main/resources/application.properties) contains the following values:
| Property | Description |
| --- | --- |
| `export.data.directory` | The default directory where score data exports and tracking files are downloaded. |
| `export.tacking-filename` | The default export file name that is used to track the export status for export processing. |


## Building the artifact
```shell
mvn clean package
```

This will produce a `gym-score-parser.jar` file in the [target](target) directory.

## Running tests
```shell
mvn test
```

## Running the Application

Refer to the [Command Line Interface](docs/cli.md) document for the options that can be used to run the application.
