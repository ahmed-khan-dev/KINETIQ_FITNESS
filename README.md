# Kinetiq - Fitness & Wellness Android Application

A modern Android fitness application built with Kotlin, featuring user authentication, workout planning, and comprehensive health tracking.

## Project Overview

Kinetiq is a comprehensive fitness and wellness application designed to help users:
- Create and manage personalized workout plans
- Track exercises and set performance metrics
- Monitor dietary preferences and nutrition
- View progress with detailed analytics
- Secure their data with biometric authentication

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (Local SQLite)
- **UI Framework**: Android Jetpack (Navigation, ViewModel, LiveData, StateFlow)
- **Authentication**: Biometric + PIN security
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API 26
- **Target SDK**: API 37

## Project Structure

```
Kinetiq/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/kinetiq/
│   │   │   │   ├── data/               # Data layer (Room, Repository, DAOs)
│   │   │   │   ├── domain/             # Domain models
│   │   │   │   ├── ui/                 # UI layer (Fragments, ViewModels)
│   │   │   │   │   ├── auth/           # Authentication (Lock, Security)
│   │   │   │   │   ├── home/           # Home screen
│   │   │   │   │   ├── profile/        # User profile & onboarding
│   │   │   │   │   ├── workout/        # Workout planning & tracking
│   │   │   │   │   ├── meal/           # Meal logging
│   │   │   │   │   └── progress/       # Progress tracking
│   │   │   │   └── utils/              # Utilities & constants
│   │   │   ├── res/                    # Resources (layouts, strings, colors)
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                # Instrumented tests
│   │   └── test/                       # Unit tests
│   └── build.gradle.kts
├── docs/                               # Project documentation
├── gradle/                             # Gradle configuration
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Key Features

### Phase 5: User Profile & Authentication ✅
- **Biometric Authentication**: Fingerprint/face recognition with PIN fallback
- **User Profiles**: Comprehensive profile with physical metrics, fitness level, and goals
- **Dietary Preferences**: Food preferences and dietary restrictions
- **Calorie & Macro Calculations**: Automatic BMR/TDEE calculations based on profile
- **Onboarding Flow**: First-time setup experience with guided steps
- **Session Management**: 15-minute session timeout for security

### Phase 6A: Workout Planning & Tracking ✅
- **Deterministic Workout Generation**: Rules-based workout plan creation using user profile
- **Weekly Workout Plans**: Structured multi-day splits (Push/Pull/Legs variations)
- **Exercise Library**: Pre-seeded exercise catalog with target muscles and equipment requirements
- **Workout Sessions**: Real-time workout tracking with set/rep logging
- **Medical Restrictions**: Exercise filtering based on medical flags (knee injury, lower-back injury, shoulder injury)
- **Equipment Considerations**: Respects user's available equipment (home gym, full gym, minimal, etc.)

### Planned Features (Phase 7+)
- Advanced exercise customization and replacement
- Workout history and progress analytics
- Meal planning and nutrition tracking
- Photo-based progress tracking
- Social features and badges/streaks

## Build Instructions

### Prerequisites
- Android Studio 2024.1+
- JDK 17+
- Android SDK API 37+
- Gradle 9.5+

### Building

```bash
# Clone the repository
git clone https://github.com/ahmed-khan-dev/KINETIQ_FITNESS.git
cd KINETIQ_FITNESS

# Build debug APK
./gradlew :app:assembleDebug

# Run tests
./gradlew :app:test

# Build release APK (requires keystore)
./gradlew :app:assembleRelease
```

## Running on Emulator/Device

```bash
# Install debug build
./gradlew :app:installDebug

# Run on connected device
adb shell am start -n com.example.kinetiq/.MainActivity
```

## Database Schema

The app uses Room with SQLite for local data persistence:

### Core Entities
- `user_profiles`: User profile data (age, height, weight, goals)
- `dietary_preferences`: Food preferences and restrictions
- `user_targets`: Calculated calorie/macro targets

### Fitness Data
- `exercise_library`: Pre-seeded exercise catalog
- `workout_plans`: Generated weekly workout plans
- `workout_sessions`: Individual workout days
- `session_exercises`: Exercises within a session
- `exercise_logs`: Logged exercise performance

### Additional Features
- `weight_logs`: Weight tracking history
- `progress_photos`: Progress photo storage
- `meal_logs`: Meal tracking
- `app_sessions`: Session management for auth

See `docs/DB_SCHEMA.md` for detailed schema documentation.

## Architecture

### MVVM Pattern
- **Model**: Room entities and data repositories
- **View**: Fragments and XML layouts with view binding
- **ViewModel**: StateFlow-based state management for lifecycle safety

### State Management
- **StateFlow**: Real-time reactive state updates
- **ViewModelScope**: Lifecycle-aware coroutine scope
- **Repository Pattern**: Centralized data access layer

### Navigation
- **Navigation Component**: Fragment-based navigation with nav graph
- **Bottom Navigation**: Top-level tab navigation (Home, Workout, Meal, Progress, Profile)
- **Child Navigation**: Hierarchical navigation within feature modules

## Security Features

- **Biometric Authentication**: Secure fingerprint/face unlock
- **PIN Fallback**: 4-digit PIN for alternative unlock
- **SHA-256 Hashing**: PIN storage with cryptographic hashing
- **Session Timeouts**: Automatic re-lock after 15 minutes of inactivity
- **Local Storage**: All data stored locally in encrypted Room database

## Testing

```bash
# Run unit tests
./gradlew :app:test

# Run instrumented tests (on device/emulator)
./gradlew :app:connectedAndroidTest

# Run with coverage
./gradlew :app:jacocoTestReport
```

## Documentation

Comprehensive documentation is available in the `docs/` directory:
- `PROJECT_STATE.md`: Current project status and implementation details
- `ARCHITECTURE.md`: Architecture decisions and patterns
- `DB_SCHEMA.md`: Complete database schema documentation
- `CHANGELOG.md`: Detailed version history and changes
- `DECISIONS.md`: Key architectural decisions
- `PRODUCTS_REQUIREMNTS.md`: Product requirements document

## Contributing

1. Create a feature branch from `main`
2. Make your changes following the existing code style
3. Test thoroughly on emulator/device
4. Submit a pull request with detailed description

## Known Limitations

- Device-level manual testing of all features remains outstanding
- No external API integrations (fully local/offline)
- No Phase 7+ features implemented yet

## Future Enhancements

- [ ] Advanced exercise customization
- [ ] Workout history analytics
- [ ] Comprehensive meal planning
- [ ] Photo-based progress tracking
- [ ] Social features and community
- [ ] Cloud sync capabilities
- [ ] Export workout data

## License

This project is proprietary. All rights reserved.

## Author

Ahmed Khan
- GitHub: [@ahmed-khan-dev](https://github.com/ahmed-khan-dev)

## Build Status

- Latest Build: ✅ SUCCESS (`:app:assembleDebug`)
- Last Updated: September 26, 2026

---

For detailed information about specific features or implementation, please refer to the documentation in the `docs/` directory or review the source code comments.

