/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage.migration;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImgurToMinioRehostService {

    private static final Logger logger = LoggerFactory.getLogger(ImgurToMinioRehostService.class);

    private final OutfitRepository outfitRepository;
    private final MinioStorageService minioStorageService;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public ImgurToMinioRehostService(OutfitRepository outfitRepository, MinioStorageService minioStorageService) {
        this(outfitRepository, minioStorageService, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public ImgurToMinioRehostService(
            OutfitRepository outfitRepository,
            MinioStorageService minioStorageService,
            HttpClient httpClient
    ) {
        this.outfitRepository = outfitRepository;
        this.minioStorageService = minioStorageService;
        this.httpClient = httpClient;
    }

    public RehostReport rehostAll(boolean dryRun, int limit, long delayMillis) {
        logger.info("Starting Imgur to MinIO rehosting (dryRun={}, limit={}, delayMillis={})", dryRun, limit, delayMillis);

        List<OutfitEntity> outfits = outfitRepository.findByLinkContaining("imgur.com");
        int totalFound = outfits.size();
        logger.info("Discovered {} outfit(s) with Imgur URLs in database", totalFound);

        if (limit > 0 && outfits.size() > limit) {
            outfits = outfits.subList(0, limit);
            logger.info("Limiting migration to first {} outfit(s)", outfits.size());
        }

        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        int processed = 0;
        List<RehostReport.RehostFailure> failures = new ArrayList<>();

        for (int i = 0; i < outfits.size(); i++) {
            OutfitEntity outfit = outfits.get(i);
            processed++;
            String sourceUrl = outfit.getLink();
            String outfitId = outfit.getId();
            int currentNumber = i + 1;

            if (sourceUrl == null || !sourceUrl.contains("imgur.com")) {
                skipped++;
                continue;
            }

            logger.info("[{}/{}] Processing outfit ID: {} (URL: {})", currentNumber, outfits.size(), outfitId, sourceUrl);

            if (dryRun) {
                logger.info("[{}/{}] [DRY RUN] Would download and rehost outfit ID: {} from {}", currentNumber, outfits.size(), outfitId, sourceUrl);
                succeeded++;
                continue;
            }

            try {
                // Download asset from Imgur
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(sourceUrl))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "FashionScape-Bot/3.0 (Asset-Migration)")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();

                if (statusCode < 200 || statusCode >= 300) {
                    String reason = "HTTP " + statusCode + " while downloading asset";
                    logger.warn("[{}/{}] Failed to download outfit ID {}: {}", currentNumber, outfits.size(), outfitId, reason);
                    failures.add(new RehostReport.RehostFailure(outfitId, sourceUrl, reason));
                    failed++;
                    continue;
                }

                byte[] imageBytes = response.body();
                if (imageBytes == null || imageBytes.length == 0) {
                    String reason = "Received empty response payload";
                    logger.warn("[{}/{}] Outfit ID {} returned 0 bytes from {}", currentNumber, outfits.size(), outfitId, sourceUrl);
                    failures.add(new RehostReport.RehostFailure(outfitId, sourceUrl, reason));
                    failed++;
                    continue;
                }

                String contentType = response.headers().firstValue("Content-Type").orElse("image/png");
                String ext = minioStorageService.resolveExtension(contentType, sourceUrl);
                String objectName = minioStorageService.buildOutfitObjectName(outfitId, ext);

                UploadResult uploadResult = minioStorageService.upload(objectName, imageBytes, contentType);
                String newPublicUrl = uploadResult.publicUrl();

                // Update outfit in database
                updateOutfitLink(outfit, newPublicUrl);

                succeeded++;
                logger.info("[{}/{}] Successfully rehosted outfit ID: {} -> {}", currentNumber, outfits.size(), outfitId, newPublicUrl);

                if (delayMillis > 0 && currentNumber < outfits.size()) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Rehost loop sleep interrupted");
                        break;
                    }
                }
            } catch (Exception e) {
                String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
                logger.error("[{}/{}] Error during rehosting for outfit ID {}: {}", currentNumber, outfits.size(), outfitId, errorMsg, e);
                failures.add(new RehostReport.RehostFailure(outfitId, sourceUrl, errorMsg));
                failed++;
            }
        }

        RehostReport report = new RehostReport(totalFound, processed, succeeded, failed, skipped, dryRun, failures);
        logger.info("Imgur to MinIO rehost migration finished: {}", report);
        return report;
    }

    @Transactional
    public void updateOutfitLink(OutfitEntity outfit, String newUrl) {
        outfit.setLink(newUrl);
        outfit.setUpdated(Instant.now());
        outfitRepository.save(outfit);
    }

    public record RehostReport(
            int totalFound,
            int processed,
            int succeeded,
            int failed,
            int skipped,
            boolean dryRun,
            List<RehostFailure> failures
    ) {
        public record RehostFailure(
                String outfitId,
                String sourceUrl,
                String reason
        ) {}

        @Override
        public String toString() {
            return String.format(
                    "RehostReport[totalFound=%d, processed=%d, succeeded=%d, failed=%d, skipped=%d, dryRun=%b, failureCount=%d]",
                    totalFound, processed, succeeded, failed, skipped, dryRun, failures.size()
            );
        }
    }
}
