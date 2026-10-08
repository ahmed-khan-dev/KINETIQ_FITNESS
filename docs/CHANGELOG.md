# Kinetiq Changelog

## 2026-10-08

### Phase 6: Reusable Weekly Workout Cycle System & Phase 7: Profile Avatar Redesign
- Added `planType` and `cycleWeekNumber` to `WorkoutPlanEntity.kt`.
- Updated `AppDatabase.kt` version to 3 with `MIGRATION_2_3`.
- Added `repeatWorkoutPlanNewWeek(...)` in `AppRepository.kt` and `startNewWeekCycle()` in `WorkoutViewModel.kt`.
- Added "⚡ START NEW WEEK / REPEAT PLAN" button in `fragment_workout_plan.xml` and `WorkoutPlanFragment.kt`.
- Added `ivProfileAvatar` (`ShapeableImageView` with primary stroke and dummy avatar image) to `fragment_onboarding.xml` and `fragment_profile.xml`.
- Preserved 100% of existing View IDs across all layouts.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-04

### Circular Home Dashboard Ring & Weight Logging Bug Fix
- Created custom `CircularCalorieRingView.kt` rendering the circular metabolic progress arc (`#00DAF3` to `#75FF9E` gradient), consumed calories, target calories, and remaining calorie badge on the left side of the Home hero card.
- Updated `fragment_home.xml`, `HomeFragment.kt`, and `HomeViewModel.kt` to bind consumed calories, target calories, and macro progress bars.
- Added `getTodayWeightLog` and `updateWeightLog` to `WeightLogDao.kt` and `AppRepository.kt`.
- Updated `saveWeight()` in `ProgressViewModel.kt` to update today's existing weight log instead of creating duplicate entries for the same date.
- Updated `WeightTrajectoryView.kt` to group weight logs by calendar day, eliminating graph fluctuations and spikes.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-04

### Custom Workouts, Meal CRUD & Gallery, Weight Trajectory Graph, and Navigation Polish
- Styled `BottomNavigationView` in `activity_main.xml` with active tab glowing pill indicator and translucent dark surface matching Stitch design mockups.
- Added `addCustomExerciseToSession(...)` in `WorkoutViewModel.kt` giving users full privilege to add custom exercises and create personalized workout plans.
- Added `deleteMealLog` and `updateMealLog` to `MealLogDao.kt`, `AppRepository.kt`, and `MealViewModel.kt`.
- Added "🖼 Gallery" photo picker to `LogMealFragment.kt` and `ProgressFragment.kt` using `ActivityResultContracts.GetContent()`.
- Added 3-dot overflow menu (`⋮`) on meal items with Delete action in `LogMealFragment.kt`.
- Created custom `WeightTrajectoryView.kt` canvas view drawing a 4-week weight loss curve with `#00E676` glowing gradient fill, data point dots, and date labels.
- Preserved 100% of existing View IDs across all layouts.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-04

### Stitch Weekly Workout Plan UI Translation
- Updated `fragment_workout_plan.xml` matching Stitch design with microcycle header, split badge, telemetric badges (`tvWorkoutMinutes`, `tvExerciseCount`, `tvWorkoutFocus`), today's session card, and weekly schedule breakdown.
- Preserved 100% of existing View IDs across `fragment_workout_plan.xml` and `WorkoutPlanFragment.kt`.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-04

### Logo Vector Integration & LockFragment Loading Fix
- Converted user provided SVG logo into Android Vector Drawable (`ic_kinetiq_logo.xml`).
- Updated `ic_launcher_foreground.xml` and added branding logo `ivLockLogo` to `fragment_lock.xml`.
- Fixed `LockFragment` loading state in `LockViewModel.kt` by wrapping session queries in `try-catch` to ensure lock controls display reliably.
- Preserved 100% of existing View IDs across all layouts.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-03

### Stitch Auth Lock Screen UI Translation
- Updated `fragment_lock.xml` matching Stitch design with glassmorphism card, fingerprint touch sensor, 4 PIN dot indicators, 3x4 numeric keypad, and session security badge.
- Updated `LockFragment.kt` with keypad event handling (number keys 0-9 and backspace) and active PIN dot indicator color updates.
- Preserved 100% of existing View IDs across `fragment_lock.xml` and `LockFragment.kt`.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-10-03

### Stitch Home Dashboard UI Translation
- Defined Stitch kinetic dark color palette (`kinetic_surface`, `kinetic_card_surface`, `kinetic_primary`, `kinetic_secondary`, `kinetic_tertiary`) in `res/values/colors.xml`.
- Updated `fragment_home.xml` matching Stitch design cards, calorie progress ring text, macro progress bars, AI directive callout box (`tvCoachingTip`), and 2-column Bento grid.
- Preserved 100% of existing View IDs across `fragment_home.xml` and `HomeFragment.kt`.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-09-27

### Dashboard AI Coaching Insights & Dynamic Day Scheduling
- Transformed `HomeViewModel.kt` to generate intelligent AI coaching insights (calorie adherence vs `user_targets`, macro gap analysis, weight trend delta, and goal-specific coaching tips).
- Updated `HomeFragment.kt` with interactive card clicks that navigate directly to `workoutPlanFragment`, `logMealFragment`, `progressFragment`, and `profileFragment`.
- Fixed workout day selection in `WorkoutViewModel.kt`: added `currentDayOfWeekIndex()` to dynamically select today's scheduled workout session based on local time or auto-suggest the next session.
- Added custom exercise entry support (`insertExercise` in `ExerciseLibraryDao.kt` and `saveCustomExercise` in `AppRepository.kt`).
- Preserved 100% of existing View IDs across all layouts.
- Verified build status: `:app:assembleDebug` builds cleanly.

## 2026-09-26

### Phase 5 — User Profile & Onboarding
- Created `UserTargetDao.kt` for `user_targets` table queries.
- Added atomic `@Transaction` method `saveFullProfileAndTargets(...)` in `AppRepository.kt` to save `user_profiles`, `dietary_preferences`, and `user_targets` atomically.
- Implemented `ProfileViewModel.kt` with BMR (Mifflin-St Jeor), TDEE, goal-based calorie targets, PRD calorie floors (1200 kcal female / 1500 kcal male), and macro splits.
- Updated `fragment_profile.xml` and `ProfileFragment.kt` with a 4-step onboarding UI (Physical Metrics, Fitness Configuration, Nutrition & Health Disclaimer, Calculated Targets Preview).
- Configured first-launch onboarding routing in `LockFragment.kt` and `nav_graph.xml`.
- Verified profile editing support (preserves existing entity IDs when updating).
- Verified build status: `:app:assembleDebug` builds cleanly.

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