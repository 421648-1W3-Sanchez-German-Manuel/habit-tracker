# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

Monorepo with two independent sub-projects:

- `habit-tracker-backend/` — Spring Boot 3.3 (Java 21, Maven) REST API
- `habit-tracker-frontend/` — Expo / React Native (TypeScript) app

Each sub-project has its own README; the top-level [README.md](README.md) is the quick-start that ties them together. There is no parent Maven/npm workspace — commands must be run from each sub-project's directory.

## Common commands

All commands assume PowerShell on Windows (this is a Windows dev environment per the README). Substitute `cp` for `Copy-Item` on bash.

### Backend (`habit-tracker-backend/`)

```powershell
# First-time setup
Copy-Item .env.example .env          # then edit .env with real values
docker compose up -d mongo postgres mongo-express ollama

# Run the API (reads .env via spring-boot-maven-plugin)
mvn spring-boot:run

# Tests
mvn test                                              # full suite
mvn -Dtest=HabitServiceTest test                      # single class
mvn -Dtest=HabitServiceTest#methodName test           # single method

# Build a jar
mvn clean package

# Stop infra
docker compose down
```

The Spring Boot Maven plugin pins JVM timezone to `America/Argentina/Buenos_Aires` via `app.jvm.timezone` (see [pom.xml](habit-tracker-backend/pom.xml:23)). When reproducing date/streak bugs, set the same TZ.

There is **no `application.yml` / `application.properties`** — all Spring config comes from `.env` (loaded by the Boot plugin). When adding configuration, follow this pattern: add the env var to `.env.example`, reference it via `${...}` from either Spring's built-in keys (e.g. `SPRING_DATA_MONGODB_URI`) or an `@ConfigurationProperties` class under `config/`.

### Frontend (`habit-tracker-frontend/`)

```powershell
npm install
Copy-Item .env.example .env          # set EXPO_PUBLIC_API_URL

npm run start                        # expo start -c (clears cache)
npm run android
npm run ios
npm run web
```

There is no `npm test` and no linter configured.

`EXPO_PUBLIC_API_URL` is required at startup — [services/config.ts](habit-tracker-frontend/services/config.ts) throws if missing. On native devices it auto-rewrites `localhost` to the Metro bundler host (or `10.0.2.2` on Android emulator), so a value of `http://localhost:8080` usually works in dev without manual LAN-IP juggling.

## Architecture

### Backend persistence split (important)

This is the central architectural decision and the source of most non-obvious behavior:

- **`User`** lives in **PostgreSQL** (JPA / `UserRepository`). PK is a String UUID generated on insert.
- **`Habit`** and **`HabitLog`** live in **MongoDB** (Spring Data Mongo). `Habit.userId` is the String UUID — there is no DB-level FK; cross-store consistency is enforced only in service code.
- A habit can be a "default" template (`isDefault=true`, no `userId`) or a user-owned habit. When a user adopts a default, the new habit gets a `sourceDefaultHabitId` pointer back to the template.
- `Habit.embedding` is a `List<Double>` vector produced by Ollama (`llama3` embeddings via `/api/embeddings`), used for similarity dedupe.

When touching habit creation or queries, remember that "user's habits" almost always means "owned habits + defaults" — see `HabitRepository.findAllByUserIdOrIsDefaultTrue` and the `findMostSimilarHabitForUserOrDefault` flow in [HabitSimilarityService.java](habit-tracker-backend/src/main/java/com/tp1/habittracker/service/HabitSimilarityService.java).

### Backend request flow

1. `SecurityConfig` — stateless, CSRF off, JWT-only. Only `/auth/login` and `/auth/register` are public; everything else requires a Bearer token validated by `JwtAuthenticationFilter`. CORS allowed origins come from `app.cors.allowed-origin-patterns` (default `http://localhost:*,http://127.0.0.1:*`).
2. Controllers (`controller/`) extract the authenticated user id with `authentication.getName()` — this is the Postgres user UUID, populated by `JwtService`. Never trust path-param `userId`; controllers reject mismatches as 404 (see `HabitController.getHabitsByUser`).
3. Services (`service/`) orchestrate the cross-store work. `HabitService` writes to Mongo but reads `User` from Postgres to validate ownership. `HabitSimilarityService` first does a deterministic name-normalized match, then falls back to Ollama embeddings + cosine similarity (`SimilarityUtils.cosineSimilarity`) thresholded by `HabitSimilarityProperties`.
4. `GlobalExceptionHandler` maps domain exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `UpstreamBadResponseException`, `UpstreamServiceUnavailableException`) to consistent `ApiErrorResponse` payloads.
5. `StartupDataSeeder` (gated by `APP_SEED_ENABLED`) inserts demo data only when **all** collections/tables are empty.

### Ollama integration

The backend talks to a local Ollama container for embeddings. `OllamaClient` uses Spring WebFlux's `WebClient` (configured in `WebClientConfig`, base URL from `APP_OLLAMA_BASE_URL`) and translates 5xx → `UpstreamServiceUnavailableException`, 4xx → `UpstreamBadResponseException`. The Ollama container does not pre-pull the `llama3` model — first call will be slow or fail until the model is pulled (`docker exec habit-tracker-ollama ollama pull llama3`).

### Frontend structure

- `navigation/RootNavigator.tsx` — top-level switch between `AuthNavigator` and `MainTabNavigator` based on Zustand `authStore`. Waits for `hasHydrated` before rendering so a persisted token doesn't flash the login screen.
- `store/authStore.ts` — Zustand store; token is persisted via `expo-secure-store`. `fetchCurrentUser` runs once after hydration if a token exists but the user object is missing.
- `services/` — thin axios wrappers over the backend. All requests go through `httpClient` (`services/httpClient.ts`) which reads `config.apiUrl`. Auth header is attached by the store/interceptor pattern in `authService`.
- `screens/` and `components/` — UI only; business logic lives in `services/` and `store/`.

## Conventions worth knowing

- Lombok is used heavily on the backend (`@Data`, `@Builder`, `@RequiredArgsConstructor`). The Maven compiler plugin's annotation processor path is configured explicitly — IDEs need Lombok installed to compile.
- The Mongo port is **27018** (host) → 27017 (container), and Postgres is **5434** → 5432. Connection strings in `.env.example` reflect these mapped ports — don't "fix" them to 27017/5432.
- Backend uses `Authentication` directly from the SecurityContext rather than `@AuthenticationPrincipal` — keep that pattern; helpers like `extractAuthenticatedUserId` exist in each controller.
- `application.yml` does not exist; do not add one without checking whether the same value can live in `.env` instead.
