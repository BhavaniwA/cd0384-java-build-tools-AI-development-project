# CosmoChain Launch Monitoring CLI

This project simulates a real engineering scenario where you inherit a partially completed codebase and must stabilize it before production deployment.

You will step into the role of a **Senior Software Engineer at CosmoChain**, a fictional space mission logistics company that monitors upcoming rocket launches from multiple providers. The application checks for changes in launch schedules and notifies the operations team when meaningful updates occur.

The starter codebase contains incomplete build configuration, missing test assertions, and logic issues that must be identified and corrected. Your goal is to repair the build system, complete the testing framework, and ensure the application behaves correctly.

By completing this project you will practice:

- Configuring Maven builds  
- Writing unit tests with JUnit 6  
- Isolating dependencies with Mockito  
- Implementing integration tests  
- Measuring test coverage with JaCoCo  
- Performing manual code review to detect logic errors  

---

# Getting Started

Instructions for getting a copy of the project running on your local machine.

## Dependencies

You must have the following tools installed:

- Java 17+
- Apache Maven 3.8+
- Git

Verify installations using:

`java -version`  
`mvn -version`  
`git --version`

---

## Installation

Clone the repository:

`git clone https://github.com/udacity/cd0384-java-build-tools-AI-development-project.git`

Navigate to the application directory:

`cd cd0384-java-build-tools-AI-development-project/project/application`

Compile the project:

`mvn clean compile`

---

# Testing

This project contains both **unit tests and integration tests**.

Unit tests validate individual components of the application.  
Integration tests verify that multiple components interact correctly.

## Running Tests

Run unit tests only:

`mvn clean test -DskipITs=true`

Run unit tests without cleaning:

`mvn test -DskipITs=true`

Run the full test suite (unit tests + integration tests + coverage):

`mvn clean verify`

Skip integration tests during verification:

`mvn verify -DskipITs=true`

Run optional live API smoke tests:

`mvn verify -Plive-api-smoke`

Open the JaCoCo coverage report in your browser:

`target/site/jacoco/index.html`

Your project must achieve **at least 65% code coverage**.

---

# Break Down Tests

## CoreLogicAndWorkflowTest

Tests the main application workflow including:

- detecting new launches  
- detecting launch date changes  
- ignoring unchanged launches  
- handling provider failures safely  

## LaunchDetailsFormatterTest

Validates formatting logic used to display launch information in the CLI output.

These tests verify that:

- launch details are displayed consistently  
- blank or missing information is handled safely  
- date and time values are formatted correctly  

## ApplicationIntegrationIT

Integration tests validate interactions between multiple components such as:

- API client behavior  
- repository persistence  
- notification triggering  
- error handling when providers fail  

These tests simulate realistic application workflows.

---

# Project Instructions

You will complete the project by following a structured engineering workflow.

## Phase 1: Repair the Maven Build

Restore the missing Maven configuration so the project supports:

- JUnit 6  
- Mockito  
- Maven Surefire for unit tests  
- Maven Failsafe for integration tests  
- JaCoCo coverage reporting  

Verify the build using:

`mvn clean verify`

---

## Phase 2: Implement Unit Tests with JUnit 5

Complete the unit tests using **Test-Driven Development (TDD)**.

Focus on validating key behaviors including:

- new launch detection  
- launch date change detection  
- unchanged launch handling  
- safe error handling  

---

## Phase 3: Manual Code Review & Repair

Review the source code to identify logic errors and incomplete implementations.

Correct issues so the application behaves according to the functional requirements.

---

## Phase 4: Mockito Isolation

Use **Mockito** to isolate dependencies during unit testing.

Mock external dependencies such as repositories and notification services to ensure tests focus only on the logic under test.

---

## Phase 5: Integration Testing

Complete the integration tests to verify that components interact correctly.

Integration tests ensure that:

- API responses are parsed correctly  
- launch updates trigger the correct notifications  
- repository state is updated properly  
- failures in one provider do not stop processing of others  

---

## Phase 6: Final Verification with Coverage

Run the full build and confirm that:

- all tests pass  
- integration tests execute successfully  
- JaCoCo generates a coverage report  
- the project meets the **65% coverage requirement**

Run:

`mvn clean verify`

---

# Built With

- Apache Maven – Build automation and dependency management  
- JUnit 6 – Unit testing framework  
- Mockito – Mocking framework for isolating dependencies  
- JaCoCo – Code coverage analysis tool  

---

# License

See the `LICENSE` file included with this repository.
