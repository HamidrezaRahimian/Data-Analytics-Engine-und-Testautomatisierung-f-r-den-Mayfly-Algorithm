# mayfly-analytics

This project implements a Mayfly Algorithm for minimizing the 10D Ackley function. It also contains analytics, tests, JGiven acceptance tests, and simple report/export support.

This project uses Gradle because the provided project already contains Gradle wrapper files. The PDF allows Maven or Gradle.

## Requirements

- Java 25
- Gradle wrapper included in the project
- Maven commands are listed below because the assignment mentions them, but this repository currently has no Maven build file.

Check the JDK:

```bash
java -version
```

## Build Commands

Linux with Gradle:

```bash
./gradlew test
./gradlew check
./gradlew multiRunVerify
```

Windows PowerShell with Gradle:

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat multiRunVerify
```

Maven commands from the assignment, only usable if a Maven build is added:

```bash
mvn test
mvn verify
mvn -Pmulti-run verify
```

## Run Main

Linux:

```bash
./gradlew classes
java -cp build/classes/java/main edu.swarmintelligence.mayfly.Main
```

Windows PowerShell:

```powershell
.\gradlew.bat classes
java -cp build\classes\java\main edu.swarmintelligence.mayfly.Main
```

## Reports

JaCoCo coverage report path requested by the assignment for Maven:

```text
target/site/jacoco/
```

Current Gradle HTML coverage report:

```text
build/reports/jacoco/test/html/index.html
```

JaCoCo XML path requested by the assignment for Maven:

```text
target/site/jacoco/jacoco.xml
```

Current Gradle XML coverage report:

```text
build/reports/jacoco/test/jacocoTestReport.xml
```

JGiven HTML report:

```text
target/jgiven-reports/html/
```

Analytics markdown report:

```text
docs/analytics-report.md
```

Generated export files:

```text
target/analytics/analytics-report.csv
target/analytics/analytics-report.json
```

The Gradle task `multiRunVerify` is defined in `build.gradle.kts`.

## Directory Structure

```text
src/main/java/edu/swarmintelligence/mayfly/   production code
src/test/java/edu/swarmintelligence/mayfly/   unit and acceptance tests
src/test/resources/                           test configuration
docs/                                         documentation and generated analytics report
build/                                        Gradle build output
target/                                       generated JGiven and analytics output
```

## Phase Notes

Phase 1 contains Aufgabe 1 and Aufgabe 2 and was completed manually without AI support.

Phase 2 contains Aufgabe 3, Aufgabe 4, and Aufgabe 5. AI support was used for Phase 2 and is documented in:

```text
docs/ai-usage-log.md
```
