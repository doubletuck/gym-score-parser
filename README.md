# Gym Score Parser

Fetches and parses web pages to extract NCAA gymnastics scores.

## Running the Application

Build first, then run the fat JAR:
```shell
mvn clean package
java -jar target/gym-score-parser.jar
```

Or run in the background:
```shell
java -jar target/gym-score-parser.jar > logs/application.log 2>&1 &
```

> **Note:** The export directory specified in `application.properties` (`export.data.directory`) must exist before running.