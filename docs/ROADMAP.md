# Kinetiq Project Roadmap & Architecture

## Current Implemented Phases (Stable)
- **Phase 1**: Base Android MVVM Foundation & Single-Activity Shell.
- **Phase 2**: Room Database Setup (26 Tables, DAOs, Exercise Seed Data).
- **Phase 3**: Authentication & Session Management (SHA-256 PIN & Biometrics).
- **Phase 4**: Lock Screen & Navigation Polish.
- **Phase 5**: User Profile, BMR/TDEE Onboarding & Target Calculations.
- **Phase 5.1**: Stitch Kinetic UI Design System, Circular Metabolic Engine Ring, Meal Log CRUD & Gallery Picker, 4-Week Weight Trajectory Line Graph.

---

## Active & Upcoming Phases

### Phase 6 — Reusable Weekly Workout Cycle System [PLANNED]
- **Objective**: Make workout plans repeatable week after week without losing historical workout logs.
- **Schema Enhancements**: Add `weekNumber` to track cycle occurrences.
- **UI Actions**: "Start New Week", "Repeat Plan", "Continue Current Week".
- **Data Safety**: Historical `exercise_logs` remain immutable with `completedAt` timestamps for trend analysis.

### Phase 7 — Custom Workout Plan Builder [PLANNED]
- **Objective**: Give users full privilege to create, edit, duplicate, and schedule custom workout plans.
- **Schema Enhancements**: Support `planType = 'CUSTOM_USER'` in `workout_plans`.
- **Features**: Custom plan naming, day selection, exercise picking from `exercise_library`, target sets/reps/rest time configuration, reordering.

### Phase 8 — Profile & Onboarding Redesign [PLANNED]
- **Objective**: Redesign profile setup & onboarding UI matching Stitch export with profile picture `ImageView` (dummy avatar support).

### Phase 9 — Level 1 Local Deterministic Data Analytics [PLANNED]
- **Objective**: Build pure local analytics from Room database without network AI.
- **Metrics**: Weekly completion %, volume trends, set/rep counts, weight loss trajectory, calorie & macro adherence ratios, streak calculation.

---

## Future OpenRouter AI Roadmap (Post-Local First System)

### Phase 10 — OpenRouter AI Infrastructure & Security Layer [PLANNED - NOT IMPLEMENTED YET]
- **Objective**: Introduce a secure, resilient network & API layer for OpenRouter AI calls.
- **Architecture**: App → AI Repository → Proxy Backend / OpenRouter API → Structured JSON Validation → UI.
- **Security**: Local developer key vault for dev; proxy service for production APK security.
- **Resilience**: Rate-limit handling, token cost control, local fallback behavior when offline.

### Phase 11 — Level 2 & 3 AI Progress Analysis & Recommendations [PLANNED - NOT IMPLEMENTED YET]
- **Objective**: Provide AI-assisted interpretation of local Room analytics data.
- **Features**: Weekly progress summaries, macro adjustment suggestions, volume progression advice.
- **Safety**: Deterministic local data remains authoritative; user must confirm any plan or target changes.

### Phase 12 — Level 4 Interactive AI Fitness & Nutrition Coach [PLANNED - NOT IMPLEMENTED YET]
- **Objective**: Natural language conversational coaching using user's actual Room data context.
- **Capabilities**: Answering user queries ("Why is my bench press stalling?", "Suggest a workout for tomorrow").
- **Safety Filters**: No medical diagnosis; injury flags strictly enforced; suggestions clearly distinguished from verified data.
