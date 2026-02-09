# Gym Score Parser

Fetches and parses web pages to extract NCAA gymnastics scores.

## Building the Project

```bash
mvn clean package
```

## Running the Application

Basic run:
```shell
mvn spring-boot:run
```

Run in quiet mode in the background:
```shell
mvn spring-boot:run -q 2>&1 &
```

Or using the compiled JAR:

```shell
java -jar target/gym-score-parser-1.0.0.jar
```