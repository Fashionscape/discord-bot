# Ideas to imlpement eventually

This is a combination of both things that I want to do, and features that were removed from production in favor of easier v3 streamlining.

### Member Role
**What:** Give a new user a Member role a set time after joining the server. This member role gives that user the ability to talk in channels. The role and the timeout should be set by Administrators via bot commands. Enabled and configured on a per-server basis.
**Why:** We often have to shield the server against bot accounts, trolls, or spammers. Not being able to talk in any channel upon joining filters out the vast majority of these accounts. 

### Achievement Listener
**What:** A channel-based automated reaction engine that adds a celebratory reaction to people's posts in a staff-determined achievements channel. Achievement channel and reaction emoji should be set by Administrators via bot commands. Enabled and configured on a per-server basis.
**Why:** To make it easier for others to celebrate and react to achievements. Foster camaraderie.

### New Member Listener / Welcome Message Engine
**What:** A listener that tracks new joiners to the server. It then spits out a targeted welcome message to that user in a dedicated channel. The message and the channel should be set by Administrators. Enabled and configured on a per-server basis. Used and implemented in conjunction with the Member Role Listener
**Why:** Help new members orient themselves and prevents spammers. Can be used to provide a more seamless onboarding experience.

### Report Listener / Moderation Reporting
**What:** A listener that tracks messages in a dedicated reporting channel. It keeps the channel clear and forwards all reports to the moderation team, in a dedicated moderator-only channel. The reporting channel and the moderator channel should be set by Administrators. Enabled and configured on a per-server basis.
**Why:** Helps moderation by allowing users to reach the entire mod team with one message safely.

### Thumbs Listener / Announcements / News / Votes
**What:** A configurable listener that provides social media-like pre-set reactions to messages. Designed to be used in announcement or similar style channels. Enabled and configured on a per-server basis by the Administrators, should be able to handle up to 5 (or more?) channels and react to each message with a thumbs up/down.
**Why:** Helps start reactions, especially in scenarios where all server members (including staff) have their votes counted.

### Per Server Gallery and Permissions
**What:** Galleries, images, configs, and permissions should be applied at the server level, so that each server can manage its own galleries, images, etc. This prevents server Administrators from being able to manage the assets and configurations of another server. Fashionscape home server still retains master privileges and should have commands available to Fashionscape Administrators only (StaticPermission#FSHomeAdmin) for managing server block lists and potentially even the ability of other servers to create and manage galleries.
**Why:** This prevents cross-moderation, whether by accident or malicious intent. It keeps databases clean, and gives other servers more ownership of their own channels and galleries. It could also potentially be leveraged to provide monetized service tiers by enabling supporting servers to pay for the ability to create galleries.
### Outfit Diversity Engine
**What:** Make `/outfit random` favor outfits with a low `display_count` (times shown), for example by weighting the pick toward the least-shown outfits instead of choosing uniformly. `display_count` is incremented every time `/outfit random` shows an outfit.
**Why:** As the database grows, a uniform random pick keeps repeating some outfits while others are rarely seen. Favoring low-count outfits spreads views across the whole gallery.

## From the October 2026 v3 review

Items 1–3 of that review (cross-server staff permissions, vote integrity, deleted outfits staying public) are tracked separately in the security fix plan. Everything else it found is below.

## Review: Security & Hardening

### Gallery Upload Validation
**What:** Cap attachment size before downloading, allow only `image/png`, `image/jpeg`, `image/gif`, and `image/webp`, and verify the file's magic bytes instead of trusting the filename (`isImage`) and the reported `Content-Type`. Stream to MinIO rather than reading the whole body into memory (`MinioStorageService.uploadOutfitFromUrl`).
**Why:** A large attachment can exhaust the 3.5 GB heap, and arbitrary content types end up served from the public media domain.

### Docker Compose Hardening
**What:** Stop publishing Postgres (5432), MinIO (9000/9001), and the actuator port (8080) on all interfaces; bind to `127.0.0.1` or drop the mappings. Refuse to start with the default `minioadmin` credentials. Run the bot container as a non-root user. Pin `minio/minio` to a release instead of `latest`.
**Why:** The defaults expose the database and object storage to anyone who can reach the host.

### Command Cooldowns & Rate Limits
**What:** Per-user cooldowns on slash commands and ScapeFashion searches.
**Why:** Stops a single user from spamming the bot or hammering the scape.fashion API on our behalf.

## Review: Correctness & Data Integrity

### Atomic Gallery Ingestion
**What:** Make upload → save → showcase post recoverable: delete the MinIO object if the database save fails, and retry or flag outfits whose showcase post failed.
**Why:** Failures today leave orphaned images in storage or outfits nobody ever sees.

### Search Pagination Button IDs
**What:** Store search state server-side (or encode it compactly) instead of putting the raw tag in the button ID.
**Why:** Discord caps component IDs at 100 characters, so long tags throw an error, and a tag containing `:` breaks the parser in `OutfitSlashCommand.handle`.

### Imgur Rehost Fixes
**What:** Disable redirect-following (or reject `removed.png`) in `ImgurToMinioRehostService`, and move `updateOutfitLink` into a separate bean so its `@Transactional` actually applies.
**Why:** Imgur redirects removed images to a placeholder, which the tool would rehost over the real link. The transaction annotation is currently ignored because of self-invocation.

### Cheaper Message Handling
**What:** In `GalleryEventListener.onMessageReceived`, check whether the channel is a gallery before checking the blocklist, and cache gallery channels and blocklist entries.
**Why:** Every message in every server currently costs two database queries.

### Separate Migration Runs From the Bot
**What:** Run the SQLite and Imgur migration CLIs as a separate Spring profile or entrypoint that doesn't start JDA. Turn off `spring.flyway.baseline-on-migrate`.
**Why:** Today they run in the same boot as the live bot in no fixed order. `baseline-on-migrate` silently skips V1 on any non-empty database.

### Gallery Channel Types & Emoji
**What:** Accept forum, announcement, and media channels in `/gallery add`, and either use the stored `emoji` option or remove it.
**Why:** Only plain text channels are allowed today, and `emoji` is saved but never used.

## Review: Infrastructure & Build

### Publish the `latest` Image
**What:** Point CI's `latest` tag at `trunk` (it currently targets `main` and `v3-modernization`).
**Why:** `docker-compose.yml` pulls `:latest`, which is never published from the default branch.

### One Gradle Version
**What:** Pick one Gradle version (the wrapper is 9.7.1, the Dockerfile builds with the `gradle:8.10.2` image, and AGENTS.md says 8.10+) and have the Dockerfile use `./gradlew`.
**Why:** Spring Boot 3.3's plugin is only certified on Gradle 8.x, and the container build doesn't match local or CI builds.

### Dependency Upgrades & Legacy Removal
**What:** Move off Spring Boot 3.3.x (past open-source support). Delete the dead legacy code (`commands/`, `guthix/`, `database/controllers`, `database/models`, `listeners/`, `endpoints/imgur`, `utilities/`) once ported, and drop Javacord, JitPack (`Kaaz:ConfigurationBuilder`), OkHttp, emoji-java, and eventually SQLite.
**Why:** Javacord is unmaintained, and JitPack builds arbitrary GitHub code, which is a supply-chain risk.

### CI Hardening
**What:** Pin actions to commit SHAs, grant `packages: write` only to the publish job, stop publishing images for every branch push, and add Dependabot or Renovate, CodeQL, and image scanning.
**Why:** Smaller blast radius and automatic notice of vulnerable dependencies.

### Container Runtime Settings
**What:** Replace `-Xmx3500m` with `-XX:MaxRAMPercentage`, add a container healthcheck against `/actuator/health`, and switch to JSON logs for production.
**Why:** The heap follows the container's memory limit, orchestrators can see when the bot is unhealthy, and logs can be shipped and searched.

### Trim Gateway Intents
**What:** Drop `GUILD_MEMBERS` and `MemberCachePolicy.ALL` until a feature (welcome messages, member role) needs them.
**Why:** Caching every member costs memory, and Discord requires extra verification for privileged intents once a bot is in more than 100 servers.

### MinIO Bucket Setup
**What:** Configure the bucket's public-read policy in code instead of assuming it, and create the bucket once at startup instead of calling `ensureBucketExists()` on every upload.
**Why:** The public URL only works if the policy was set by hand, and every upload makes an extra round trip.

### Backups
**What:** Scheduled backups of the Postgres and MinIO volumes, with a tested restore.
**Why:** There are none today.

## Review: Features

### Port Remaining Legacy Commands
**What:** Color roles (`/color`, `/colors`), the server-join handler (including leaving blocked servers automatically), help, stats, privacy, retag, edit-meta, and featured outfits.
**Why:** These existed before v3 and haven't been ported to slash commands.

### Moderation Tooling
**What:** Report buttons on outfits, an optional review queue before an outfit is published, and a hard delete that also removes the image.
**Why:** Gives moderators a record and a workflow instead of log lines.

### User Data Self-Service
**What:** A `/mydata` command to export or delete a user's submissions, plus a rewritten `privacy.md` (it still mentions Imgur, old Discord#tag handles, and a 2020 date).
**Why:** Makes data requests self-serve and keeps the policy accurate.

### Discovery & Engagement
**What:** Autocomplete for tags and ScapeFashion item names, an outfit-of-the-week or leaderboard from vote data, and a web gallery on media.fashionscape.org.
**Why:** Makes the gallery easier to browse and gives people a reason to come back.

### Observability
**What:** Micrometer/Prometheus metrics for commands, uploads, and gateway latency; error alerting (e.g. Sentry); per-server feature flags.
**Why:** We currently find out about failures from users.
