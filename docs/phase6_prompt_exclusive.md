FINAL PHASE 6 IMPLEMENTATION PROMPT — KINETIQ

You are implementing Phase 6 — Workout Plan & Workout Logging for the Kinetiq Android application.

IMPORTANT SCOPE RULE

This is the submission version of the project.

The goal is to implement the minimum functional Phase 6 required by the provided Assignment PDF and the relevant MVP requirements in the PRD.

DO NOT over-engineer this phase.

After submission, this project will be developed further for personal use/deployment, and advanced workout customization can be added later.

Therefore:

DO NOT BUILD:
Custom workout builder
User-created exercises
Exercise creation UI
Advanced exercise search
Replace-exercise UI
Drag-and-drop exercise ordering
Advanced workout customization
Complex exercise library management
AI/ML workout generation
Backend/API workout generation
Firebase/cloud workout functionality
Any Phase 7 functionality
Any feature not required for Phase 6
USE SEEDED EXERCISE DATA

The assignment explicitly allows/requests ready-made exercises:

On first open, insert about 15 ready-made exercises so the plan can pick from them.

Therefore, use a small local seeded exercise library (~15 exercises) in Room.

The user does NOT need to manually add exercises for this submission.

1. READ THE PROJECT DOCUMENTATION FIRST

Before modifying anything, read:

AGENTS.md
docs/PRODUCT_REQUIREMENTS.md
docs/DB_SCHEMA.md
docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/PROJECT_STATE.md
docs/CHANGELOG.md

Also inspect the actual current Phase 6 implementation.

Do not assume that existing entities, DAOs, repositories, ViewModels, generators, layouts, or navigation are correct.

2. RESPECT THE EXISTING PROJECT

This is an existing Android Kotlin/XML project.

Current architecture uses:

Kotlin
XML layouts
Room
MVVM
ViewModel
StateFlow
Repository layer
Navigation Component
Bottom navigation

Follow the existing architecture.

VERY IMPORTANT

Do NOT rename existing:

XML IDs
Kotlin classes
packages
Room tables
Room columns
navigation IDs

unless absolutely necessary and supported by the existing architecture.

Do not create duplicate versions of existing entities or screens.

Before adding anything, determine whether an equivalent already exists.

3. PHASE 6 MINIMUM FUNCTIONAL REQUIREMENT

The final Phase 6 flow should be:

Existing Profile
↓
Goal + Days/Week + Equipment
↓
Seeded Exercise Library
↓
Workout Generator
↓
7-Day Workout Plan
↓
Today's Workout
↓
Exercise Detail
↓
Log Sets / Reps / Weight
↓
Complete Workout
↓
Save Workout Log

The user should be able to actually use this flow.

4. EXERCISE SEED DATA

Create/use approximately 15 ready-made exercises.

Do NOT create a UI for adding exercises.

The exercise data should contain enough information for the workout plan and exercise detail screen.

At minimum:

id
name
targetMuscle
equipment
difficulty
defaultSets
defaultRepRange
defaultRestSeconds
instructions/notes

If the existing schema already contains additional relevant fields such as:

MET
videoUrl
priorityScore
progressionType
exclusionTags

use them where appropriate.

Do not create unnecessary fields if they are not needed.

Example seed exercises:

Push-up
Bench Press
Incline Dumbbell Press
Shoulder Press
Lateral Raise
Triceps Pushdown

Lat Pulldown
Seated Cable Row
Barbell/Dumbbell Row
Biceps Curl

Squat
Leg Press
Romanian Deadlift
Leg Curl
Calf Raise

Approximately 15 exercises is sufficient.

Use the existing project's seed-data mechanism if one already exists.

5. VERIFY THAT SEED DATA IS ACTUALLY INSERTED

This is especially important.

The current Phase 6 problem appears to be that the workout UI exists but is not connected to real exercise data.

Make sure:

App starts
↓
Room database created
↓
Exercise seed data inserted
↓
Exercise DAO can query exercises
↓
Workout generator queries those exercises
↓
Generated workout contains real exercises

Do NOT simply hard-code:

"Push A"
"5 exercises"
"15 sets"

in the UI.

Those values must come from actual Room data / generated workout data.

6. WORKOUT GENERATOR

Implement the simplest rules-based generator required by the assignment.

The assignment specifies:

3 days/week → Full Body
4 days/week → Upper/Lower

The PRD additionally describes:

5–6 days/week → Push/Pull/Legs

Implement only what fits the existing Phase 6 architecture and assignment scope.

The generator should:

Read the user's saved profile.
Read:
goal
days/week
equipment
fitness level if already available
relevant medical restrictions if already available
Query the seeded exercise library.
Filter exercises based on available equipment.
Avoid exercises excluded by existing medical restriction logic if that logic already exists.
Select appropriate exercises.
Assign sets/reps/rest.
Create the weekly workout plan.
Persist the generated plan in Room.

Do NOT introduce an ML model.

The PRD explicitly defines the MVP workout generator as a deterministic rules/scoring engine rather than trained ML.

7. WORKOUT PLAN SCREEN

The Workout screen should display real generated data.

For example:

Workout

This Week

Monday
Push
• Bench Press
• Incline Dumbbell Press
• Shoulder Press
• Lateral Raise
• Triceps Pushdown

Tuesday
Pull
• Lat Pulldown
• Cable Row
• Dumbbell Row
• Biceps Curl

Wednesday
Rest

Thursday
Legs
• Squat
• Leg Press
• Romanian Deadlift
• Leg Curl
• Calf Raise

The exact split depends on the user's days/week.

Do not hard-code this exact example.

8. WORKOUT DETAIL

When the user taps a workout:

Workout Detail

show the actual exercises belonging to that generated session.

Each exercise should show at minimum:

Exercise Name
Target Muscle
Sets
Rep Range
Rest

Example:

Bench Press
Chest

3 sets × 8–12 reps
Rest: 75 sec
9. EXERCISE DETAIL / LOGGING

When the user opens an exercise, display:

Exercise Name

Target Muscle
Instructions

Planned:
3 sets × 8–12 reps
Rest: 75 sec

Set    Weight    Reps    Completed
1      40 kg     10      ✓
2      40 kg     9       ✓
3      40 kg     8       ○

The user only needs to log their actual performance.

They do NOT need to customize the exercise itself.

Allow the user to record:

actual weight
actual reps
completed/not completed
RPE if the existing architecture supports it

The PRD specifically describes exercise logging as:

log sets/reps/weight/RPE → exercise logs

so preserve that functionality where practical.

10. SAVE WORKOUT LOG

When a set is completed:

planned set
↓
actual performance
↓
save to Room

When the workout is completed:

Workout Session
↓
completed
↓
actual exercise/set data persisted

The data must survive app restart.

Do not keep workout logs only in ViewModel memory.

11. GPS REQUIREMENT FROM ASSIGNMENT

The assignment requires GPS to be saved when a workout, meal, or progress log is saved.

For workout completion:

Workout completed
↓
obtain current location
↓
save latitude
save longitude
save accuracy
save timestamp

If GPS is unavailable:

gps_ok = false

and the workout should still be saved.

Reuse the project's existing GPS/location repository if it already exists.

Do NOT redesign the entire location architecture just for Phase 6.

12. BOTTOM NAVIGATION

Keep the existing bottom navigation:

Home
Workout
Meal
Progress
Profile

Workout should remain the selected tab when appropriate.

Child navigation should be:

Workout
↓
Workout Detail
↓
Exercise Detail

Do not redesign the application's navigation.

13. ROOM ARCHITECTURE

Use the existing Room architecture.

Conceptually, the minimum relationship should be:

Exercise
↓
Workout Session / Plan
↓
Session Exercise
↓
Workout Set / Exercise Log

But reuse existing entities if equivalent entities already exist.

Do NOT create:

ExerciseEntity2
WorkoutEntity2
WorkoutSessionEntity2

just because the current implementation has a problem.

Fix the existing architecture instead.

14. IMPORTANT — FIX THE CURRENT "0 SETS × 0 REPS" PROBLEM

The current Exercise screen has shown values such as:

0 sets × 0 reps
Rest: 0 sec
Target muscles: not specified
0 / 0 sets completed

This must be fixed.

Trace the complete data flow:

Seed Data
↓
DAO
↓
Repository
↓
Workout Generator
↓
Workout Session
↓
Session Exercise
↓
ViewModel
↓
Exercise Screen

Find exactly where the data is being lost.

Do not fix it by simply putting default values in the XML.

The screen must receive actual data.

15. NO CUSTOMIZATION FOR SUBMISSION

Do NOT implement:

+ Add Exercise
  Replace Exercise
  Create Exercise
  Custom Workout
  Edit Exercise Library
  Drag Exercise
  Exercise Search

These can be added after submission.

For now:

Seeded exercises → generated plan → logging

is enough.

16. DO NOT BREAK PHASE 5

Phase 5 has already been manually checked.

Do NOT modify the existing onboarding/profile behavior unless Phase 6 genuinely requires it.

The existing flow should remain:

First launch:
Lock → Onboarding → Home

Existing user:
Lock → Home

Profile remains:

View saved profile
Edit saved profile

Do not reintroduce the previous onboarding/profile problems.

17. DO NOT START PHASE 7

Stop after Phase 6.

Do not implement:

advanced progress
gamification
notifications
AI meal recognition
meal planning improvements
advanced adaptive AI
social features
deployment
cloud backend

Only complete the required Phase 6 workout functionality.

18. BUILD VERIFICATION

After implementation:

Run:

.\gradlew.bat :app:assembleDebug

Fix all compilation errors.

Also verify:

Room schema compiles
DAOs compile
ViewModels compile
navigation compiles
XML references resolve
binding classes resolve
no duplicate resources
no duplicate Room entities
no unresolved IDs

Do not claim success unless the build actually succeeds.

19. MANUAL TEST CHECKLIST

After the build succeeds, verify this exact flow:

Test 1 — Exercise seed
Launch app
↓
Room initializes
↓
Exercises exist

Confirm that approximately 15 exercises are actually present.

Test 2 — Generate plan
Open Workout
↓
Workout plan appears
↓
Actual exercise names appear

No empty exercises.

Test 3 — Workout detail

Tap today's workout.

Confirm:

Exercise names
Target muscles
Sets
Reps
Rest

are real values.

Test 4 — Exercise logging

Open an exercise.

Enter:

Weight
Reps

Complete a set.

Confirm progress changes from:

0 / 3

to:

1 / 3

etc.

Test 5 — Persistence

Close/reopen the app.

Confirm logged workout data remains.

Test 6 — Workout completion

Complete the workout.

Confirm the session is marked completed and the workout log is persisted.

Test 7 — Different profile settings

Test at least:

3 days/week
4 days/week

and confirm the generated split changes accordingly.

Test 8 — Equipment

Confirm equipment filtering works using the user's existing profile data.

20. DOCUMENTATION

After implementation, update only the relevant project documentation:

PROJECT_STATE.md
CHANGELOG.md

Document:

Phase 6 completed
Seeded exercise library
Workout generation
Workout logging
Room persistence
Manual verification

Do not rewrite unrelated documentation.

FINAL ACCEPTANCE CRITERIA

Phase 6 is complete when:

[✓] ~15 exercises are seeded into Room
[✓] Workout generator uses actual seeded exercises
[✓] User profile affects workout generation
[✓] 7-day workout plan is persisted
[✓] Workout screen shows real exercises
[✓] Workout detail shows sets/reps/rest
[✓] Exercise detail shows real exercise information
[✓] User can log actual weight/reps
[✓] Completed sets are persisted
[✓] Workout completion is persisted
[✓] GPS is attached to workout log according to assignment
[✓] Data survives app restart
[✓] Existing Phase 5 functionality remains intact
[✓] No custom workout builder was added
[✓] No unnecessary architecture was introduced
[✓] No Phase 7 work was started
[✓] :app:assembleDebug succeeds
MOST IMPORTANT INSTRUCTION

Keep Phase 6 minimal and submission-focused.

Do not interpret the larger PRD as a reason to build every possible workout feature.

For this submission:

Seeded exercises + rules-based weekly plan + workout detail + basic workout logging = Phase 6.

After submission, the project can be expanded into a much more powerful personal/deployed fitness application.

Before changing files, inspect the current implementation and reuse what already exists. Do not create parallel architecture simply because the current implementation has a bug.