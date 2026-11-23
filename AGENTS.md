# AGENTS.md — arcana-android

Instructions for any AI coding agent (Claude Code, Codex, Cursor, Copilot, Gemini…) working
in this repository **or in any product created from it**. `CLAUDE.md`, `GEMINI.md` and
`.github/copilot-instructions.md` all point here; this file is the single source.

> **You MUST NOT report a task as done until `tools/arch-check.sh` exits 0 and the Gradle
> commands in §5 pass.** Violations are blocked twice: the pre-commit hook
> (`tools/githooks/pre-commit`) rejects Kotlin files outside the layer packages, `*Impl`
> imports outside `di/`, and ViewModels without Input/Output/Effect; CI (Jenkins stage
> "Architecture Qube", threshold 90) blocks the merge. Skipping this costs the product its
> architecture: a feature written as one Composable that calls Room directly passes the
> compiler and fails both gates after the work is done.

## 0. First command in a fresh clone

Run `tools/install-hooks.sh` once. Git does not install hooks on clone; until you run it the
pre-commit check does not exist and only CI catches a wrong layout — after the work is
written. `tools/new-app.sh` runs it for you.

## 1. Starting something new

| Situation | Do this | Never do this |
|---|---|---|
| New Android product | `tools/new-app.sh <name> <dest-dir> [applicationId]` — copies the whole layered skeleton **plus this file, the hook and the CI gate**, renames package / applicationId / app name / Sonar key / Jenkins names | Create a fresh Android Studio project and copy a few packages into it |
| New feature (screen + data) | `tools/new-feature.sh <Name>` — generates the full vertical slice (§2) with Hilt bindings, Room table + migration, nav route, home entry and unit tests, then edit the placeholder fields | Write a Composable that talks to a DAO or API, or put a new feature in one file |
| New field on a feature | Change model, entity, mapper, migration SQL and tests together | Change only the entity (Room fails at runtime, not compile time) |

Cost of copying parts instead of the skeleton: the product loses this file, the hook and the
CI gate, and nothing tells the next agent the rules exist.

## 2. Layers — where a file goes

All production Kotlin lives under `app/src/main/java/<namespace>/` (template namespace
`com.example.arcana`, read from `app/build.gradle.kts`). Only `MainActivity.kt` and
`MyApplication.kt` sit at the root. Tests mirror the same packages under `app/src/test/java/`.

| Layer | Package | Contains | May depend on |
|---|---|---|---|
| Entry | `<ns>` root | `MainActivity` (`@AndroidEntryPoint`), `MyApplication` (`@HiltAndroidApp`) | nav, ui.theme, core, sync |
| Navigation | `nav/` | `NavGraph` — routes, destinations, home entries | ui.screens, core.analytics |
| View | `ui/screens/*Screen.kt`, `ui/theme/` | Composables: collect `output`, send `Input`, show `Effect`. No logic | its ViewModel, domain.model, ui.theme |
| ViewModel | `ui/screens/*ViewModel.kt` | `@HiltViewModel`, extends `AnalyticsViewModel`; `Input` / `Output` / `Effect` + `onEvent(input)` | **interfaces** in domain.service, domain.model, core |
| Service (use cases) | `domain/service/<X>Service.kt` + `domain/service/impl/<X>ServiceImpl.kt` | business rules, validation, orchestration | domain.repository **interfaces**, domain.model, domain.validation, sync, core |
| Domain | `domain/model/`, `domain/repository/` (interfaces, `CacheEventBus`), `domain/validation/` | pure Kotlin models and contracts | core |
| Data | `data/repository/impl/`, `data/local/` (Room `AppDatabase`, `dao/`, `entity/`, `mapper/`, `migration/`), `data/network/` + `data/remote/` (Ktorfit API), `data/worker/`, `data/analytics/` | repository implementations: Room is the source of truth, network syncs into it, `CachingDataRepository` adds the memory cache | domain, core, sync |
| Sync | `sync/` | `Synchronizer`, `SyncManager`, `SyncWorker` (WorkManager) | core |
| DI | `di/` | Hilt `@Module`s — **the only place that names `*Impl` classes** | everything |
| Core | `core/common/`, `core/analytics/` | `AppError`, `NetworkMonitor`, `RetryPolicy`, `StringProvider`, analytics | nothing above it |

Direction is one way: view → ViewModel → service → repository interface ← data implementation.
A lower layer never imports a higher one; ui never imports `data.*`.

## 3. Rules

| # | Rule | Enforced by |
|---|---|---|
| R1 | Every new Kotlin file lives in one of the layer packages of §2 (`core, data, di, domain, nav, sync, ui`). | pre-commit hook; arch-qube "Layer Direction Enforcement" |
| R2 | Depend on interfaces. Only Hilt modules in `di/` import a class from an `.impl.` package or named `*Impl`; bind it with `@Binds`. Tests may construct impls directly. | pre-commit hook; arch-qube "Only DI Container Imports Impl", "Interface/Impl Colocation", "Implementation Naming Convention" |
| R3 | `domain/` is pure Kotlin. New persistent data gets a Room `*Entity` in `data/local/entity/` plus a mapper in `data/local/mapper/`; do not annotate domain models with `@Entity`. (`User`/`UserChange` are legacy exceptions — do not copy them.) | arch-qube "Layer Direction Enforcement" (imports); the entity rule is on you |
| R4 | A ViewModel declares `sealed interface Input`, `data class Output`, `sealed interface Effect`, exposes `output: StateFlow<Output>` and `effect` (Channel → Flow), and has one entry `fun onEvent(input: Input)`. | pre-commit hook (arch-qube marks its MVVM I/O/E rules NOT EVALUATED) |
| R5 | Composables never call a service, repository or DAO; they render `output` and send `Input`. Business logic lives in the service, not the ViewModel or the repository. | arch-qube "View Cannot Call Service", "Unidirectional Data Flow", "No Business Logic in Boundary" |
| R6 | Offline-first: the UI observes a Room `Flow`; writes go to Room first, the network syncs via `sync/`. Do not return network results straight to the UI. | review (on you) |
| R7 | Every new service, repository impl and ViewModel has a unit test under `app/src/test/java/` (Mockito-Kotlin, `runTest`, Turbine for effects). | arch-qube "Test Coverage Threshold"; Sonar coverage gate (80%) in CI |
| R8 | Changing a Room entity bumps `version` in `AppDatabase` and adds a `Migration` in `data/local/migration/` registered in `DatabaseModule`; the SQL must match the entity exactly. | Room at runtime (on you — the unit tests do not open a database) |
| R9 | Screens are registered in `nav/NavGraph.kt` at the `// new-feature:*` markers and reachable from the home screen. Keep the markers; `tools/new-feature.sh` needs them. | `tools/new-feature.sh` refuses to run without them |
| R10 | User-visible text goes in `res/values*/strings.xml`; ViewModels read it through `StringProvider`. No secrets or API keys in source. | Sonar; review |

## 4. Worked example

The User feature shows every layer: `UserScreen` → `UserViewModel` → `UserService` /
`UserServiceImpl` → `DataRepository` ← `CachingDataRepository` → `OfflineFirstDataRepository`
(Room `UserDao` + `UserNetworkDataSource`), wired in `di/ServiceModule.kt` and
`di/RepositoryModule.kt`. For a clean new slice, run `tools/new-feature.sh Sample` in a scratch
branch and read what it generates.

## 5. Before you say "done"

Run these, in this order, and paste the last line of each into your summary:

```bash
./gradlew testDebugUnitTest --no-daemon     # CI stage "Unit Tests with Coverage"
./gradlew assembleDebug --no-daemon         # CI stage "Gradle Build"
tools/arch-check.sh                         # CI stage "Architecture Qube": same image, framework android, threshold 90
```

Outside the CI image, `gradle.properties` points aapt2 at `/opt/android-sdk/...`; add
`-Pandroid.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/36.0.0/aapt2` to both commands.

`arch-check.sh` exit 1 means fail; exit 2 means it could not run (no Docker / image) — that
is **not** a pass, say so. Do not lower `--threshold`, add exclusions, or move code to dodge a
rule — fix the structure. CI also runs SonarQube (coverage ≥ 80%) which you cannot run
locally; keep R7.

## 6. Reference

- Detailed patterns: `arcana-android-developer-skill`
  (https://github.com/jrjohn/arcana-skills/tree/main/arcana-android-developer-skill).
  Optional detail only. This file is the source of truth: if the skill and AGENTS.md
  disagree, AGENTS.md wins.
