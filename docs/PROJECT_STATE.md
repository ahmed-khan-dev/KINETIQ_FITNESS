# Kinetiq Project State

## Current Status

The Phase 6 data-flow correction is in place. The user reports that workout exercises now load during a workout. The plan preview and completion-summary additions are implemented but still need a device check.

## Verified by Source Inspection

- `ExerciseSeedData` defines 15 exercise records.
- `AppDatabase` registers a first-create callback that inserts those records when the exercise table is empty.
- `WorkoutViewModel` generates persisted workout sessions and ordered session-exercise rows linked to exercise IDs.
- The runtime data loss was in navigation/state ownership: `WorkoutSessionFragment` created a separate `WorkoutViewModel` and called `prepareSession(0)` without loading the selected session. That view model's in-memory session list was empty, so the UI rendered its default zero-valued state.

## Phase 6 Changes in This Revision

- The Workout plan now passes today's persisted session ID to the workout screen.
- The workout screen loads the selected session, ordered session-exercise rows, and matching library exercise records from Room.
- Completing a set stores one performance record with reps and optional weight, along with a GPS record marked available or unavailable. Progress is restored from saved exercise logs when the session is reopened.
- The plan screen now lists each saved exercise with target muscle, sets, reps, and rest. It derives completion progress from saved set logs and refreshes when returning from the session.
- Today's workout card displays saved progress and changes to a completed state after all planned sets are logged.
- Existing entity, table, column, screen, and view IDs were retained.

## Verification Status

- `:app:compileDebugKotlin` passed after the plan preview and completion-state changes.
- `:app:assembleDebug` succeeded after Gradle JVM criteria was set to the JetBrains vendor at JDK 21.
- The user reports that real exercises load in the active workout. The expanded plan preview and completed-state indicator have not yet been checked on device.
- GPS fallback and saved-log restoration across app restart still need explicit device verification.

Phase 6 core flow is user-reported working; retain the remaining device checks above before calling the full phase verified.

## Phase 7 — Meal Logging (Implemented; Device Verification Pending)

- Added a CameraX meal photo preview and capture controller.
- Meal photos are written directly under the application's private `files/images/meals` directory.
- Added manual meal name, slot, calories, protein, carbohydrate, and fat entry with recent saved meals displayed in the Meal tab.
- Meal records store the private photo path and GPS log ID in Room; GPS unavailability does not prevent saving.
- Added a Room migration from database version 1 to 2 for meal name and nutrition columns.
- `:app:assembleDebug` succeeded with the Phase 7 implementation.
- Device verification of CameraX capture, Room migration on an installed version 1 database, GPS fallback, and saved-meal restoration remains pending.

## Phase 8 — Progress Tracking (Implemented; Device Verification Pending)

- Added weight entry and recent weight history backed by the existing `weight_logs` table.
- Added CameraX capture for Front, Side, and Back progress photos.
- Progress photos are stored under the app-private `files/images/progress` directory; Room stores their paths and capture timestamps.
- Weight and photo entries each reference a GPS log. Saving continues if permission is declined or a location fix is unavailable.
- `:app:assembleDebug` succeeded with the Phase 8 implementation.
- Device verification of weight/photo persistence, camera capture, angle selection, and GPS fallback remains pending.

## Input Validation Hardening Before Phase 9

- Profile setup and editing validate age, height, weight, the required medical disclaimer, and lengths of optional allergy/medical restriction text.
- Meal logging validates meal name and calories. Optional macro fields allow blank values but reject malformed, negative, non-finite, or excessive values.
- Workout set logging validates reps and any optional weight before saving.
- PIN entry requires exactly four numeric digits before authentication or first-time PIN setup.
- Progress weight logging validates its numeric range and displays an inline field error.
- `:app:assembleDebug` succeeded after these changes. Device validation of the error states remains pending.

## Phase 9 — Home Dashboard (Implemented; Device Verification Pending)

- Home reads the first saved weekly workout and displays exercise set progress and completion state.
- Home summarizes meals logged during the current local calendar day, including calories and macros.
- Completing every set in a workout session updates the saved workout streak. The dashboard shows the current active streak and longest streak.
- Home previews the latest saved progress photo and its angle/date, and shows the latest GPS coordinates or unavailable status.
- Added quick navigation actions to Workout, Meal, and Progress.
- `:app:assembleDebug` succeeded. Device verification of the dashboard, daily refresh, streak updates, photo preview, and GPS status remains pending.
