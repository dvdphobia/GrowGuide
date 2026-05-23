# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GrowGuide is a native Android app (Kotlin) for tracking houseplants. Users can add plants, log growth entries, set watering reminders, and chat with an AI (Ollama) for plant care advice. Firebase provides auth, Firestore database, and Storage.

## Build & Development

The project uses Gradle with the Android Gradle Plugin. All commands run from the repository root.

| Task | Command |
|------|---------|
| Compile Kotlin (fast check) | `./gradlew :app:compileDebugKotlin` |
| Build debug APK | `./gradlew :app:assembleDebug` |
| Install debug APK on device | `./gradlew :app:installDebug` |
| Clean build artifacts | `./gradlew clean` |

The app requires `local.properties` with `sdk.dir` and `OLLAMA_API_KEY`. These are gitignored and already configured in the local environment.

## Architecture

The app follows an **Activity-based architecture** with no dependency injection framework, no ViewModels, and no Jetpack Compose. Everything is plain Android SDK + Firebase.

### Package Structure

```
com.growguide.app
├── activities/     # One Activity per screen
├── adapters/       # RecyclerView adapters
├── models/         # Firestore data classes
├── network/        # API clients
└── util/           # Utilities (e.g. BroadcastReceiver)
```

### Screen Flow

`SplashActivity` → checks Firebase Auth state → routes to `LoginActivity` or `MainActivity`

- `LoginActivity` / `RegisterActivity` — Firebase email/password auth
- `MainActivity` — Plant list with real-time Firestore updates, search, offline banner
- `AddPlantActivity` — Form to create a plant; copies photo to app-private storage
- `PlantDetailActivity` — Three sections: plant info, growth log, AI chat
- `ProfileActivity` — Displays current Firebase user metadata

### Firebase Data Model

Firestore collections are namespaced per user:

```
users/{userId}/plants/{plantId}          → Plant
users/{userId}/plants/{plantId}/logs/{id} → LogEntry
users/{userId}/plants/{plantId}/chats/{id} → ChatMessage
```

All model classes use `@PropertyName` annotations for Firestore deserialization and provide default values (required by Firestore's `toObject()`).

### Networking

`OllamaApiClient` is a Kotlin `object` singleton. It uses OkHttp on a background `Thread` to call the Ollama Chat API. The API URL is `https://ollama.com/api/chat`, model is `gpt-oss:120b`. The `Authorization` header uses `Bearer ${BuildConfig.OLLAMA_API_KEY}`, injected at build time from `local.properties`.

### Key Patterns

- **Lifecycle**: Activities register Firestore `addSnapshotListener` in `onStart()` and remove it in `onStop()`. This is the primary pattern for real-time UI updates.
- **UI updates from callbacks**: Firebase and OkHttp callbacks run on background threads. UI updates must go through `runOnUiThread`.
- **View binding**: Activities use `findViewById`, not ViewBinding or Compose.
- **RecyclerView**: Adapters hold mutable lists and call `notifyDataSetChanged()` (no DiffUtil).
- **Markdown rendering**: AI chat replies use `Markwon` to render markdown (bold, lists, etc.) in `ChatAdapter`.
- **Photos**: `AddPlantActivity` copies gallery URIs to `filesDir/photos/` to avoid permission expiration. Plant photos are stored as local file paths, not uploaded to Firebase Storage.

### Watering Reminders

`WateringReminderReceiver` is a `BroadcastReceiver` triggered by `AlarmManager` (scheduling is not yet implemented in Activities). It posts local notifications via `NotificationManager` with a channel ID of `watering_reminders`.

## Security & Sensitive Files

- `local.properties` — contains `OLLAMA_API_KEY` and `sdk.dir`. **Gitignored.** A hook blocks direct edits.
- `app/google-services.json` — Firebase credentials. **Gitignored.** A hook blocks direct edits.
- `local.properties.example` — safe template without real keys.

## Testing

There are currently **no unit tests or instrumented tests** in this project. If adding tests, place them in:
- Unit tests: `app/src/test/java/...`
- Instrumented tests: `app/src/androidTest/java/...`

## Dependencies (key)

- Firebase BOM (Auth, Firestore, Storage)
- OkHttp 4.12.0
- Gson 2.11.0
- Markwon 4.6.2 (Markdown rendering)
- Material Design 3
- AndroidX (AppCompat, RecyclerView, ConstraintLayout)
