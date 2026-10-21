# Agent Guidelines & Project Architecture: FashionScape Bot (`fsbot`)

Welcome to the **FashionScape Bot** repository. This document serves as a comprehensive onboarding, architecture, and operational guide for autonomous agents and engineers contributing to this codebase.

---

## 1. Project Overview

**FashionScape Bot** is a Discord bot tailored for the RuneScape community (supporting both Old School RuneScape / OSRS and RuneScape 3 / RS3), focused on character fashion, cosmetics, and outfit sharing.

### Core Features
- **Outfit Galleries & Ingestion:** Watches designated gallery channels for image uploads, pushes them to Imgur (in production), registers them in an internal database, and facilitates user rating/voting via emoji reactions.
- **Outfit Showcase & Search (`~outfit`):** Allows users to search and showcase random or tagged outfits, filtered by submitter or tag.
- **ScapeFashion API Integration (`~osrssearch`, `~rs3search`):** Queries [scape.fashion](https://scape.fashion) and [rune.scape.fashion](https://rune.scape.fashion) for equipment items by color hex/name and equipment slot.
- **Role & Server Management:** Color roles (`~color`, `~colors`), welcoming new members, achievement tracking, and server configuration.
- **Moderation & Permissions:** Blacklist management (servers and users), staff/developer commands, custom permissions, and soft-delete/restoration of submitted outfits.

---

## 2. Technology Stack

| Component | Technology | Details / Notes |
|---|---|---|
| **Language & Runtime** | Java 16 / Java 21 | `sourceCompatibility = 16` in `build.gradle`; local VM may use OpenJDK 21. |
| **Build System** | Gradle (Wrapper 7.2) | Note Gradle 7.2 / Java 21 compatibility considerations (see Section 6). |
| **Discord Library** | Javacord (`3.8.0`) | Gateway intents include `Intent.MESSAGE_CONTENT`. Branch `3-jda-refactor` exists for JDA migration. |
| **Application Framework** | Spring Boot (`2.5.4`) | Runs web starter & actuator on port 8080 (health checks / planned API). |
| **Database** | SQLite via JDBC (`3.41.2.2`) | Connection pooling via `SQLiteConnectionPoolDataSource`. PostgreSQL driver also present. |
| **HTTP Clients** | OkHttp 4 (`4.9.1`) & Java 11 `HttpClient` | OkHttp for Imgur; Java 11 `HttpClient` for ScapeFashion API. |
| **Configuration** | Kaaz `ConfigurationBuilder` (`0.4`) | Parses `~/.fsbot/bot.config`. |
| **JSON Parsing** | Gson (`2.8.8`) & `org.json` | Gson for ScapeFashion responses; `org.json` for Imgur. |
| **Logging** | SLF4J with Logback (`1.2.5`) | Structured console logging configured in `src/main/resources/logback.xml`. |
| **Containerization** | Docker | Multi-stage build (`gradle:7.2.0-jdk16` -> `openjdk:16`). |

---

## 3. Repository Architecture & Layout

```
.
├── Dockerfile                   # Multi-stage production container build
├── Makefile                     # Helper targets for Docker builds and running
├── build.gradle                 # Gradle dependencies and build plugins
├── settings.gradle              # Gradle project settings
├── privacy.md                   # Bot privacy policy for Discord compliance
└── src
    └── main
        ├── java/dev/salmonllama/fsbot
        │   ├── Main.java               # Application bootstrap (Config, DB, Guthix, Listeners, Spring)
        │   ├── commands/               # Command implementations categorized by domain
        │   │   ├── developer/          # Bot owner/dev commands (Test, Invite, Permission, etc.)
        │   │   ├── general/            # User commands (Help, Ping, Stats, Outfit, Color, etc.)
        │   │   ├── osrssearch/         # OSRS item slot and color search commands
        │   │   ├── rs3search/          # RS3 item slot and color search commands
        │   │   └── staff/              # Moderation commands (Retag, Blacklist, OutfitInfo, etc.)
        │   ├── config/                 # BotConfig and Google SecretManager integration
        │   ├── database/               # Database access layer
        │   │   ├── controllers/        # Async controllers (Outfit, Gallery, Blacklist, Config, etc.)
        │   │   ├── models/             # Schema definitions and data entities
        │   │   ├── DatabaseModel.java  # Abstract base model
        │   │   ├── DatabaseProvider.java # JDBC connection pool and query executor
        │   │   └── FSDB.java           # Central database initialization and registry
        │   ├── endpoints/              # Third-party API clients
        │   │   ├── imgur/              # Imgur upload and deletion API client
        │   │   └── scapefashion/       # ScapeFashion item/color HTTP endpoints
        │   ├── exceptions/             # Custom exception types
        │   ├── guthix/                 # Custom command framework and dispatcher ("Guthix")
        │   ├── listeners/              # Discord gateway event listeners
        │   ├── services/               # Helper services (IdService, MemberRoleService)
        │   └── utilities/              # Constants, assertions, warnings, and Discord utilities
        └── resources
            └── logback.xml             # Logback console pattern & logger configuration
```

---

## 4. Key Subsystems & Design Patterns

### 4.1 Guthix (Command Framework)
The custom command dispatcher is named **Guthix** (`dev.salmonllama.fsbot.guthix`):
- **`Guthix`**: Implements `MessageCreateListener`. Validates non-bot authors, checks if the author is blacklisted (`UserBlacklistController`), verifies the configured command prefix, resolves the command via `Registry`, evaluates permissions via `PermissionManager`, and executes `Command.invoke(CommandContext)`.
- **`Command`**: Abstract base class. Each command defines its `name`, `description`, `usage`, `category`, `permission`, `aliases`, and `onCommand(CommandContext)`.
- **`PermissionManager`**: Validates user access based on `PermissionType` (`NONE`, `OWNER`, `ROLE`, `STATIC`, `PERMISSION`). `STATIC` permissions are stored in the database.

### 4.2 Gallery & Outfit Lifecycle
1. **Submission (`ImageListener`):**
   - Listens for messages with image attachments in configured gallery channels (`GalleryController`).
   - If `ENVIRONMENT` env var is set (Production): Uploads image to Imgur using `ImgurAPIConnection`, captures Imgur link and `deleteHash`, then saves an `Outfit` record in SQLite.
   - If in Development: Directly saves the Discord image URL to SQLite without calling Imgur.
2. **Reactions (`ThumbsListener`):**
   - Adds default reaction emojis (e.g. `:heartpulse:`) or thumbs up/down to allow server members to vote.
3. **Retrieval (`OutfitCommand`):**
   - Users run `~outfit` (or search by tag/author) to view randomly selected community outfits formatted in Discord embeds.
4. **Moderation (`ForceDeleteCommand`, `RemoveOutfitCommand`, `RestoreOutfitCommand`):**
   - Outfits are soft-deleted by updating the `deleted` flag.

### 4.3 Database Architecture
- **Schema Management:** Each model in `dev.salmonllama.fsbot.database.models` defines a static `schema()` method returning raw SQL `CREATE TABLE IF NOT EXISTS` statements. Tables are initialized on startup in `FSDB.prepareTables()`.
- **Controllers:** Controllers in `database.controllers` provide asynchronous facades using `CompletableFuture.supplyAsync()` wrapping JDBC calls in `DatabaseProvider`.

---

## 5. Environment & Configuration

### Configuration File (`~/.fsbot/bot.config`)
The application looks for configuration in `$HOME/.fsbot/bot.config` (`Constants.BOT_FOLDER`):
```properties
DB_ADDR=fsbot.db
DB_NAME=fsbot
DEFAULT_PREFIX=~
BOT_OWNER=<discord_owner_id>
STAFF_ROLE=<staff_role_id>
REPORT_CHANNEL=<report_channel_id>
ACHIEVEMENT_CHANNEL=<achievement_channel_id>
WELCOME_CHANNEL=<welcome_channel_id>
OUTFIT_LOG=<outfit_log_channel_id>
BOT_TOKEN_DEV=<dev_token>
BOT_TOKEN_PROD=<prod_token>
IMGUR_CLIENT=<imgur_client_id>
IMGUR_BEARER=<imgur_bearer_token>
```
> [!IMPORTANT]
> If `bot.config` is missing or fails to parse, `BotConfig.initConfig` will print a stack trace and terminate with `System.exit(1)`. Always ensure `~/.fsbot/bot.config` is initialized before running the application.

### Environment Variables
- `ENVIRONMENT`: If non-null (e.g. `ENVIRONMENT=PROD`), activates production tokens (`BOT_TOKEN_PROD`) and enables Imgur API uploads. If unset, development mode is used.
- `GOOGLE_APPLICATION_CREDENTIALS`: Path to Google Cloud credentials service account JSON if utilizing GCP Secret Manager.

---

## 6. Build, Development & Execution Workflows

### Execution Environment Rules
When working in an Android Laptop / Linux VM environment:
- **I/O-Heavy Task Cache Redirection:** Always redirect `./gradlew` cache away from virtiofs/FUSE mounted shared directories to native ext4 storage:
  ```bash
  PROJECT_CACHE_DIR="$HOME/.cache/projects/$(basename "$PWD")-$(pwd -P | sha1sum | cut -c1-8)"
  ./gradlew <task> --project-cache-dir="$PROJECT_CACHE_DIR/gradle"
  ```
- **Java Version Compatibility:**
  The project is configured for Java 16 with Gradle 7.2. Modern systems running JDK 21+ may encounter compatibility issues with Gradle 7.2. When modernizing:
  - Upgrade Gradle wrapper to `8.5+` or use a compatible JDK 16/17 runtime.

### Running with Docker
The repository includes a multi-stage `Dockerfile`:
```bash
docker build -t fashionscape-bot .
docker run -p 8080:8080 -e ENVIRONMENT=PROD -v ~/.fsbot:/root/.fsbot fashionscape-bot
```

---

## 7. Known Technical Debt & Modernization Roadmap

When working on features or refactors, consider the following areas of technical debt:

1. **Discord Slash Commands & JDA Migration:**
   - The bot currently relies solely on legacy prefix commands (`~command`) and message content parsing via Javacord `3.8.0`.
   - Branch `origin/3-jda-refactor` contains early work to migrate to Java Discord API (JDA).
   - Moving to Discord Application Commands (Slash Commands) is strongly advised for Discord API compliance and user experience.

2. **Async & Concurrency in Database Calls:**
   - Several controllers wrap blocking JDBC calls in `CompletableFuture`, but callers frequently invoke `.join()` synchronously on gateway listener threads (e.g., `UserBlacklistController.exists().join()` in `Guthix.onMessageCreate` and `StaticPermissionController.getByUser().join()`).
   - SQLite uses file-level locking. Under heavy concurrent message traffic, multiple threads writing simultaneously can encounter locking issues.

3. **Spring Boot Utilization:**
   - Spring Boot (`2.5.4`) is started in `Main.java` alongside Javacord, but dependency injection (`@Service`, `@Repository`, `@Autowired`) is not used. The bot components are mostly instantiated statically or manually.
   - Either refactor the bot components into idiomatic Spring beans or streamline the build by removing unused Spring dependencies if only a lightweight HTTP server is needed.

4. **Testing Suite:**
   - Currently, there are no automated tests under `src/test`.
   - Priority should be given to creating unit tests for command parsing (`RegistryCommand`), permission evaluation, and ScapeFashion API response mapping.

5. **Secret Management:**
   - `SecretManager.java` (Google Cloud Secret Manager) is currently sidelined in favor of `bot.config`. Unify secret loading to support standard environment variables, `.env`, or GCP secrets cleanly.
