# Kinetiq — Database Schema
## PRD-Aligned Database Specification

> **Source of truth:** Kinetiq AI Fitness and Diet Coach Product Requirements Document, Section 8 — Database Schema Design.
>
> This file preserves the PRD's table names, relationships, and field terminology. For the Android implementation, these tables will be represented as Room entities using Kotlin naming conventions, while the Room `@Entity(tableName = "...")` should retain the PRD table name.

---

# 1. Schema Rules

The PRD defines a PostgreSQL-oriented schema. It states that:

- Types are illustrative PostgreSQL types.
- All tables include `id` as a UUID primary key.
- All tables include `created_at` and `updated_at` timestamps unless noted.
- Foreign keys establish relationships between user, profile, workout, nutrition, progress, and gamification data.

For the Android assignment, the database is local Room + SQLite rather than PostgreSQL. The PRD schema is therefore the conceptual schema to preserve, while SQLite/Room-compatible Kotlin types should be used in the actual implementation.

---

# 2. Identity and Profile

## 2.1 users

### PRD table
`users`

### Purpose
Stores the application's user identity and authentication-related account information.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `email` | Unique email address |
| `password_hash` | Password hash |
| `oauth_provider` | OAuth provider |
| `oauth_id` | OAuth provider user ID |
| `role` | `user` or `admin` |
| `is_active` | Active/inactive account |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Notes
- `role` values: `user`, `admin`.
- `password_hash` is nullable when `oauth_provider` is set.

---

## 2.2 user_profiles

### PRD table
`user_profiles`

### Purpose
Stores the user's fitness profile.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `age` | User age |
| `gender` | Gender |
| `height_cm` | Height in centimeters |
| `weight_kg` | Weight in kilograms |
| `fitness_level` | Fitness level |
| `goal` | Fitness goal |
| `activity_level` | Activity level |
| `equipment_access` | Available equipment |
| `session_minutes` | Available session duration |
| `days_per_week` | Workout days per week |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (1) user_profiles`

---

## 2.3 dietary_preferences

### PRD table
`dietary_preferences`

### Purpose
Stores diet preferences, allergies, medical restrictions, and disclaimer acknowledgment.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `diet_type` | Diet preference |
| `allergies` | Array/list of allergies |
| `medical_flags` | Array/list of medical restriction flags |
| `disclaimer_ack_at` | Timestamp of disclaimer acknowledgment |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Diet type values specified by PRD

- `vegetarian`
- `vegan`
- `non_vegetarian`
- `keto`
- `high_protein`
- `low_carb`
- `balanced`

### Relationship
`users (1) → (1) dietary_preferences`

---

## 2.4 user_targets

### PRD table
`user_targets`

### Purpose
Stores calculated nutrition/fitness targets over time.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `bmr` | Basal metabolic rate |
| `tdee` | Total daily energy expenditure |
| `calorie_target` | Daily calorie target |
| `protein_g` | Protein target |
| `carbs_g` | Carbohydrate target |
| `fat_g` | Fat target |
| `effective_date` | Date from which target applies |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (N) user_targets`

### Important PRD rule
Targets are recalculated when the profile or weight changes and are historized by `effective_date`.

---

# 3. Workout Domain

## 3.1 exercise_library

### PRD table
`exercise_library`

### Purpose
Stores the curated exercise library used by the workout generator.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `name` | Exercise name |
| `muscle_group` | Target muscle group |
| `equipment_tag` | Equipment requirement/tag |
| `difficulty` | Difficulty level |
| `met_value` | MET value for calorie estimation |
| `video_url` | Instructional video URL |
| `instructions` | Step-by-step instructions |
| `exclusion_tags` | Exercise exclusion tags |
| `progression_type` | Progression strategy/type |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Notes
- Curated seed data.
- `exclusion_tags` are matched against relevant user medical flags.
- Compound movements can be prioritized using the PRD's `priority_score` recommendation, although `priority_score` is discussed in the generator rules and is not listed as a column in the schema table itself.

---

## 3.2 workout_plans

### PRD table
`workout_plans`

### Purpose
Represents one weekly workout-plan cycle.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `week_start_date` | Start date of plan week |
| `split_type` | Workout split type |
| `status` | Plan status |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Status values specified by PRD
- `active`
- `completed`
- `skipped`

### Relationship
`users (1) → (N) workout_plans`

---

## 3.3 workout_sessions

### PRD table
`workout_sessions`

### Purpose
Stores individual workout sessions within a weekly plan.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `plan_id` | Foreign key to `workout_plans` |
| `day_of_week` | Planned day |
| `session_name` | Session name |
| `estimated_duration_min` | Estimated duration |
| `estimated_calories` | Estimated calorie burn |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`workout_plans (1) → (N) workout_sessions`

---

## 3.4 session_exercises

### PRD table
`session_exercises`

### Purpose
Associates exercises with a workout session and stores the prescription for that session.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `session_id` | Foreign key to `workout_sessions` |
| `exercise_id` | Foreign key to `exercise_library` |
| `order_index` | Exercise ordering within session |
| `target_sets` | Prescribed sets |
| `target_reps` | Prescribed reps |
| `target_rest_sec` | Prescribed rest in seconds |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`workout_sessions (1) → (N) session_exercises`

### Relationship
`exercise_library (1) → (N) session_exercises`

---

## 3.5 exercise_logs

### PRD table
`exercise_logs`

### Purpose
Stores what the user actually completed for a prescribed session exercise.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `session_exercise_id` | Foreign key to `session_exercises` |
| `user_id` | Foreign key to `users` |
| `logged_sets` | JSON representation of actual sets |
| `rpe` | Optional rate of perceived exertion |
| `completed_at` | Completion timestamp |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### `logged_sets` example from PRD

```json
[
  {
    "reps": 10,
    "weight_kg": 40
  }
]
```

### Relationship
`users (1) → (N) exercise_logs`

`session_exercises (1) → (N) exercise_logs`

---

# 4. Nutrition Domain

## 4.1 nutrition_items

### PRD table
`nutrition_items`

### Purpose
Stores foods/nutrition items used by recognition, manual entry, and recipes.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `name` | Food name |
| `source` | Source type |
| `calories_per_100g` | Calories per 100g |
| `protein_g` | Protein per 100g |
| `carbs_g` | Carbohydrates per 100g |
| `fat_g` | Fat per 100g |
| `fiber_g` | Fiber |
| `micronutrients` | JSON micronutrient data |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Source values specified by PRD
- `recognized`
- `manual`
- `recipe`

### Notes
`fiber_g` and `micronutrients` are reserved for Post-MVP population.

---

## 4.2 meal_logs

### PRD table
`meal_logs`

### Purpose
Stores a saved meal log.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `photo_url` | Photo reference |
| `logged_at` | Meal log timestamp |
| `meal_slot` | Meal type/slot |
| `recognition_confidence` | Recognition confidence |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Meal slot values specified by PRD
- `breakfast`
- `lunch`
- `dinner`
- `snack`

### Relationship
`users (1) → (N) meal_logs`

---

## 4.3 meal_log_items

### PRD table
`meal_log_items`

### Purpose
Allows a meal log to contain multiple recognized or manually added nutrition items.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `meal_log_id` | Foreign key to `meal_logs` |
| `nutrition_item_id` | Foreign key to `nutrition_items` |
| `quantity_g` | Quantity in grams |
| `calories` | Calculated calories |
| `protein_g` | Calculated protein |
| `carbs_g` | Calculated carbohydrates |
| `fat_g` | Calculated fat |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationships
`meal_logs (1) → (N) meal_log_items`

`nutrition_items (1) → (N) meal_log_items`

---

## 4.4 recipes

### PRD table
`recipes`

### Purpose
Stores curated recipes used for meal-plan generation.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `name` | Recipe name |
| `diet_type` | Supported diet type |
| `meal_slot` | Breakfast/lunch/dinner/snack |
| `prep_time_min` | Preparation time |
| `servings` | Serving count |
| `calories_per_serving` | Calories per serving |
| `protein_g` | Protein per serving |
| `carbs_g` | Carbohydrates per serving |
| `fat_g` | Fat per serving |
| `instructions` | Preparation instructions |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Notes
Seed content is planned for supported diet types.

---

## 4.5 recipe_ingredients

### PRD table
`recipe_ingredients`

### Purpose
Associates recipes with their nutrition items and ingredient quantities.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `recipe_id` | Foreign key to `recipes` |
| `nutrition_item_id` | Foreign key to `nutrition_items` |
| `quantity_g` | Ingredient quantity in grams |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationships
`recipes (1) → (N) recipe_ingredients`

`nutrition_items (1) → (N) recipe_ingredients`

---

## 4.6 meal_plans

### PRD table
`meal_plans`

### Purpose
Stores the generated meal plan for a specific date.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `plan_date` | Plan date |
| `recipe_ids` | List/array of recipe IDs |
| `total_calories` | Total calories |
| `total_protein_g` | Total protein |
| `total_carbs_g` | Total carbohydrates |
| `total_fat_g` | Total fat |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (N) meal_plans`

### PRD note
One row represents one planned day.

---

# 5. Progress, Analytics, and Gamification

## 5.1 weight_logs

### PRD table
`weight_logs`

### Purpose
Stores user weight history.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `weight_kg` | Weight in kilograms |
| `logged_at` | Timestamp |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (N) weight_logs`

### PRD note
Multiple entries per day are permitted. The dashboard uses the latest entry per day.

---

## 5.2 progress_photos

### PRD table
`progress_photos`

### Purpose
Stores progress photos.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `photo_url` | Photo reference |
| `angle` | Photo angle |
| `taken_at` | Photo timestamp |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Angle values specified by PRD
- `front`
- `side`
- `back`

### Relationship
`users (1) → (N) progress_photos`

---

## 5.3 daily_summaries

### PRD table
`daily_summaries`

### Purpose
Precomputed daily dashboard summary data.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `summary_date` | Summary date |
| `workouts_completed` | Completed workout count |
| `workouts_planned` | Planned workout count |
| `calories_in` | Calories consumed |
| `calorie_target` | Calorie target |
| `protein_g` | Protein consumed |
| `carbs_g` | Carbohydrates consumed |
| `fat_g` | Fat consumed |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (N) daily_summaries`

### PRD note
This is precomputed for fast dashboard reads. Raw log tables remain the source of truth.

---

## 5.4 badges

### PRD table
`badges`

### Purpose
Reference/seed table defining available achievement badges.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `code` | Badge code |
| `title` | Badge title |
| `description` | Badge description |
| `icon_url` | Badge icon reference |
| `trigger_rule` | Rule that awards badge |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Notes
Seed/reference table of possible badges.

---

## 5.5 user_badges

### PRD table
`user_badges`

### Purpose
Associates users with earned badges.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `badge_id` | Foreign key to `badges` |
| `awarded_at` | Award timestamp |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (N) user_badges`

`badges (1) → (N) user_badges`

### Constraint
Unique on:

`(user_id, badge_id)`

---

## 5.6 streaks

### PRD table
`streaks`

### Purpose
Stores current and longest user activity streak.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `current_streak` | Current streak count |
| `longest_streak` | Longest streak count |
| `last_active_date` | Last qualifying activity date |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (1) streaks`

### PRD rule
Updated on any qualifying log event.

---

## 5.7 notification_preferences

### PRD table
`notification_preferences`

### Purpose
Stores the user's notification settings.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `workout_reminders` | Workout reminder toggle |
| `meal_reminders` | Meal reminder toggle |
| `hydration_reminders` | Hydration reminder toggle |
| `checkin_reminders` | Progress/check-in reminder toggle |
| `quiet_hours_start` | Quiet-hours start |
| `quiet_hours_end` | Quiet-hours end |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### Relationship
`users (1) → (1) notification_preferences`

---

## 5.8 friendships

### PRD table
`friendships`

### Purpose
Reserved data model for future social/community functionality.

### Fields

| Column | PRD Type / Description |
|---|---|
| `id` | UUID, primary key |
| `user_id` | Foreign key to `users` |
| `friend_id` | Foreign key to `users` |
| `status` | Friendship status |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

### PRD status
Reserved for Post-MVP social features and not exposed through the MVP API.

---

# 6. Entity Relationship Summary

The PRD defines these core relationships:

```text
users (1)
 ├── (1) user_profiles
 ├── (1) dietary_preferences
 ├── (N) user_targets
 │
 ├── (N) workout_plans
 │       └── (N) workout_sessions
 │               └── (N) session_exercises
 │                       └── (N) exercise_logs
 │
 ├── (N) meal_logs
 │       └── (N) meal_log_items
 │               └── (1) nutrition_items
 │
 ├── (N) meal_plans
 │
 ├── (N) weight_logs
 ├── (N) progress_photos
 ├── (N) daily_summaries
 │
 ├── (N) user_badges
 │       └── (1) badges
 │
 ├── (1) streaks
 └── (1) notification_preferences

recipes (1)
 └── (N) recipe_ingredients
         └── (1) nutrition_items
```

Additional relationships from the PRD:

```text
workout_sessions
    └── session_exercises
            └── exercise_library

meal_logs
    └── meal_log_items
            └── nutrition_items

recipe_ingredients
    ├── recipe
    └── nutrition_item

user_badges
    ├── user
    └── badge
```

---

# 7. PRD Table Inventory

The complete PRD Section 8 schema contains:

### Identity/Profile
1. `users`
2. `user_profiles`
3. `dietary_preferences`
4. `user_targets`

### Workout
5. `exercise_library`
6. `workout_plans`
7. `workout_sessions`
8. `session_exercises`
9. `exercise_logs`

### Nutrition
10. `nutrition_items`
11. `meal_logs`
12. `meal_log_items`
13. `recipes`
14. `recipe_ingredients`
15. `meal_plans`

### Progress / Analytics / Gamification
16. `weight_logs`
17. `progress_photos`
18. `daily_summaries`
19. `badges`
20. `user_badges`
21. `streaks`
22. `notification_preferences`
23. `friendships` (reserved)

---

# 8. Android Room Naming Convention

When implementing this schema in Kotlin/Room:

| PRD table | Recommended Kotlin entity |
|---|---|
| `users` | `UserEntity` |
| `user_profiles` | `UserProfileEntity` |
| `dietary_preferences` | `DietaryPreferenceEntity` |
| `user_targets` | `UserTargetEntity` |
| `exercise_library` | `ExerciseLibraryEntity` |
| `workout_plans` | `WorkoutPlanEntity` |
| `workout_sessions` | `WorkoutSessionEntity` |
| `session_exercises` | `SessionExerciseEntity` |
| `exercise_logs` | `ExerciseLogEntity` |
| `nutrition_items` | `NutritionItemEntity` |
| `meal_logs` | `MealLogEntity` |
| `meal_log_items` | `MealLogItemEntity` |
| `recipes` | `RecipeEntity` |
| `recipe_ingredients` | `RecipeIngredientEntity` |
| `meal_plans` | `MealPlanEntity` |
| `weight_logs` | `WeightLogEntity` |
| `progress_photos` | `ProgressPhotoEntity` |
| `daily_summaries` | `DailySummaryEntity` |
| `badges` | `BadgeEntity` |
| `user_badges` | `UserBadgeEntity` |
| `streaks` | `StreakEntity` |
| `notification_preferences` | `NotificationPreferenceEntity` |
| `friendships` | `FriendshipEntity` |

Example:

```kotlin
@Entity(tableName = "exercise_library")
data class ExerciseLibraryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipmentTag: String,
    val difficulty: String,
    val metValue: Double,
    val videoUrl: String?,
    val instructions: String,
    val exclusionTags: String?,
    val progressionType: String
)
```

The exact Kotlin/Room field types may differ from PostgreSQL because this Android assignment uses Room + SQLite.

---

# 9. Assignment Scope vs Full PRD Schema

The provided assignment is intentionally narrower than the full PRD.

The assignment specifically requires local data for:
- profile
- exercises
- weekly plan
- workout logs
- meal logs
- weight
- progress photos
- GPS stamps
- session
- settings

The full PRD additionally specifies:
- `users`
- `dietary_preferences`
- `user_targets`
- `workout_sessions`
- `session_exercises`
- `exercise_logs`
- `nutrition_items`
- `meal_log_items`
- `recipes`
- `recipe_ingredients`
- `meal_plans`
- `daily_summaries`
- `badges`
- `user_badges`
- `streaks`
- `notification_preferences`
- reserved `friendships`

For the assignment implementation, prioritize the required local functionality while preserving the PRD's domain/table terminology.

---

# 10. Important PRD Notes

- `daily_summaries` is a precomputed read model; raw logs remain the source of truth.
- `friendships` is reserved for Post-MVP social features.
- `fiber_g` and `micronutrients` are reserved for Post-MVP nutrition enrichment.
- Progress-photo body-composition estimation is Post-MVP.
- The larger product architecture describes PostgreSQL/cloud storage, but the Android assignment uses local Room/SQLite and offline operation.

---

# 11. Implementation Principle

Do not invent alternative table names when an equivalent PRD table already exists.

Use:

```text
PRD table name
      ↓
Room @Entity(tableName = "PRD_name")
      ↓
Kotlin Entity class
      ↓
DAO
      ↓
Repository
      ↓
ViewModel
      ↓
XML UI
```

The database schema in this file should remain the reference when implementing the Android Room database.
