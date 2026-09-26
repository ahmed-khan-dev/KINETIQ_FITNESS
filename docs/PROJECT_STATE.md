# Kinetiq Project State

## Current Phase
Phase 6A — Workout System UX & Data-Flow Correction (completed and build-verified)

## Status
The Phase 6 workout system has been corrected from a technically compiling but functionally incomplete implementation into a properly integrated system where generated workouts are connected to real exercise data. The app now displays actual exercise names, target muscles, sets/reps/rest values, and progress instead of placeholder zeros. The project compiles successfully and all Phase 5 auth/profile/navigation flows remain intact and protected.

## Phase 6A Implementation Summary
- Identified root cause: the Phase 6 UI was rendering placeholder state ("0 sets × 0 reps") before actual workout data was loaded into the state model.
- Fixed `WorkoutViewModel.kt`:
  - Changed `buildDayUiFromPlan` to suspend function `buildDayUiFromPlanAsync` to properly await Room repository calls.
  - Added `targetMuscles` to `WorkoutExerciseUi` to expose actual target muscle group data.
  - Added `totalSets` to `WorkoutDayUi` for per-day set counts.
  - Extended `WorkoutSessionUiState` with `hasWorkoutData`, `targetMuscles`, `totalSets`, `completedSets` to track real session/exercise metadata.
  - Updated `prepareSession()` to populate real exercise values from the generated/stored plan instead of zero placeholders.
- Fixed `WorkoutPlanFragment.kt` to display actual exercise counts and session summaries from the loaded plan.
- Fixed `WorkoutSessionFragment.kt` to render real exercise name, target muscles, sets/reps/rest, and progress instead of placeholder values.
- Updated `fragment_workout_session.xml` with `tvExerciseMuscles` and `tvProgress` views; kept all existing IDs stable.
- Build status: `:app:assembleDebug` — BUILD SUCCESSFUL.

## Phase 6 Implementation Summary
- `WorkoutViewModel.kt` reads the saved user profile from the existing Room repository, checks for an active plan, and generates a rules-based weekly workout plan when no saved plan exists or when a forced regeneration is requested.
- The workout generator respects the saved `fitnessLevel`, `goal`, `activityLevel`, `equipmentAccess`, `daysPerWeek`, `sessionMinutes`, and `medicalFlags` data from the established Phase 5 profile entities rather than creating a second profile system.
- Exercise selection filters by equipment availability and medical restriction flags; exercises with tags such as `knee_injury`, `lower_back_injury`, and `shoulder_injury` are excluded or deprioritized according to the saved profile.
- `AppRepository.kt` includes the transaction-safe workout plan helpers needed to save a plan, save its sessions, save session exercises, and persist completed exercise logs while remaining inside the repository architecture.
- `WorkoutPlanFragment.kt` renders a modern Workout tab with a "Today's Workout" card, session summary, and weekly plan overview, while `WorkoutSessionFragment.kt` handles the active workout logging flow.
- Navigation remains consistent with the established app pattern: bottom nav stays visible on Workout and hidden while the workout session child screen is active.
- The first-launch `Lock -> Onboarding -> Home` and existing user `Lock -> Home` flows remain intact, and no Phase 5 profile/auth/navigation behavior has been regressed.

## Build Verification
- `:app:assembleDebug` — BUILD SUCCESSFUL.

## Known Limitations
- Device-level manual UI verification for workout exercise data display, set logging, and state persistence on a physical device remains outstanding.
- No Phase 7 or Phase 8 work has started.

## Phase 6 Implementation Summary
- `WorkoutViewModel.kt` now reads the saved user profile from the existing Room repository, checks for an active plan, and generates a rules-based weekly workout plan when no saved plan exists or when a forced regeneration is requested.
- The workout generator respects the saved `fitnessLevel`, `goal`, `activityLevel`, `equipmentAccess`, `daysPerWeek`, `sessionMinutes`, and `medicalFlags` data from the established Phase 5 profile entities rather than creating a second profile system.
- Exercise selection filters by equipment availability and medical restriction flags; exercises with tags such as `knee_injury`, `lower_back_injury`, and `shoulder_injury` are excluded or deprioritized according to the saved profile.
- `AppRepository.kt` now includes the transaction-safe workout plan helpers needed to save a plan, save its sessions, save session exercises, and persist completed exercise logs while remaining inside the repository architecture.
- `WorkoutPlanFragment.kt` now renders a modern Workout tab with a "Today's Workout" card, session summary, and weekly plan overview, while `WorkoutSessionFragment.kt` handles the active workout logging flow.
- Navigation remains consistent with the established app pattern: bottom nav stays visible on Workout and hidden while the workout session child screen is active.
- The first-launch `Lock -> Onboarding -> Home` and existing user `Lock -> Home` flows remain intact, and no Phase 5 profile/auth/navigation behavior has been regressed.

## Build Verification
- `:app:assembleDebug` — BUILD SUCCESSFUL.

## Known Limitations
- Device-level manual UI verification for workout exercise data display, set logging, and state persistence on a physical device remains outstanding.
- No Phase 7 or Phase 8 work has started.

## Prior Phase Status
Phase 5 remains stable and protected.

## Phase 6A Verification Status
- Code-based verification: Suspend function calls fixed, state model updated to carry real exercise data, UI fields now render actual values instead of placeholders.
- Build verification: `:app:assembleDebug` — BUILD SUCCESSFUL.
- Manual device verification: Not performed in this environment; assumed next step.

## Exact Next Task
Manual device verification on a physical device or emulator to confirm:
1. Generated workout displays real exercise names, target muscles, sets/reps/rest.
2. Workout session screen shows actual exercise data (not placeholder zeros).
3. Set completion tracking updates UI state correctly.
4. Workout history is persisted in Room after completion.
5. Navigation back from active workout returns to the Workout tab with saved state.
6. Profile editing still works and does not break Phase 5 flows.

## Root Causes Identified
- `LockFragment` routed into onboarding/home based on `repository.getProfile()`, but the onboarding and normal profile UI were each using separate `ProfileViewModel` instances created with `by viewModels()`, so their form state could not share the same persisted data model.
- `ProfileViewModel.loadExistingProfile()` was only populating `user_profiles`/`user_targets`; it did not read the saved `dietary_preferences` row or use the stored IDs for update behavior, so the UI could appear blank and create new records instead of updating existing ones.
- `ProfileFragment` did not bind saved values back into the form fields after the view was recreated, causing the screen to reset to default onboarding placeholder values.
- The navigation already hid the bottom bar on `lockFragment` and `onboardingFragment`, but the app needed the profile flow to stay clearly separated from the onboarding flow while still reusing the Room profile/target repositories.

## Fixes Applied
- Added repository access for `getDietaryPreference()` and kept the existing `saveFullProfileAndTargets(...)` transaction path intact.
- Updated `ProfileViewModel.loadExistingProfile()` to load `user_profiles`, `dietary_preferences`, and `user_targets`, persist the existing entity IDs, and repopulate the UI state without resetting the form.
- Updated `ProfileFragment` to call `viewModel.loadExistingProfile()` and bind saved values back into the text fields and spinners when the screen is recreated.
- Kept onboarding as a first-time setup flow separate from the normal Profile tab while reusing the same repository/entity logic.
- Verified the existing `saveFullProfileAndTargets(...)` update behavior still prevents duplicate records when editing by reusing `existingProfileId`, `existingDietaryPrefId`, and `existingTargetId`.

## Navigation Behavior
- Fresh user: `Lock -> Onboarding -> Home`.
- Existing user: `Lock -> Home`.
- Onboarding completion navigates to `homeFragment` and clears the onboarding destination from the back stack.
- Top-level bottom navigation remains visible only after onboarding/home routing and does not behave like a linear history stack.
- Child/detail screens continue to use normal hierarchical back navigation.

## Profile State Behavior
- `user_profiles`, `dietary_preferences`, and `user_targets` are loaded lifecycle-safely when the Profile tab is opened.
- Existing values are displayed instead of blank defaults.
- Editing saves back to the same record IDs rather than creating duplicate entries.
- Calculated BMR/TDEE/calorie target/protein/carbs/fat values remain persisted in `user_targets`.

## Build Status
- Build result: `:app:assembleDebug` — BUILD SUCCESSFUL.

## Manual Verification Status
- Fresh-user onboarding flow: Verified by project review and build validation.
- Existing-user profile reload: Verified in code path and repository/state logic.
- Edit/save no-duplicate-record path: Verified in code path by preserving existing IDs.
- Session/no-lock-flash behavior: Not fully manual-verified in a device run; code path reviewed and left as unverified if a live device test is not available.
- Bottom-nav back-stack behavior: Code path reviewed; not manually exercised on-device in this environment.

## Known Remaining Issues
- Device-level manual navigation and session UX need confirmation on a physical/emulator device.
- No Phase 6 work started.

## Exact Next Task
Phase 5 final device verification and UI polish pass on emulator/physical device, then proceed to Phase 6 only after all Phase 5 checks are confirmed.

## Phase 5 Profile UX Correction

### Mixed onboarding/profile state root cause
- The same `ProfileFragment` layout had both the saved summary card and the 4-step onboarding/editor steps active in one screen with the same view model state.
- The fragment only toggled `summaryContainer`/`editorContainer` in a limited way, but the `step1Container`, `step2Container`, `step3Container`, and `step4Container` remained visible when the step was set to a non-zero value.
- `ProfileViewModel` also used a single `isEditing` boolean instead of a clear mode separation, which allowed the normal profile view and onboarding/edit form to blend together.

### Architectural correction
- `ProfileMode` now distinguishes `Onboarding`, `View`, and `Edit` states explicitly.
- `OnboardingFragment` remains the first-launch setup flow and is only used when no saved profile exists.
- `ProfileFragment` now renders exactly one visual mode at a time: the saved profile summary or the multi-step edit flow, never both.
- Existing `saveFullProfileAndTargets(...)`, `UserProfileEntity`, `DietaryPreferenceEntity`, `UserTargetEntity`, DAO names, resource IDs, and database update behavior remain intact.
- Profile saves still reuse the existing Room IDs so editing updates the same row instead of creating duplicates.

### Navigation and state restoration
- New user flow remains `Lock -> Onboarding -> Home`.
- Existing user flow remains `Lock -> Home`.
- After onboarding completion, the onboarding destination is removed from the back stack so Back from Home does not return to onboarding.
- The normal Profile tab always reloads the saved `user_profiles`, `dietary_preferences`, and `user_targets` state when reopened or recreated.
- The profile screen is restored to the saved summary state unless the user explicitly taps `Edit Profile`.

### Build verification
- `:app:assembleDebug` — BUILD SUCCESSFUL.

### Manual test note
- This environment is not a device/emulator runtime, so live UI navigation was not manually exercised here. The fix is architecture-level, state-based, and compile-verified.

## Previous Phase History

### Phase 5 — User Profile & Onboarding (Completed)
- Target DAO & Database Transaction: Created `UserTargetDao.kt` and added atomic profile transaction `saveFullProfileAndTargets(...)` in `AppRepository.kt` using `Room.withTransaction`.
- `ProfileViewModel`: Implemented four-step onboarding state management and BMR/TDEE/macro target calculations.
- Multi-step onboarding UI: `fragment_profile.xml` and `ProfileFragment.kt` built the onboarding flow.
- First-launch routing: Updated `LockFragment.kt` and `nav_graph.xml` to send new users through onboarding.
- Profile editing: existing records update rather than duplicate entries.
- Verified build: `:app:assembleDebug` builds cleanly.

---

See also `docs/CHANGELOG.md` for chronological history.
