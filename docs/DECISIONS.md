# Architecture Decisions

## Kotlin + XML
Chosen because the assignment requires/permits native Android and XML and it is the fastest path for this deadline.

## MVVM
Required by the assignment.

## Room
Chosen for local SQLite persistence.

## No Backend
The assignment explicitly requires a local/offline application.

## No Trained ML Model
Workout generation will use deterministic rules.

## Local Image Storage
Meal and progress images will be stored under app-private filesDir.

## Navigation Component
Used for screen navigation.

## KSP
Prefer KSP for Room annotation processing where compatible with the project's Kotlin/Gradle configuration.