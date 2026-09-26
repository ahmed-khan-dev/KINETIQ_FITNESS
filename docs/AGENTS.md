# Kinetiq Android Project — Agent Instructions

## Project Identity

This is a native Android application called **Kinetiq — AI Fitness and Diet Coach**.

## Primary Technology

* Kotlin
* XML layouts
* Android Studio
* MVVM architecture
* ViewModel + StateFlow
* Navigation Component
* Room Database
* SQLite
* CameraX
* AndroidX BiometricPrompt
* Google Play Services Location

## Critical Architecture Rules

1. This is a **native Android Kotlin project**.
2. Use **XML layouts**, not Jetpack Compose unless explicitly requested.
3. Follow MVVM.
4. UI/screens must not directly access Room, GPS, CameraX, or biometric APIs.
5. Screens communicate with ViewModels.
6. ViewModels manage UI state.
7. Repositories handle database, location, file, and biometric operations.
8. Keep the application functional without a backend.
9. Do not introduce Firebase authentication, PostgreSQL, Node.js, Express, cloud storage, or a live backend.
10. The application must work in airplane mode.

## Assignment Priority

The provided Android assignment requirements are the source of truth for the implementation.

The larger Product Requirements Document is reference material only. When the PRD conflicts with the Android assignment, follow the Android assignment.

## Required Features

* Local Room database
* Profile
* Exercise library
* Weekly workout plan
* Workout logging
* Meal logging
* Meal photo capture
* Progress photo capture
* Weight logging
* GPS data attached to logs
* Biometric authentication
* PIN fallback when required
* Home dashboard
* Offline operation

## GPS

When saving a workout, meal, or progress photo:

* Attempt to obtain one current location fix.
* Store latitude.
* Store longitude.
* Store accuracy.
* Store timestamp.
* Store whether GPS was successfully obtained.
* If GPS is unavailable, save the log anyway with GPS marked unavailable.

## Image Storage

Images must be saved inside the application's filesDir.

Use:

* filesDir/images/meals
* filesDir/images/progress

The database should store the image path, not the image bytes.

## Biometrics

Use AndroidX BiometricPrompt.

Preferred biometric authentication:

* BIOMETRIC_STRONG

Session timeout:

* 15 minutes

If biometric authentication cannot be used and PIN fallback is implemented, never store the PIN as plain text.

## Workout Generation

The assignment uses deterministic rules.

Do not introduce an ML model for workout generation.

Example:

* 3 days/week → Full Body
* 4 days/week → Upper/Lower

The generated weekly plan should be stored locally in Room.

## Development Style

* Prefer simple implementations suitable for a one-day deadline.
* Avoid unnecessary abstraction.
* Do not introduce libraries unless needed.
* Keep classes small and understandable.
* Use meaningful names.
* Add comments only where they clarify non-obvious logic.

## Safe Agent Workflow

Before making major architectural changes:

1. Inspect the existing project.
2. Reuse existing working code where possible.
3. Make the smallest change required.
4. Build the project.
5. Fix compilation errors.
6. Do not modify unrelated files.

## Navigation and User Experience Rules

### 1. Navigation Hierarchy

The application has two navigation levels:

**Primary navigation**

* Home
* Workout
* Meal
* Progress
* Profile

These five destinations are top-level application sections represented by the bottom navigation bar.

**Secondary navigation**

* Detail screens
* Form screens
* Add/edit screens
* Exercise detail
* Workout logging
* Meal details
* Progress photo details
* Other child screens

Secondary destinations may use the normal Android Navigation back stack.

### 2. Bottom Navigation Must Not Behave Like a Linear History

Do NOT implement bottom navigation so that switching tabs produces a simple history such as:

Home → Workout → Meal → Progress → Profile

and then Back produces:

Profile → Progress → Meal → Workout → Home

Bottom-navigation destinations are sibling/top-level destinations, not a sequence of pages.

Repeatedly switching tabs must not create duplicate instances of those top-level destinations.

### 3. Top-Level Navigation Behavior

When switching between bottom-navigation destinations:

* use `launchSingleTop` / equivalent behavior where appropriate
* use `popUpTo` / state-saving behavior where appropriate
* restore the selected destination's state where practical
* avoid duplicate top-level destinations
* preserve useful scroll/form/UI state when returning to a section
* do not unnecessarily recreate or reset a destination

Use the Navigation Component's supported state-saving/restoration mechanisms rather than manually maintaining duplicate stacks unless there is a clear requirement.

### 4. Child Screen Back Behavior

Child screens should behave like normal hierarchical navigation.

Examples:

Home → Workout → Workout Details → Back → Workout

Workout → Exercise Details → Back → Workout Details

Meal → Meal Details → Back → Meal

Progress → Photo Details → Back → Progress

Profile → Edit Profile → Back → Profile

Back should return to the immediate logical parent.

### 5. Root Back Behavior

When the user is at a root/top-level destination, pressing Back must NOT walk backward through every previously selected bottom-navigation tab.

The application should follow a predictable root-navigation strategy.

For this project, Home is the main/root entry point after authentication.

Do not make the user repeatedly press Back through unrelated tabs before leaving the application.

### 6. Home Is the Application Root

After successful authentication:

* if onboarding is required → Onboarding
* otherwise → Home

After onboarding is completed:

Onboarding → Home

The authentication and onboarding destinations must not remain incorrectly accessible through normal back navigation.

The user should not be able to press Back from Home and return to an already completed onboarding/authentication screen.

### 7. State Preservation

Navigation must preserve useful user state.

Examples:

* Returning to Workout should preserve its current list/scroll state where practical.
* Returning to Profile should show the saved profile, not restart onboarding.
* Returning to Meal should not unnecessarily clear entered/loaded data.
* Returning to Progress should preserve the relevant screen state.

Database-backed data must always be reloaded correctly when necessary.

ViewModel + StateFlow should be used for screen state rather than relying on Fragment instance state alone.

### 8. Smooth Navigation

Navigation should feel familiar to users of modern applications such as Instagram, YouTube, and other mainstream mobile applications.

Prioritize:

* predictable Back behavior
* no duplicate screens
* no unexpected jumps
* no unnecessary reloads
* no visible screen flashing
* preservation of useful state
* consistent transitions
* clear parent/child hierarchy

Do not add flashy animations that reduce responsiveness.

Prefer subtle, consistent transitions.

### 9. Navigation and Authentication

The lock/authentication screen is outside the bottom-navigation hierarchy.

The bottom navigation must be hidden during:

* authentication
* first-time onboarding
* other full-screen setup/security flows where appropriate

After successful completion of the setup/security flow, navigate to the appropriate top-level destination and reset the previous setup flow from the back stack.

### 10. Adding New Screens

Whenever a new feature or screen is added, determine whether it is:

**Top-level section**
or
**Child/detail screen**

Do not add every new screen to bottom navigation.

Only major peer sections belong in bottom navigation.

New detail screens should normally remain inside their parent section's navigation flow.

### 11. Navigation Changes Must Be Tested

Whenever navigation code is modified, test at minimum:

1. Switching between all five bottom-navigation destinations.
2. Re-selecting the current tab.
3. Opening a child/detail screen and pressing Back.
4. Switching tabs after opening a child screen.
5. Returning to the previous tab and checking state preservation.
6. Pressing Back from a root destination.
7. Relaunching the app.
8. Authentication → Home/Onboarding routing.
9. Onboarding completion → Home.
10. Ensuring no duplicate destinations are created.

Do not mark a navigation change complete based only on a successful Gradle build. It must also be manually tested on the emulator/device.

### 12. Do Not Rewrite Navigation Unnecessarily

Before modifying navigation:

* inspect the existing navigation graph
* inspect `MainActivity`
* inspect bottom-navigation setup
* understand the existing back-stack behavior

Make the smallest change that achieves the required UX.

Do not migrate navigation frameworks or rewrite the entire navigation architecture without a documented reason.


## Important

Do not build the entire application at once.

Implement features incrementally and keep the project compiling after each feature.
