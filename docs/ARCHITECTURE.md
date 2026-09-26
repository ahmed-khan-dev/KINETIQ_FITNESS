# Kinetiq Architecture

## Architecture Pattern

MVVM with a simple layered structure.

UI
↓
ViewModel
↓
Repository
↓
Local data / device services

## UI

Fragments and XML layouts.

Fragments must not directly access:
- Room
- GPS APIs
- Camera APIs
- Database implementation

## ViewModel

ViewModels:
- hold UI state
- expose StateFlow
- call repositories
- survive configuration changes

## Repository

Repositories coordinate:
- Room database
- location services
- local image storage
- application data

## Data

Room + SQLite for local persistence.

## Offline First

The application must work in airplane mode.

No server is required for the assignment.