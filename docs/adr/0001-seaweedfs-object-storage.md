# ADR 0001: Use SeaweedFS for object storage

- **Status:** Accepted
- **Date:** 2026-10-24
- **Deciders:** matrix

## Context

The bot keeps every outfit image in S3-compatible object storage. Images are publicly readable under `outfits/` and served at `https://media.fashionscape.org`. Images of deleted outfits are moved to `deleted/outfits/`, where they are kept privately (see `SeaweedStorageService` and `OutfitService.delete`). That split depends on a bucket policy that allows anonymous `s3:GetObject` on `outfits/*` only.

Until now the storage server was MinIO, both in `docker-compose.yml` and in the Testcontainers integration tests. MinIO's community edition is no longer usable as a dependency:

- MinIO stopped publishing free community builds in October 2025. The repository was marked unmaintained in February 2026 and archived in April 2026.
- The `minio/minio` images were deleted from Docker Hub in September 2026.
- `quay.io/minio` started requiring authentication shortly afterwards. CI could no longer pull the image, and every MinIO-backed test failed.
- The last community release, `RELEASE.2025-09-07`, will not receive security fixes.

Interim fixes didn't hold up. Pinning quay.io stopped working once quay required a login. Chainguard's free `cgr.dev/chainguard/minio` image works, but only offers an unpinned `latest` tag and still tracks a project that is no longer maintained upstream.

## Decision

Replace MinIO with **SeaweedFS** in docker-compose and in the integration tests. Run it as a single node with `weed mini`, which serves the master, volume, filer and S3 gateway from one container (`chrislusf/seaweedfs`, pinned to a release).

Replace the **MinIO Java client** (`io.minio:minio`) with the **AWS SDK for Java v2** (`software.amazon.awssdk:s3`). The MinIO client is a generic S3 client, but it is maintained by the same company that archived the server, and against SeaweedFS it failed to parse one of the gateway's XML responses in our first CI run. The AWS SDK is the reference S3 client: it is actively maintained, it is the client SeaweedFS's own S3 tests use, and choosing it means the bot no longer depends on anything from MinIO. The bucket policy and the quarantine logic are unchanged; only the calls that carry them out are.

The client is configured for a self-hosted S3 gateway (`SeaweedConfiguration`):

- `endpointOverride` points at `SEAWEED_ENDPOINT`, and `forcePathStyle(true)` addresses buckets as `/{bucket}/{key}`.
- The region is fixed to `us-east-1`. SeaweedFS ignores it, but the SDK needs one to sign requests.
- Checksums are calculated and validated only when an operation requires them (`WHEN_REQUIRED`). Since SDK 2.30 the default adds CRC checksum headers to every upload, which third-party S3 servers don't all accept.
- It uses the SDK's default Apache HttpClient 5 transport (`apache5-client`). The Netty transport is only for the async API, so it is excluded in `build.gradle`. (Under Spring Boot 3.3 the Apache transport was excluded too, in favour of the JDK-based `url-connection-client`, because Boot 3.3 managed an HttpClient 5 older than the SDK needs. Spring Boot 4.1 manages a new enough one.)

Name configuration after the server, not the client:

| Before | After |
|---|---|
| `fsbot.minio.*` | `fsbot.seaweed.*` |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MINIO_PUBLIC_URL` | `SEAWEED_ENDPOINT`, `SEAWEED_ACCESS_KEY`, `SEAWEED_SECRET_KEY`, `SEAWEED_BUCKET`, `SEAWEED_PUBLIC_URL` |
| `BotProperties.MinioProperties` / `minio()` | `BotProperties.SeaweedProperties` / `seaweed()` |
| compose service `minio`, volume `minio_data`, port 9000 | compose service `seaweed`, volume `seaweed_data`, port 8333 |
| `MinioConfiguration`, `MinioStorageService` | `SeaweedConfiguration`, `SeaweedStorageService` |
| `ImgurToMinioRehostCli`, `ImgurToMinioRehostService` | `ImgurToSeaweedRehostCli`, `ImgurToSeaweedRehostService` |
| `MinioClient` bean | `S3Client` bean |

## Alternatives considered

- **Keep MinIO (quay.io pin or Chainguard image).** Rejected. Upstream is archived, and neither registry offers a pinned image that is both free and pullable without a login. We'd be running unpatched or unpinned storage.
- **RustFS** (Apache 2.0). A close MinIO drop-in with MinIO-style bucket policies, but it's young: reviews in 2026 put its S3 coverage at about 60%, and it has had recent CVEs.
- **Garage** (AGPL-3.0). Light and simple, but it has no bucket policies, only whole-bucket public "website" mode. Keeping deleted images private would need a second bucket and moves between buckets.
- **Ceph RGW.** Complete, but heavy to operate (monitors, OSDs, 4 GB+ RAM). Overkill for one bot's image bucket.

SeaweedFS was chosen because it is the most mature of these (in production since 2012, with frequent releases), it is Apache 2.0 licensed, it supports S3 bucket policies including anonymous `GetObject` on a prefix, and it runs as one container.

## Consequences

- **Deployments need new `.env` keys.** Compose refuses to start without `SEAWEED_ACCESS_KEY` and `SEAWEED_SECRET_KEY`. Without them SeaweedFS would run in its allow-all anonymous mode, so making them required is deliberate.
- **The public URL's reverse proxy must point at the S3 gateway on port 8333** (path-style `/{bucket}/{key}`) instead of MinIO's port 9000.
- **No data migration is needed.** v3 isn't in production yet, so no MinIO data exists to move. Legacy Imgur images are still rehosted by the existing CLI.
- **CI now tests against the same storage server we deploy.** The anonymous-access integration test (200 on `outfits/`, 403 once quarantined) guards the bucket-policy behavior that private deletion depends on.
- **Nothing in the bot depends on MinIO any more,** neither the server, the client nor the class names. The `Flyway V5` migration's comment still mentions `MinioStorageService`; it's left alone because editing an applied migration changes its checksum.
- **Unit tests mock `S3Client` with request objects.** The storage service calls the `S3Client` methods that take built request objects (not the lambda overloads), so tests can capture and check each request.
- **Sources:** [AWS SDK for Java v2](https://github.com/aws/aws-sdk-java-v2), [MinIO removal timeline](https://byteiota.com/minio-docker-hub-quay-anonymous-pull-fix/), [alternatives comparison](https://lowcloud.io/en/blog/minio-alternatives), [SeaweedFS bucket policies](https://github.com/seaweedfs/seaweedfs/wiki/S3-Bucket-Policies), [SeaweedFS `weed mini`](https://github.com/seaweedfs/seaweedfs/wiki/Quick-Start-with-weed-mini).
