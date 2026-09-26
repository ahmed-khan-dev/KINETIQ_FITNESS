# Kinetiq — AI Fitness and Diet Coach

## Product Requirements

### 1. Project Overview

Kinetiq is a native Android fitness and diet coaching application.

The application helps a user:

* unlock the app using fingerprint or face authentication
* create a basic fitness profile
* receive a locally generated weekly workout plan
* log completed workouts
* log meals with photos
* log body weight
* capture progress photos
* store GPS information with saved logs
* view today's fitness summary on the Home screen

The application must work without an internet connection and must be usable in airplane mode.

---

# 2. Primary Goal

Deliver a working Android MVP demonstrating the complete local user flow:

**Biometric Unlock → Profile → Weekly Plan → Workout/Meal/Progress Logging → Home Summary**

The application is intended to demonstrate:

* Local database usage
* MVVM architecture
* GPS integration
* Camera/image storage
* Biometric authentication
* Local workout-plan generation
* Dashboard data aggregation

---

# 3. Platform and UI Requirements

Target platform:

* Android
* Native Kotlin
* XML layouts

The UI should use normal Android Views and XML layouts.

The application should contain separate screens/fragments for the major features.

---

# 4. Required Features

## 4.1 Authentication / App Lock

The application must start with a lock screen.

The user should be able to authenticate using:

* Fingerprint
* Face authentication where supported

Use Android biometric authentication.

After successful authentication:

* navigate to Home
* create/update the local authenticated session

The expected session timeout is 15 minutes.

If biometric authentication is unavailable and PIN fallback is implemented:

* use a 4-digit PIN
* never store the PIN as plain text

---

# 5. User Profile

The user must be able to create a basic fitness profile.

Required information:

* Age
* Gender
* Height
* Weight
* Fitness level
* Goal
* Activity level
* Dietary preference
* Allergies/restrictions
* Medical-restriction flags
* Equipment access
* Workout time availability
* Days per week

Goals may include:

* Weight loss
* Muscle gain
* Recomposition
* Endurance
* General fitness

Dietary preferences may include:

* Vegetarian
* Vegan
* Non-vegetarian
* Keto
* High-protein
* Low-carb
* Balanced

Equipment options may include:

* None
* Home basic equipment
* Full gym

Time availability:

* 15 minutes
* 30 minutes
* 45 minutes
* 60+ minutes

Days per week should influence the weekly workout plan.

---

# 6. Workout Plan Generation

The MVP does NOT require a trained machine-learning model.

Workout generation should be deterministic and rules-based.

Basic split rules:

* 3 days/week → Full Body
* 4 days/week → Upper / Lower
* 5–6 days/week → Push / Pull / Legs

The workout generator should:

1. read the user's profile
2. select an appropriate split
3. select exercises from the local exercise library
4. filter exercises according to available equipment
5. consider fitness level
6. avoid exercises matching relevant exclusion tags
7. assign target sets, reps and rest
8. create a weekly plan
9. save the plan locally

The weekly plan must be stored in the local database.

The Plan screen should show:

* day
* session name
* exercises
* sets
* reps
* rest
* estimated duration where available

The plan should display the rule used to generate the split.

---

# 7. Exercise Library

The application must contain a local exercise library.

At least approximately 15 ready-made exercises should be available for initial plan generation.

Each exercise may contain:

* name
* muscle group
* equipment
* difficulty
* instructions
* sets
* reps
* rest
* MET/calorie estimation
* exclusion tags
* optional instructional video URL

Exercise data should be seeded locally.

---

# 8. Workout Logging

The user must be able to open a planned workout and log completion.

The application should support:

* completed sets
* repetitions
* weight where applicable
* optional RPE
* completion time

When a workout is saved:

* create a workout log in Room
* attempt to obtain the current GPS location
* save latitude
* save longitude
* save accuracy
* save timestamp
* save whether GPS was available

If GPS is unavailable:

* the workout must still be saved
* GPS status should be marked unavailable

---

# 9. Meal Logging

The user must be able to log a meal.

The meal flow should support:

1. open Log Meal
2. capture a meal photo
3. save the image locally
4. enter or display meal information
5. save the meal log
6. attach GPS information

The MVP can use manual nutrition entry when an external recognition service is unavailable.

The meal record may contain:

* meal name
* meal slot
* calories
* protein
* carbohydrates
* fat
* photo path
* timestamp
* GPS information

Supported meal slots:

* Breakfast
* Lunch
* Dinner
* Snack

---

# 10. Meal Image Storage

Meal images must be saved inside the application's private files directory.

Expected location:

`filesDir/images/meals`

The database should store the image file path/reference.

Do not store the complete image as a database blob.

Do not rely only on the device gallery.

---

# 11. Progress Tracking

The user must be able to:

### Weight

* enter current weight
* save weight history
* view previous entries

### Progress Photo

* capture a progress photo
* select an angle
* save the photo locally
* save timestamp
* save GPS information

Supported photo angles:

* Front
* Side
* Back

Progress photos should be stored under:

`filesDir/images/progress`

The database should store the file path.

---

# 12. GPS Requirements

GPS must be associated with saved:

* workout logs
* meal logs
* progress photos

Store:

* latitude
* longitude
* accuracy
* timestamp
* GPS success/failure status

The application should request location permission when necessary.

The application must not prevent a log from being saved merely because GPS is unavailable.

---

# 13. Home Dashboard

The Home screen should summarize today's activity.

It should display at minimum:

### Today's Workout

* today's planned workout
* completion status

### Meals

* meals logged today
* today's basic nutrition totals where available

### Streak

* current activity streak

### Progress

* most recent progress photo
* latest weight where available

### GPS

* most recent GPS/log timestamp
* GPS status/value where available

The Home screen should refresh after a workout, meal, weight, or progress-photo save.

---

# 14. Streak

A streak represents consecutive days on which the user has logged at least one qualifying activity.

Qualifying activities include:

* workout
* meal

The current streak should be visible on Home.

The streak resets when a qualifying day is missed.

---

# 15. Local Database

Use Room with SQLite.

The database should contain data for the following areas:

* Profile
* Exercises
* Weekly workout plan
* Workout sessions
* Workout logs
* Meal logs
* Weight logs
* Progress photos
* GPS data
* Session/authentication state
* Settings

The database is local to the device.

No server is required for the assignment.

---

# 16. Suggested Core Data Relationships

User/profile data should connect to:

* workout plans
* workout logs
* meals
* weight logs
* progress photos
* streak information

Workout plans should contain:

* weekly plan
* workout sessions
* session exercises
* exercise logs

Meal logging may contain:

* meal log
* one or more meal items

Progress contains:

* weight entries
* progress photos

GPS information should be associated with the relevant saved activity.

---

# 17. Offline Requirement

The application must function in airplane mode.

The following must work without internet:

* app unlock
* profile creation
* workout-plan generation
* workout logging
* meal logging
* image viewing
* weight logging
* progress-photo logging
* Home dashboard
* local database queries

No live backend is required for the assignment.

---

# 18. AI / Recommendation Scope

The word "AI" in the application name does not require a trained machine-learning model for the assignment.

Workout recommendations should use deterministic rules.

Meal information may be entered manually for the offline MVP.

The larger product PRD describes future cloud food recognition and learned recommendation models, but those are not required for this one-day Android assignment.

---

# 19. Required Main Screens

The implementation should contain approximately these screens:

1. Lock Screen
2. Home
3. Profile / Onboarding
4. Weekly Workout Plan
5. Workout Detail / Logging
6. Log Meal
7. Meal Details
8. Progress
9. Weight Entry
10. Progress Photo
11. Settings / App Lock if required

The exact number of screens may be simplified where two related functions can be combined.

---

# 20. Camera Requirements

Use the Android camera stack suitable for local image capture.

The application must:

* capture an image
* save it to app-private storage
* retain the file path
* display the saved image later

Meal images and progress images must be stored separately.

---

# 21. Required Permissions

The application may require:

* Camera permission
* Fine location permission
* Biometric authentication capability

Permissions should be requested only when needed.

The user should be given an understandable explanation when a permission is required.

---

# 22. Error / Empty States

The application should handle:

* no workout plan
* no meals logged
* no progress photos
* no weight entries
* camera unavailable
* location unavailable
* biometric unavailable
* invalid profile input
* database errors

GPS failure must never prevent saving a valid log.

---

# 23. Assignment Acceptance Criteria

The application is considered functionally complete when the following demonstration works:

1. Launch the application.
2. Unlock using biometric authentication.
3. Reach Home.
4. Create/save a user profile.
5. Generate and display a weekly workout plan.
6. Open a workout.
7. Log/complete the workout.
8. Save GPS information with the workout.
9. Open meal logging.
10. Capture a meal photo.
11. Save the meal.
12. Display the saved meal photo.
13. Save GPS information with the meal.
14. Enter a weight.
15. Capture/save a progress photo.
16. Display the progress photo.
17. Save GPS information with the progress record.
18. Return to Home.
19. Confirm today's summary is updated.
20. Repeat the important flow in airplane mode.

---
# Navigation UX

Kinetiq uses a familiar mobile navigation hierarchy.

## Primary Navigation

The bottom navigation contains five top-level sections:

* Home
* Workout
* Meal
* Progress
* Profile

These destinations are sibling sections rather than a linear navigation history.

Switching between these sections should not create a stack requiring the user to press Back through previously visited tabs.

## Secondary Navigation

Feature-specific screens are child destinations of their parent section.

Examples:

* Workout → Workout Details → Exercise Details
* Meal → Meal Details
* Progress → Photo Details
* Profile → Edit Profile

Back should return to the logical parent screen.

## UX Goals

Navigation should provide:

* predictable Back behavior
* no duplicate top-level screens
* preservation of useful screen state
* smooth section switching
* clear parent/child relationships
* no unnecessary screen flashes
* correct authentication/onboarding routing

The application should feel familiar to users of modern mobile applications.

Home is the primary application root after authentication.


# 24. Priority Order

Implementation priority:

### P0 — Must Work

* Project builds
* Navigation
* Biometric lock
* Profile
* Room database
* Exercise seed data
* Weekly workout generation
* Workout logging
* GPS
* Meal photo capture
* Local image storage
* Weight logging
* Progress photo
* Home dashboard
* Offline operation

### P1 — Useful if Time Allows

* Better UI
* Streak polish
* More exercise data
* Better meal details
* Before/after progress comparison
* Additional validation

### P2 — Not Required for Tomorrow

* Cloud backend
* PostgreSQL
* Firebase authentication
* External food-recognition API
* Cloud image storage
* Push notifications
* Social features
* Wearables
* Payments
* Trained ML recommendation model
* Automated body-composition estimation

---

# 25. Source of Truth

For this assignment, the **2-page Android assignment specification is the primary implementation requirement**.

The larger Kinetiq Product Requirements Document is reference material for product context and future architecture.

When the larger PRD conflicts with the assignment's local/offline Android requirements, implement the assignment requirement.

The application should prioritize:
**working functionality → correct architecture → testing → UI polish.**
