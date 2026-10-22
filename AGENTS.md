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
| **Language & Runtime** | Java 21 LTS | Configured via `java.toolchain.languageVersion = 21` in `build.gradle`. Uses Java 21 Virtual Threads. |
| **Build System** | Gradle (Wrapper 8.10+) | Gradle wrapper updated for full Java 21 compatibility. |
| **Discord Library** | JDA (`5.1.0`) | Discord API v10 with native slash commands, button interactions, and virtual thread event loop. Javacord retained temporarily for transition. |
| **Application Framework** | Spring Boot (`3.3.4`) | Dependency injection, `@ConfigurationProperties`, Actuator health/metrics, and Spring Data JPA. |
| **Database** | PostgreSQL 16+ & SQLite | PostgreSQL managed via Flyway migrations (`V1__init_schema.sql`) and Spring Data JPA; SQLite retained for legacy migration CLI. |
| **Media Storage** | MinIO S3 (`io.minio:minio:8.5.12`) | Self-hosted S3 object storage for gallery outfit images with public CDN URL resolution; automated Imgur-to-MinIO migration CLI. |
| **Configuration** | `application.yml` + `@ConfigurationProperties` | Centralized type-safe configuration in `BotProperties` (`fsbot.discord.*`, `fsbot.minio.*`). |
| **JSON & HTTP Clients** | Java 21 `HttpClient`, OkHttp 4, Gson 2.11 | Java 21 native client for MinIO ingestion and external APIs; OkHttp/Gson in legacy utilities. |
| **Logging** | SLF4J with Logback | Structured console logging configured in `src/main/resources/logback.xml`. |
| **Containerization** | Docker & Docker Compose | Multi-stage build (`gradle:jdk21` -> `eclipse-temurin:21-jre`); turnkey `docker-compose.yml` with Postgres and MinIO. |

---

## 3. Repository Architecture & Layout

```
.
├── Dockerfile                   # Multi-stage production container build (Temurin 21)
├── docker-compose.yml           # Local dev orchestrating Bot, Postgres 16, and MinIO
├── example.env                  # Environment template for all bot credentials & storage
├── build.gradle                 # Gradle dependencies and build plugins
├── settings.gradle              # Gradle project settings
├── docs/
│   └── v3_architecture_plan.md  # Comprehensive v3 modernization plan and roadmap
├── src
│   ├── main
│   │   ├── java/dev/salmonllama/fsbot
│   │   │   ├── Main.java               # Spring Boot Application bootstrap entrypoint
│   │   │   ├── DiscordBotRunner.java   # Spring CommandLineRunner initializing JDA 5 & Virtual Threads
│   │   │   ├── config/                 # BotProperties record (@ConfigurationProperties(prefix = "fsbot"))
│   │   │   ├── discord/                # JDA 5 Modern Discord Integration
│   │   │   │   ├── commands/           # SlashCommand interface & SlashCommandDispatcher
│   │   │   │   │   └── general/        # Modern slash commands (PingSlashCommand, etc.)
│   │   │   │   ├── components/         # ButtonHandler and interactive UI component routing
│   │   │   │   └── listeners/          # GalleryEventListener with interactive voting & deletion buttons
│   │   │   ├── storage/                # MinIO Object Storage
│   │   │   │   ├── MinioConfiguration.java # MinioClient Spring bean configuration
│   │   │   │   ├── MinioStorageService.java# Asset upload, stream, and CDN public URL resolution
│   │   │   │   └── migration/          # ImgurToMinioRehostCli & ImgurToMinioRehostService
│   │   │   ├── database/               # Database access layer
│   │   │   │   ├── entities/           # JPA entities (OutfitEntity, GalleryChannelEntity, etc.)
│   │   │   │   ├── repositories/       # Spring Data JPA repositories
│   │   │   │   ├── migration/          # SqliteToPostgresMigrationCli & MigrationService
│   │   │   │   ├── controllers/        # Legacy SQLite controllers (in progress of full port)
│   │   │   │   └── models/             # Legacy SQLite schema models
│   │   │   ├── services/               # Core services (BlocklistService, IdService)
│   │   │   ├── commands/               # Legacy text-prefix commands (being ported to slash commands)
│   │   │   ├── guthix/                 # Legacy command dispatcher ("Guthix")
│   │   │   └── endpoints/              # ScapeFashion and Imgur clients
│   │   └── resources
│   │       ├── application.yml         # Spring Boot & Bot configuration properties
│   │       ├── db/migration/           # Flyway PostgreSQL migrations (V1__init_schema.sql)
│   │       └── logback.xml             # Logback console pattern & logger configuration
│   └── test/java/dev/salmonllama/fsbot # Comprehensive JUnit 5 & Spring Boot test suite
```

---

## 4. Key Subsystems & Design Patterns

### 4.1 Modern Discord Interaction & Command Engine (JDA 5)
The primary Discord interface has migrated from legacy Javacord text parsing to **JDA 5.1.0**:
- **`DiscordBotRunner`:** Implements `CommandLineRunner`. Initializes JDA 5 with Java 21 Virtual Thread pools (`Executors.newVirtualThreadPerTaskExecutor()`) for callback, event, and elastic rate-limit execution, alongside a scheduled virtual thread executor for gateway heartbeat and rate-limit scheduling. Automatically bypasses gateway connection during test executions or when tokens are omitted. Cleanly terminates JDA and thread pools via `@PreDestroy`.
- **`SlashCommand` & `SlashCommandDispatcher`:**
  - `SlashCommand` defines `getName()`, `getDescription()`, `buildCommandData()`, and `execute(SlashCommandInteractionEvent)`.
  - `SlashCommandDispatcher` (extends `ListenerAdapter`) injects all Spring beans implementing `SlashCommand` and registers them with Discord on `ReadyEvent`. Supports instant guild command deployment using `fsbot.discord.dev-guild-id`.
  - Intercepts incoming slash interactions, evaluates blocklist status via `BlocklistService`, routes to the corresponding command bean, and provides centralized exception handling.
- **`ButtonHandler`:** Modular interface for UI button components. Dispatched by `SlashCommandDispatcher` based on component ID prefixes.
- **Legacy Guthix (`dev.salmonllama.fsbot.guthix`):** Legacy text-prefix parser (`~command`) maintained temporarily while individual commands are ported to slash commands.

### 4.2 Gallery & Outfit Lifecycle (v3 Modernized)
1. **Submission (`GalleryEventListener`):**
   - Listens for messages with image attachments in channels registered in `GalleryChannelRepository`.
   - Filters out bot authors, webhooks, non-guild messages, and blocked users/servers.
   - Streams image attachments directly into MinIO S3 via `MinioStorageService.uploadOutfitFromUrl()`.
   - Persists an `OutfitEntity` into PostgreSQL via `OutfitRepository`.
2. **Interactive Showcase & Voting Buttons:**
   - Automatically posts an outfit showcase embed into the gallery channel with interactive action row buttons:
     - `Button.primary("gallery:upvote:<id>", "❤️ Upvote (0)")`: dynamically increments `displayCount` in PostgreSQL and updates the button label in place.
     - `Button.danger("gallery:delete:<id>", "🗑️ Delete")`: validates that the interacting user is the submitter or holds staff moderation permissions (`ADMINISTRATOR`, `MANAGE_SERVER`, `MESSAGE_MANAGE`), then soft-deletes the entity (`deleted = true`) and updates the showcase embed.

### 4.3 Database Architecture (PostgreSQL & Flyway)
- **Schema Management:** PostgreSQL schema managed via versioned Flyway migrations located in `src/main/resources/db/migration` (`V1__init_schema.sql`).
- **Data Access:** Spring Data JPA repositories with HikariCP connection pooling.
- **Legacy Migration:** `SqliteToPostgresMigrationService` and `SqliteToPostgresMigrationCli` provide safe migration of legacy `fsbot.db` records into PostgreSQL.

### 4.4 Object Storage Architecture (MinIO S3)
- **`MinioStorageService`:** Configured via `MinioClient` bean in `MinioConfiguration`. Handles bucket initialization, image stream upload, extension resolution, existence checks, and public CDN URL formatting (`https://media.fashionscape.org/outfits/{id}.png`).
- **`ImgurToMinioRehostCli` / `ImgurToMinioRehostService`:** Standalone CLI tool to scan existing outfit records pointing to Imgur, download images, re-upload them to MinIO, and update PostgreSQL URLs.

### 4.5 Security & Blocklist Subsystem
- **`BlocklistService`:** Centralized service that verifies whether users or guilds are prohibited from bot commands and gallery submissions using `BlockedUserRepository` and `BlockedServerRepository`.

---

## 5. Environment & Configuration

Configuration uses standard 12-factor principles via Spring Boot `application.yml` and `@ConfigurationProperties` binding into the `BotProperties` record.

### Key Environment Variables (`example.env`)
```bash
# Discord Gateway
FSBOT_DISCORD_TOKEN=<bot_token>
FSBOT_OWNER_ID=<discord_owner_id>
FSBOT_DEV_GUILD_ID=<optional_dev_guild_for_instant_slash_command_sync>

# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fashionscape
SPRING_DATASOURCE_USERNAME=fsbot
SPRING_DATASOURCE_PASSWORD=fsbot

# MinIO Object Storage
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=fashionscape-outfits
MINIO_PUBLIC_URL=https://media.fashionscape.org
```

---

## 6. Build, Development & Execution Workflows

### Execution Environment Rules
When working in an Android Laptop / Linux VM environment:
- **I/O-Heavy Task Cache Redirection:** Always redirect `./gradlew` cache away from virtiofs/FUSE mounted shared directories to native ext4 storage:
  ```bash
  PROJECT_CACHE_DIR="$HOME/.cache/projects/$(basename "$PWD")-$(pwd -P | sha1sum | cut -c1-8)"
  ./gradlew <task> --project-cache-dir="$PROJECT_CACHE_DIR/gradle"
  ```
- **Java 21 LTS Runtime:** Ensure JDK 21 is active. Gradle tasks run via Gradle wrapper 8.10+.

### Running with Docker Compose
The repository includes a turnkey `docker-compose.yml` orchestrating the bot with PostgreSQL and MinIO:
```bash
docker compose up -d
```

---

## 7. Modernization Roadmap Status (v3)

The project is executing the v3 modernization plan documented in `docs/v3_architecture_plan.md`:

- [x] **Phase 1: Build & Base Infrastructure (`feature/v3-phase1-build`):**
  - Upgraded to Gradle 8.10+, Spring Boot 3.3.4, Java 21 LTS toolchain.
  - Added Docker multi-stage build (Temurin 21) and `docker-compose.yml`.
  - Replaced `bot.config` with `application.yml` and `BotProperties`.
- [x] **Phase 2: Database & SQLite Migration (`feature/v3-phase2-database`):**
  - Implemented PostgreSQL schema via Flyway (`V1__init_schema.sql`).
  - Added JPA entities and Spring Data repositories.
  - Implemented `SqliteToPostgresMigrationService` and CLI runner.
- [x] **Phase 3: Media Storage Migration (`feature/v3-phase3-minio`):**
  - Integrated MinIO Java SDK via `MinioStorageService`.
  - Added direct image stream ingestion from Discord CDN.
  - Implemented `ImgurToMinioRehostCli` and migration service.
- [x] **Phase 4: Discord Interaction & Gateway Refactor (`feature/v3-phase4-jda-interactions`):**
  - Integrated JDA 5.1.0 with Java 21 Virtual Threads.
  - Created `SlashCommand` interface and `SlashCommandDispatcher`.
  - Implemented `GalleryEventListener` with interactive voting and delete buttons.
  - Replaced Javacord initialization in `DiscordBotRunner`.
- [x] **Phase 5: Feature Ports & ScapeFashion Integration (`feature/v3-phase5-commands`):**
  - Port outfit showcase and search commands (`/outfit random`, `/outfit search`, `/outfit info`, `/outfit delete`).
  - Modernize `ScapeFashionClient` using Java 21 `HttpClient` and Jackson/Gson.
  - Port OSRS & RS3 color search commands.
  - Port staff moderation and blocklist commands (`/block user`, `/block server`).
- [ ] **Phase 6: Integration Tests & Final Release (`feature/v3-phase6-testing`):**
  - Testcontainers for PostgreSQL and MinIO end-to-end integration tests.
  - Final PR of `v3-modernization` into `main`.
