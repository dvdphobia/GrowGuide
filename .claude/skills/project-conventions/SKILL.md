---
name: project-conventions
description: Architecture and coding conventions for the GrowGuide Android app. Read this before adding or modifying Kotlin code.
user-invocable: false
---

# GrowGuide Project Conventions

## Architecture

GrowGuide is a **native Android app** using an **Activity-based architecture** with the following package structure:

```
com.growguide.app
├── activities/     # One Activity per screen (Splash, Login, Register, Main, Profile, AddPlant, PlantDetail)
├── adapters/       # RecyclerView adapters (PlantAdapter, ChatAdapter, LogAdapter)
├── models/         # Data classes (Plant, ChatMessage, LogEntry)
├── network/        # API clients (OllamaApiClient)
└── util/           # Utilities (WateringReminderReceiver)
```

## Patterns

### Activities
- Extend `AppCompatActivity`
- Use `ViewBinding` or `findViewById` for views
- Firebase Auth checks happen in `onStart()` or `onResume()` for auth-gated screens
- AI chat flows pass `plantName`, `plantType`, `plantNotes` to `OllamaApiClient.sendMessage()`

### Adapters
- Extend `RecyclerView.Adapter`
- ViewHolders are inner classes named `ViewHolder`
- Data binding is manual (no Jetpack Compose)

### Models
- Plain Kotlin data classes
- Firestore models use no-argument constructors (Firestore requirement)
- `ChatMessage` supports both user and AI roles

### Network
- `OllamaApiClient` is a Kotlin `object` (singleton)
- Uses OkHttp on a background `Thread`
- API key injected via `BuildConfig.OLLAMA_API_KEY` from `local.properties`
- Gson for JSON serialization

## Firebase Usage

- **Auth**: Email/password authentication
- **Firestore**: Stores `plants` and `chat_messages` collections
- **Storage**: Plant images uploaded to Firebase Storage
- **Cloud Messaging**: Not currently used

## UI Conventions

- Material Design 3 components
- Custom drawables for chat bubbles (`bg_chat_user.xml`, `bg_chat_ai.xml`)
- `Markwon` library renders markdown in AI chat responses
- Night mode colors defined in `values-night/colors.xml`

## Dependency Injection

None. Dependencies are instantiated directly in Activities (e.g., `OllamaApiClient.sendMessage(...)`).

## Testing

Currently no unit tests or instrumented tests. If adding tests:
- Unit tests: `app/src/test/java/...`
- Instrumented tests: `app/src/androidTest/java/...`

## Security Notes

- `local.properties` and `google-services.json` are gitignored
- `OLLAMA_API_KEY` is compiled into `BuildConfig` — do not log or expose it
- Firebase Auth state is the gate for protected screens
