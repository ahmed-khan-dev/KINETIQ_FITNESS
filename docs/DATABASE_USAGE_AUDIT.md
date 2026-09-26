# Kinetiq Database Usage Audit

## Scope

Static source audit of the Room schema registered by `AppDatabase` and its use from DAOs, `AppRepository`, and the UI/ViewModels. This does not inspect a live device database or verify user data. It records the current code state to support the post-MVP redesign.

## Summary

- The database registers 26 entities. Fourteen have active app read/write paths, `MealLogItemEntity` has a partial path that current UI never exercises, and eleven have no active DAO/repository/UI path.
- The app currently uses a single hard-coded `default_user_id`; several schemas imply multiple accounts but there is no active account registration or server sync.
- Most ID-looking columns are plain strings. They are not declared as Room foreign keys, so the database does not enforce that referenced rows exist.
- Schema export is disabled (`exportSchema = false`). The database is version 2 with a single 1-to-2 migration for meal nutrition columns.

## Active entities and unused or underused fields

| Entity/table | Current use | Stored fields with no current consumer or incomplete use | Possible future use |
|---|---|---|---|
| `UserProfileEntity` / `user_profiles` | Profile screen reads/writes metrics, goal, fitness/activity levels, equipment, session duration and days/week. Workout generation consumes those settings. | `createdAt` is not displayed or queried; `updatedAt` is written on save but not used for conflict handling or history. | Keep timestamps for profile sync/change history, or remove them if the personal version stays strictly local and single-profile. |
| `DietaryPreferenceEntity` / `dietary_preferences` | Diet type, allergies, medical flags and disclaimer time are stored and restored by profile setup/editing. The disclaimer time gates setup. | `dietType` and `allergies` do not affect meal suggestions yet. **Bug:** workout generation parses `allergies` as exercise medical restriction flags; `medicalFlags` is the field that should drive exercise exclusions. `createdAt` is not consumed. | Use allergies and diet type in recipe/meal suggestions; use `medicalFlags` for exercise filtering. Keep the disclaimer timestamp as an audit record. |
| `UserTargetEntity` / `user_targets` | BMR, TDEE, calorie and macro targets are saved and shown in profile summary. `effectiveDate` orders the latest target. | Target history is not actually retained: profile save reuses `existingTargetId`, replacing the prior row. `createdAt` is not consumed. | Insert a new target row when targets change; use effective dates for historical charts and day-specific nutrition goals. |
| `ExerciseLibraryEntity` / `exercise_library` | Name, muscle group, equipment, difficulty, instructions, exclusion tags and default sets feed plan generation and workout display. | `metValue`, `videoUrl`, and `progressionType` are never consumed. `defaultReps` and `defaultRestSec` are seeded but the generator computes its own values from goal and ignores them. Timestamps are not consumed. | Use MET values for a better calorie estimate; show exercise demonstrations; use progression type and per-exercise defaults when generating plans. |
| `WorkoutPlanEntity` / `workout_plans` | Active plan lookup uses `userId` and `status`; plan ID links sessions; split type is displayed; `createdAt` selects the newest active plan. | `weekStartDate` is written but never used to select a current week. `updatedAt` is not consumed. Older plans are not clearly archived when a new plan is generated. | Enforce week boundaries, retain plan history, mark replaced plans inactive, and let users compare plan versions. |
| `WorkoutSessionEntity` / `workout_sessions` | Plan ID, day number, session name and estimated duration are used for weekly plan display and Home. | `estimatedCalories` is calculated and stored but never displayed or used. Timestamps are not consumed. | Display estimates with a clear estimate label or replace them with actual estimates based on exercise/MET data and logged duration. |
| `SessionExerciseEntity` / `session_exercises` | Session/exercise links, ordering, target sets/reps/rest are used in plan and active workout screens. | Timestamps are not consumed. No declared foreign keys enforce the `sessionId`/`exerciseId` references. | Add enforced relationships/cascade policy; use `updatedAt` if users can edit or replace individual planned exercises. |
| `ExerciseLogEntity` / `exercise_logs` | Set log text, session-exercise link, user and completion time are used to restore and show workout progress. GPS log ID is written. | `rpe` is always saved as the constant `7`; the user never provides or sees it. GPS links are not resolved by the workout/history UI. `createdAt`/`updatedAt` are not used. `loggedSets` encodes set count, reps and weight in text, so analytics must parse strings. | Collect RPE when useful. For analytics, migrate to typed set rows/columns for reps, load, set type and completion time. Link the GPS row only if workout location history is a real product feature. |
| `MealLogEntity` / `meal_logs` | Meal name, slot, calories, macros, image path and logged time are used by Meal and Home. User ID scopes queries. GPS ID is written. | `recognitionConfidence` is never populated or read. GPS links are not shown with a meal. Timestamps are not used for sync/conflict handling. | Use confidence if image recognition is added; expose per-meal location only if useful and consented. |
| `WeightLogEntity` / `weight_logs` | Weight and logged time are saved and listed in Progress. | `gpsLogId` is saved but never followed to a GPS row. `createdAt`/`updatedAt` are unused. | Show optional location/accuracy context or remove the GPS association if it has no personal value. |
| `ProgressPhotoEntity` / `progress_photos` | Private photo path, angle, and capture time are saved and displayed in Progress/Home. | `gpsLogId` is saved but never followed. `createdAt`/`updatedAt` are unused. The DB path points into app-private files and can become stale if files are removed/restored independently. | Add explicit photo deletion/cleanup and backup/restore behavior; retain photo location only if the user wants it. |
| `GpsLogEntity` / `gps_logs` | Coordinates, timestamp and availability are written by location capture; Home shows latest coordinates or unavailable status. | `accuracy` is used to choose among last-known fixes but not shown to users. Saved record links are not used to show location in the associated workout, meal, weight, or photo. | Show accuracy/context and resolve location by owner row; consider retention and privacy controls. |
| `StreakEntity` / `streaks` | Current streak, longest streak and last-active date are updated on full workout completion and displayed on Home. | `createdAt`/`updatedAt` are stored but not displayed or used for sync. The streak is workout-only, not meal/photo activity. | Keep the definition explicit; optionally add activity types or weekly goals if that fits the personal app. |
| `AppSessionEntity` / `app_sessions` | PIN hash, authenticated state, user ID and expiry timestamp drive the local lock screen. Expiry and last-active values are written. | `lastActiveTimestamp` is not used by lock validation; `SecurityUtils.isSessionExpired(...)` is currently unused. Lock logic checks `sessionExpiryTimestamp` only. The row `id` is not used as a lookup key; `userId` is. | Decide whether the timeout is sliding or fixed and enforce one policy. Keep only necessary session fields. Do not treat this local PIN record as an online account system. |

### Cross-cutting fields

- `createdAt` and `updatedAt` are present on many entities. They are usually defaulted on insert and not read. Some save paths explicitly update `updatedAt`, but no general sync/conflict mechanism uses them. They are useful if sync/history is planned; otherwise they add schema and write work without current UI value.
- `id` primary keys are used by Room and several parent/child links. Keep them when rows need stable identity; a field not displayed directly is not thereby unused.
- `userId` is used in most user-scoped queries, but code supplies `default_user_id` rather than selecting an account. Multi-user support needs account lifecycle and consistent ownership, not just the column.

## Registered but dormant or partially scaffolded entities

| Entity/table | Current database fields | Current state | Possible future use |
|---|---|---|---|
| `UserEntity` / `users` | ID, email, password hash, OAuth provider/ID, role, active flag, timestamps. | `UserDao` declares insert/get methods, but neither is called. App authentication uses a local PIN in `app_sessions`; these account fields do not currently create or authenticate a user. | Add real local/cloud account lifecycle only if needed. Otherwise remove this unused auth schema in a planned migration. |
| `NutritionItemEntity` / `nutrition_items` | Name, source, calories/macros per 100g, optional fiber/micronutrients, timestamps. | Entity is registered; no DAO or app code uses it. | Food catalog for search, barcode lookup, and macro calculation. `micronutrients` as a single string will be hard to query; normalize if nutrient-level filtering is needed. |
| `MealLogItemEntity` / `meal_log_items` | Parent meal, optional nutrition item, quantity, calories/macros, timestamps. | DAO/repository can insert items, but current meal screen saves totals directly on `MealLogEntity` and passes no items. Read methods are unused. | Ingredient/food-level meal entry with grams and per-item macros. This can replace manual total-only entry once catalog/search exists. |
| `RecipeEntity` / `recipes` | Name, diet type, slot, prep time, servings, per-serving calories/macros, instructions, timestamps. | Registered but has no DAO or UI path. | Recipe catalog and meal recommendations filtered by diet, available time and target macros. |
| `RecipeIngredientEntity` / `recipe_ingredients` | Recipe ID, nutrition item ID, quantity, timestamps. | Registered but has no DAO or UI path; links are not enforced. | Normalize ingredients and calculate serving nutrition from `nutrition_items`. |
| `MealPlanEntity` / `meal_plans` | User, date, recipe ID list, daily calories/macros, timestamps. | Registered but unused. `recipeIds` is a serialized string rather than a relational link. | Save planned meals by date. Prefer a join entity for plan-to-recipe membership when implementing it. |
| `DailySummaryEntity` / `daily_summaries` | Date, completed/planned workouts, calories in/target, macros, timestamps. | Registered but unused. Home currently computes meal totals from logs and does not persist a daily aggregate. | Cache daily dashboard totals or power charts once measuring and invalidating the aggregate is defined. Raw logs should remain the source of truth. |
| `BadgeEntity` / `badges` | Code, title, description, icon path, trigger rule, timestamps. | Registered but unused; no catalog seeding or trigger evaluator. | Achievement definitions and award rules. Store executable/typed rule configuration rather than relying on arbitrary rule strings. |
| `UserBadgeEntity` / `user_badges` | User, badge, award time, timestamps. | Registered but unused; no DAO or award flow. | Persist unlocked achievements and show an award history. |
| `NotificationPreferenceEntity` / `notification_preferences` | Workout, meal, hydration, check-in toggles and quiet-hours values. | Registered but unused; no permission, scheduler or preference UI. | Configure local reminders and quiet hours after notification delivery behavior is designed. |
| `FriendshipEntity` / `friendships` | User, friend, status and timestamps. | Registered but unused; no DAO, account backend or social UI. | Social features only if desired; would require account identity, invitations, privacy and server-side trust rules. |
| `AppSettingsEntity` / `app_settings` | Generic key/value. | Entity and DAO exist; repository exposes the DAO, but no code reads or writes settings. | Persist a small set of app-only preferences such as units or theme. Avoid duplicating profile and notification settings here. |

## DAO/repository APIs with no current callers

- `UserDao.insertUser` and `getUserById` (the profile methods on the same DAO are used through the repository).
- Flow wrappers for profile, dietary preferences, user targets and exercise catalog; their screens currently use one-shot loads or suspend queries.
- Exercise catalog `getAllExercisesFlow` and `getExerciseById`; workout generation reads the full list with a suspend query.
- Active-plan/session/exercise flows and exercise-log date-range flows/count query; history and calendar screens could use these.
- `MealLogDao.getMealLogItems` and `getTodayTotalCalories`; Home currently sums parent meal totals itself.
- Both `WeightLogDao.getLatestWeight` and `getLatestWeightFlow`; Progress currently collects the full history.
- `ProgressPhotoDao.getLatestProgressPhotoFlow`; Home currently uses its suspend latest-row query.
- `GpsLogDao.getGpsLogById`; the stored GPS IDs are currently not followed.
- `AppSessionDao.getSessionFlow` and `StreakDao.getStreakFlow`; current lock/Home reads are one-shot.
- Both `AppSettingsDao` methods; no app setting currently uses them.

Unused APIs are not necessarily harmful, but they make it harder to tell which feature paths are real. During the personal redesign, remove dead wrappers or connect them to a named feature rather than keeping parallel query styles without a caller.

## Recommended post-submission decisions

1. Fix the `medicalFlags`/`allergies` wiring before relying on workout restriction behavior.
2. Decide which dormant features matter to the personal version: nutrition catalog/food-level meals, recipe plans, daily analytics, notifications, badges, friendship/social, and account identity.
3. Decide whether GPS on every set/meal/weight/photo has enough value to justify collecting and retaining it; currently the parent IDs are saved but their location rows are not displayed.
4. Replace string-encoded `loggedSets` with typed set records if workout history, volume/progression charts, or exports are wanted.
5. Decide whether target/plan history matters. Current target saves overwrite the existing target row; generated workout plans can accumulate as active rows.
6. For any schema changes, add a Room migration and preserve user data. Enable schema export and commit schema snapshots before the personal redesign starts changing tables.
