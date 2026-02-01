# Gym Score Parser

A Spring Boot application that fetches and parses web pages to extract specific links using Selenium WebDriver.

## Features

- Fetches the web page from https://virti.us/ using Selenium WebDriver
- Renders JavaScript content to get the fully loaded page
- Parses the HTML content using JSoup
- Extracts and prints the URL of the anchor tag with "View Session" text

## Requirements

- Java 17+
- Maven 3.6+
- Chrome/Chromium browser (for Selenium WebDriver)

## Building the Project

```bash
mvn clean package
```

## Running the Application

```bash
mvn spring-boot:run
```

Or using the compiled JAR:

```bash
java -jar target/gym-score-parser-1.0.0.jar
```

## How It Works

1. The application starts the Spring Boot context
2. The `GymScoreParser` component uses Selenium WebDriver to fetch the webpage from https://virti.us/
3. Chrome is launched in headless mode to render JavaScript content
4. The page waits 3 seconds for dynamic content to load
5. JSoup parses the rendered HTML
6. It searches for an anchor tag with the text "View Session"
7. The URL (href) of the found anchor is printed to the console
8. The application exits gracefully

## Output Example

```
Fetching URL: https://virti.us/
Page loaded. HTML size: 30322 characters
Total anchor tags found: 36

Found 'View Session' link:
URL: https://virti.us/session?s=LJllrjfQuu
```

## Dependencies

- **Spring Boot 3.2.0**: Framework for Java applications
- **JSoup 1.15.3**: HTML parser for parsing rendered content
- **Selenium 4.15.0**: WebDriver for rendering JavaScript-heavy pages
- **WebDriverManager 5.6.3**: Automatic driver management for Selenium
