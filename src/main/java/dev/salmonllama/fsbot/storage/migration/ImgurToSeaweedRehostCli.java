package dev.salmonllama.fsbot.storage.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ImgurToSeaweedRehostCli implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ImgurToSeaweedRehostCli.class);

    private static final String REHOST_FLAG = "--rehost-imgur";
    private static final String DRY_RUN_FLAG = "--dry-run";
    private static final String LIMIT_PREFIX = "--rehost-limit=";
    private static final String DELAY_PREFIX = "--rehost-delay=";

    private final ImgurToSeaweedRehostService rehostService;

    public ImgurToSeaweedRehostCli(ImgurToSeaweedRehostService rehostService) {
        this.rehostService = rehostService;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean shouldRun = false;
        boolean dryRun = false;
        int limit = 0;
        long delayMillis = 100; // default 100ms throttle between Imgur requests

        // Check command line arguments
        for (String arg : args) {
            if (arg.startsWith(REHOST_FLAG)) {
                shouldRun = true;
                if (arg.equalsIgnoreCase("--rehost-imgur=dry-run")) {
                    dryRun = true;
                }
            } else if (arg.equalsIgnoreCase(DRY_RUN_FLAG)) {
                dryRun = true;
            } else if (arg.startsWith(LIMIT_PREFIX)) {
                try {
                    limit = Integer.parseInt(arg.substring(LIMIT_PREFIX.length()).trim());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid --rehost-limit argument: {}", arg);
                }
            } else if (arg.startsWith(DELAY_PREFIX)) {
                try {
                    delayMillis = Long.parseLong(arg.substring(DELAY_PREFIX.length()).trim());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid --rehost-delay argument: {}", arg);
                }
            }
        }

        // Also check environment variable trigger
        String envTrigger = System.getenv("FSBOT_REHOST_IMGUR");
        if (envTrigger != null && !envTrigger.isBlank()) {
            shouldRun = true;
            if ("dry-run".equalsIgnoreCase(envTrigger.trim())) {
                dryRun = true;
            }
        }

        if (shouldRun) {
            logger.info("=================================================");
            logger.info("Executing Imgur-to-SeaweedFS Image Rehost CLI");
            logger.info("Mode: {}", dryRun ? "DRY RUN" : "LIVE MIGRATION");
            if (limit > 0) {
                logger.info("Limit: {} outfits", limit);
            }
            logger.info("Delay: {} ms", delayMillis);
            logger.info("=================================================");

            ImgurToSeaweedRehostService.RehostReport report = rehostService.rehostAll(dryRun, limit, delayMillis);

            logger.info("=================================================");
            logger.info("Migration Summary Report:");
            logger.info("  Total Found: {}", report.totalFound());
            logger.info("  Processed:   {}", report.processed());
            logger.info("  Succeeded:   {}", report.succeeded());
            logger.info("  Failed:      {}", report.failed());
            logger.info("  Skipped:     {}", report.skipped());
            if (!report.failures().isEmpty()) {
                logger.warn("Encountered {} failure(s):", report.failures().size());
                for (ImgurToSeaweedRehostService.RehostReport.RehostFailure failure : report.failures()) {
                    logger.warn("  - Outfit {}: {} (Reason: {})", failure.outfitId(), failure.sourceUrl(), failure.reason());
                }
            }
            logger.info("=================================================");
        }
    }
}
