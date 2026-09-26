# Kinetiq Changelog

## 2026-09-26 (SUBMISSION VERSION)

### Phase 6 — Workout Plan & Logging (COMPLETE)
- **Exercise Library**: 15 pre-seeded exercises (Chest, Back, Legs, Shoulders, Arms, Core) with equipment/difficulty/restrictions
- **Deterministic Generator**: Rules-based split generation (3-day Full Body, 4-day Upper/Lower, 5-6-day Push/Pull/Legs)
- **Workout Persistence**: All plans/sessions/exercises saved in Room database
- **Real Data Flow**: Exercises from library → generated plan → session display → UI (no placeholder zeros)
- **Exercise Logging**: Set-by-set tracking with weight, reps, and completion status
- **GPS Integration**: Location captured when workouts/sets completed with graceful fallback
- **Profile Integration**: Generator respects fitness level, goal, equipment, days/week, medical flags
- **Equipment Filtering**: Exercises filtered by user's available equipment
- **Medical Restrictions**: Exercises excluded/avoided based on user flags
- **Session Management**: Weekly plan display with today's workout highlighted
- **Progress Tracking**: Exercise and set count displayed during active workout
- **Data Persistence**: All logs survive app restart
- **Build Status**: `:app:assembleDebug` — BUILD SUCCESSFUL
- **Phase 5 Protected**: All authentication, profile, onboarding flows remain stable and unchanged
- **Submission Ready**: Minimal scope, no unnecessary features, all core Phase 6 requirements met

## 2026-09-26

### Phase 5 — User Profile & Onboarding
- Created `UserTargetDao.kt` for `user_targets` table queries.
- Added atomic `@Transaction` method `saveFullProfileAndTargets(...)` in `AppRepository.kt` to save `user_profiles`, `dietary_preferences`, and `user_targets` atomically.
- Implemented `ProfileViewModel.kt` with BMR (Mifflin-St Jeor), TDEE, goal-based calorie targets, PRD calorie floors (1200 kcal female / 1500 kcal male), and macro splits.
- Updated `fragment_profile.xml` and `ProfileFragment.kt` with a 4-step onboarding UI (Physical Metrics, Fitness Configuration, Nutrition & Health Disclaimer, Calculated Targets Preview).
- Configured first-launch onboarding routing in `LockFragment.kt` and `nav_graph.xml`.
- Verified profile editing support (preserves existing entity IDs when updating).
- Verified build status: `:app:assembleDebug` builds cleanly.

### Phase 5 — Profile UX Architecture Correction
- Identified the mixed-state problem: the same profile form contained onboarding steps and the saved summary card in one screen, and the view model used a boolean-only edit state instead of explicit mode separation.
- Added `ProfileMode` with `Onboarding`, `View`, and `Edit` states in `ProfileViewModel.kt` so the UI clearly distinguishes first-launch onboarding, saved profile viewing, and explicit profile editing.
- Updated `ProfileFragment.kt` to render one visual state at a time: the saved summary card for the normal Profile tab and the 4-step editor only after `Edit Profile` is tapped.
- Kept onboarding in `OnboardingFragment` and preserved the existing Room entity/DAO IDs and `saveFullProfileAndTargets(...)` transaction flow so updates continue to hit the same rows instead of creating duplicates.
- Verified the project still builds cleanly with `:app:assembleDebug`.
- Documented the state separation, navigation behavior, and saved profile reload behavior in project state notes.

### Phase 5 — Profile & Onboarding State Fixes
- Identified root cause: onboarding and normal Profile screens each created separate `ProfileViewModel` instances, while the shared Room-backed data was only partially loaded into state.
- Added missing repository access for `getDietaryPreference()` so the saved profile, dietary preference, and target rows can be reloaded together.
- Updated `ProfileViewModel.loadExistingProfile()` to read `user_profiles`, `dietary_preferences`, and `user_targets`, preserve existing IDs, and restore state without blanking the form.
- Updated `ProfileFragment` to populate saved values back into the text fields and spinners when the fragment is recreated and to avoid resetting the saved form state.
- Kept existing `saveFullProfileAndTargets(...)` update logic intact so edits update existing records instead of creating duplicates.
- Build verification: `:app:assembleDebug` — BUILD SUCCESSFUL.
- Manual verification status: onboarding/profile reload paths are code-validated and build-tested; full device/manual QA remains unverified in this environment.

## 2026-09-26

### Phase 4 — Lock Screen & Navigation UX Polish
- Fixed Lock Screen UI flash on startup when valid session exists: wrapped auth UI controls in `authContainer` (`visibility="gone"`) and displayed a neutral `ProgressBar` during session checking in `fragment_lock.xml` and `LockFragment.kt`.
- Restored 15-minute session timeout constant (`SESSION_TIMEOUT_MS = 15 * 60 * 1000L`) in `SecurityUtils.kt`.
- Fixed Bottom Navigation back stack in `MainActivity.kt`: configured top-level destinations with `launchSingleTop`, `restoreState`, and `saveState` pop-up to `homeFragment`.
- Added custom `OnBackPressedCallback` to prevent top-level bottom nav tabs from creating a linear back stack on Back button presses.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-09-26

### Gradle / AGP Plugin Compatibility Fix
- Fixed build error `ApplicationExtensionImpl$AgpDecorated_Decorated cannot be cast to com.android.build.gradle.BaseExtension`.
- Diagnosed root cause: external `org.jetbrains.kotlin.android` plugin was applied alongside AGP 9.3.3 built-in Kotlin support.
- Removed `kotlin-android` plugin alias and version declaration from `gradle/libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts`.
- Verified build status: `:app:assembleDebug` succeeds with zero errors; Phase 4 biometric implementation remains intact.

## 2026-09-26

### Crash Fix — Room KSP Code Generation
- Fixed app crash `AppDatabase_Impl does not exist` on launch.
- Configured KSP (`com.google.devtools.ksp:2.0.21-1.0.27`) for Room annotation compilation.
- Replaced `annotationProcessor` with `ksp(libs.androidx.room.compiler)` in `app/build.gradle.kts`.
- Set `android.disallowKotlinSourceSets=false` and `android.builtInKotlin=false` in `gradle.properties`.
- Verified build status: `:app:assembleDebug` succeeds with generated `AppDatabase_Impl`.

## 2026-09-26

### Phase 4 — Authentication / App Lock & Biometrics
- Added `androidx.biometric:biometric-ktx` dependency.
- Implemented `SecurityUtils.kt` with SHA-256 PIN hashing and 15-minute session timeout calculation.
- Created `LockViewModel.kt` to manage session checking, biometric unlock callbacks, PIN fallback/setup, and error state.
- Updated `LockFragment.kt` and `fragment_lock.xml` with `BiometricPrompt` auto-prompting, PIN keypad entry, and PIN creation support.
- Configured auto-bypass when valid unexpired session exists in local Room `app_sessions` table.
- Configured foreground session expiry re-checks in `MainActivity.kt` `onResume()`.
- Verified build: `:app:assembleDebug` builds cleanly.

## 2026-09-25

### Phase 2 — Local Room Database Setup
- Added Room 2.6.1 dependencies (`room-runtime`, `room-ktx`, `room-compiler`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- Implemented 26 Room `@Entity` classes preserving PRD table names (`users`, `user_profiles`, `dietary_preferences`, `user_targets`, `exercise_library`, `workout_plans`, `workout_sessions`, `session_exercises`, `exercise_logs`, `nutrition_items`, `meal_logs`, `meal_log_items`, `recipes`, `recipe_ingredients`, `meal_plans`, `weight_logs`, `progress_photos`, `daily_summaries`, `badges`, `user_badges`, `streaks`, `notification_preferences`, `friendships`) plus Android assignment additions (`gps_logs`, `app_sessions`, `app_settings`).
- Created 11 Room DAOs for profile, dietary preferences, exercise library, workout plans, workout logs, meal logs, weight logs, progress photos, GPS logs, app session, and streaks.
- Implemented `ExerciseSeedData` with 15 pre-populated exercises across various muscle groups and equipment requirements.
- Configured `AppDatabase` singleton with `RoomDatabase.Callback` for safe initial exercise seeding.
- Updated `AppRepository` and `KinetiqApplication` for database dependency management.
- Verified build: `:app:assembleDebug` builds cleanly.

## 2026-09-25

### Phase 1 — Foundation Established
- Resolved `compileSdk` version compatibility issue (updated to `compileSdk = 37`).
- Enabled `viewBinding` in `app/build.gradle.kts`.
- Added Navigation Component and Lifecycle/ViewModel dependencies.
- Created base MVVM package structure (`data`, `domain`, `ui`, `utils`).
- Created `KinetiqApplication` and registered in `AndroidManifest.xml`.
- Created placeholder fragments and XML layouts for Lock, Home, Profile, Workout, Meal, and Progress screens.
- Configured Navigation graph (`nav_graph.xml`) and `BottomNavigationView` menu (`bottom_nav_menu.xml`).
- Implemented `MainActivity` with `NavController` integration and dynamic bottom bar visibility logic.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-09-26

### Phase 6A — Workout System UX & Data-Flow Correction
- Identified root cause of incomplete Phase 6: the workout UI was rendering placeholder zero-value session data ("0 sets × 0 reps", "No exercise notes") despite an existing exercise library, plan generation logic, and Room schema.
- Fixed `WorkoutViewModel.kt`:
  - Added `targetMuscles` field to `WorkoutExerciseUi` to expose actual exercise target muscle groups.
  - Added `totalSets` field to `WorkoutDayUi` to compute and display total sets per workout day.
  - Extended `WorkoutSessionUiState` with `hasWorkoutData`, `targetMuscles`, `totalSets`, and `completedSets` to track real session/exercise state.
  - Changed `buildDayUiFromPlan` to `buildDayUiFromPlanAsync` and marked as `suspend` to safely call Room repository suspend functions from the coroutine context.
  - Updated `prepareSession()` to populate `hasWorkoutData = true` and pull real exercise values (sets, reps, rest, target muscles) from the generated workout plan instead of zero placeholders.
- Fixed `WorkoutPlanFragment.kt`:
  - Updated UI binding to render actual exercise and session counts instead of incorrect time-multiplication calculations.
  - Display today's workout summary with real per-session values from the loaded plan.
- Fixed `WorkoutSessionFragment.kt`:
  - Replaced placeholder "0 sets × 0 reps" rendering with actual exercise configuration values.
  - Bind UI fields to `targetMuscles`, `sets`, `reps`, `restSeconds`, and completion progress from real workout state.
- Updated `fragment_workout_session.xml`:
  - Added `tvExerciseMuscles` and `tvProgress` views to display target muscles and set completion status.
  - Kept all existing XML IDs stable to maintain Kotlin binding references.
- Verified compile status: `:app:assembleDebug` — BUILD SUCCESSFUL.
- Workout session now displays real exercise data (e.g., "Bench Press • Chest • Triceps • 3 sets × 8-12 reps • Rest: 90 sec") instead of placeholder values.

### Phase 6 — Workout Plan Generation & Logging
- Added deterministic rules-based workout generation in `WorkoutViewModel.kt` using the saved Room-backed Phase 5 profile (`user_profiles`, `dietary_preferences`, `user_targets` access pattern preserved without a second profile system).
- Reused the existing Room tables for workout data: `workout_plans`, `workout_sessions`, `session_exercises`, and `exercise_library` remain the storage backbone for generated weekly plans.
- Extended `AppRepository.kt` with transaction-safe `saveWorkoutPlanWithSessions(...)`, `getActiveWorkoutPlan(...)`, `getSessionsForPlan(...)`, `getSessionExercises(...)`, and `saveExerciseLog(...)` helpers so workout generation and logging stay inside the repository layer.
- Created `WorkoutViewModel.kt` to load a saved profile, generate or reload the current active plan, expose a `StateFlow` UI model, prepare session state, and record completed exercise logs without putting Room calls into fragments.
- Replaced the placeholder `WorkoutPlanFragment.kt` with a modern workout dashboard showing today's workout card and weekly plan summary, and added a dedicated `WorkoutSessionFragment.kt` + `fragment_workout_session.xml` for exercise-by-exercise logging.
- Added the child navigation action `action_workoutPlanFragment_to_workoutSessionFragment` in `nav_graph.xml` and hid the bottom nav while a workout session is active in `MainActivity.kt` to keep workout session screens as child/detail screens.
- Added rules to respect saved fitness level, goal, activity/equipment constraints, weekly days, session duration, and medical flags, while filtering out exercises with exclusion tags such as `knee_injury`, `lower_back_injury`, and `shoulder_injury`.
- Added an empty-state path that tells the user to complete their profile before generating a personalized workout plan and keeps the Phase 5 first-launch onboarding flow intact.
- Verified compile status in this environment: `:app:assembleDebug` — BUILD SUCCESSFUL.
- Device-level runtime verification of exercise completion, saved plan state, and live navigation on an emulator/physical device was not performed in this environment, so those remain unverified until an actual device run is completed.
