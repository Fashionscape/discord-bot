package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.storage.SeaweedStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class OutfitService {

    private static final Logger logger = LoggerFactory.getLogger(OutfitService.class);

    private final OutfitRepository outfitRepository;
    private final SeaweedStorageService storageService;
    private final AuditService auditService;

    public OutfitService(OutfitRepository outfitRepository, SeaweedStorageService storageService, AuditService auditService) {
        this.outfitRepository = outfitRepository;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    public Optional<OutfitEntity> getRandomOutfit() {
        return outfitRepository.findRandomOutfit();
    }

    public Optional<OutfitEntity> getRandomOutfitByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return getRandomOutfit();
        }
        return outfitRepository.findRandomOutfitByTag(tag.trim());
    }

    public Optional<OutfitEntity> getRandomOutfitBySubmitter(String submitter) {
        if (submitter == null || submitter.isBlank()) {
            return getRandomOutfit();
        }
        return outfitRepository.findRandomOutfitBySubmitter(submitter.trim());
    }

    public Optional<OutfitEntity> getRandomOutfit(String tag, String submitter) {
        boolean hasTag = tag != null && !tag.isBlank();
        boolean hasSubmitter = submitter != null && !submitter.isBlank();

        if (hasTag && hasSubmitter) {
            return outfitRepository.findRandomOutfitByTagAndSubmitter(tag.trim(), submitter.trim());
        } else if (hasTag) {
            return getRandomOutfitByTag(tag);
        } else if (hasSubmitter) {
            return getRandomOutfitBySubmitter(submitter);
        } else {
            return getRandomOutfit();
        }
    }

    public Optional<OutfitEntity> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return outfitRepository.findById(id.trim());
    }

    public Optional<OutfitEntity> findActiveById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return outfitRepository.findByIdAndDeletedFalse(id.trim());
    }

    public Page<OutfitEntity> search(String tag, String submitter, Pageable pageable) {
        boolean hasTag = tag != null && !tag.isBlank();
        boolean hasSubmitter = submitter != null && !submitter.isBlank();

        if (hasTag && hasSubmitter) {
            return outfitRepository.findByTagIgnoreCaseAndSubmitterAndDeletedFalse(tag.trim(), submitter.trim(), pageable);
        } else if (hasTag) {
            return outfitRepository.findByTagIgnoreCaseAndDeletedFalse(tag.trim(), pageable);
        } else if (hasSubmitter) {
            return outfitRepository.findBySubmitterAndDeletedFalse(submitter.trim(), pageable);
        } else {
            return outfitRepository.findByDeletedFalse(pageable);
        }
    }

    @Transactional
    public OutfitEntity save(OutfitEntity outfit) {
        return outfitRepository.save(outfit);
    }

    /**
     * Saves a new gallery outfit and uploads its image under the outfit's generated ID. Saving
     * assigns the UUID, and the insert waits for the commit, so a failed upload writes no row.
     */
    @Transactional(rollbackFor = Exception.class)
    public OutfitEntity submit(OutfitEntity outfit, String sourceUrl) throws Exception {
        OutfitEntity saved = outfitRepository.save(outfit);
        UploadResult upload = storageService.uploadOutfitFromUrl(saved.getId(), sourceUrl);
        saved.setLink(upload.publicUrl());
        return saved;
    }

    /**
     * Adds the user's vote, or removes it if they already voted. One vote per user per outfit;
     * the vote row and the count change together in one transaction.
     *
     * @return the outcome, or empty if the outfit doesn't exist or is deleted
     */
    @Transactional
    public Optional<VoteResult> toggleVote(String outfitId, String userId) {
        if (!outfitRepository.existsByIdAndDeletedFalse(outfitId)) {
            return Optional.empty();
        }

        boolean added;
        if (outfitRepository.insertVote(outfitId, userId) == 1) {
            if (outfitRepository.incrementVoteCount(outfitId) == 0) {
                // Deleted after the check above: take the vote back out
                outfitRepository.deleteVote(outfitId, userId);
                return Optional.empty();
            }
            added = true;
        } else {
            if (outfitRepository.deleteVote(outfitId, userId) == 1) {
                outfitRepository.decrementVoteCount(outfitId);
            }
            added = false;
        }

        int count = outfitRepository.findVoteCount(outfitId).orElse(0);
        logger.debug("Vote {} on outfit {} by {}: count {}", added ? "added" : "removed", outfitId, userId, count);
        return Optional.of(new VoteResult(added, count));
    }

    public record VoteResult(boolean added, int voteCount) {
    }

    /**
     * Counts one more time the outfit was shown by /outfit random.
     */
    @Transactional
    public void recordShown(String id) {
        outfitRepository.incrementDisplayCount(id);
    }

    /**
     * Deletes an outfit: moves its image out of public read, then marks the outfit deleted and
     * writes the audit row in one transaction. If the image can't be moved, nothing is marked or
     * audited. If the transaction rolls back after the move, the image is moved back.
     *
     * @return false if the outfit doesn't exist or is already deleted
     * @throws OutfitImageException if the image could not be moved
     */
    @Transactional
    public boolean delete(String id, String actorId, String guildId, JDA jda) {
        Optional<OutfitEntity> found = outfitRepository.findByIdAndDeletedFalse(id);
        if (found.isEmpty()) {
            return false;
        }
        OutfitEntity outfit = found.get();

        publicImageKey(outfit).ifPresent(key -> {
            moveImage(key, true);
            onRollback(() -> moveImage(key, false));
        });

        outfit.setDeleted(true);
        outfit.setDeletedAt(Instant.now());
        outfit.setDeletedBy(actorId);
        outfit.setUpdated(Instant.now());
        outfitRepository.save(outfit);

        auditService.record(jda, AuditService.Action.OUTFIT_DELETED, actorId, guildId, AuditService.TARGET_OUTFIT, id,
                actorId.equals(outfit.getSubmitter())
                        ? "Deleted by its submitter"
                        : "Deleted by staff (submitter <@" + outfit.getSubmitter() + ">)");
        logger.info("Deleted outfit {} (by {})", id, actorId);
        return true;
    }

    /**
     * Restores a deleted outfit and makes its image public again, the reverse of {@link #delete}.
     *
     * @return false if the outfit doesn't exist or isn't deleted
     * @throws OutfitImageException if the image could not be moved back
     */
    @Transactional
    public boolean restore(String id, String actorId, String guildId, JDA jda) {
        Optional<OutfitEntity> found = outfitRepository.findById(id).filter(OutfitEntity::isDeleted);
        if (found.isEmpty()) {
            return false;
        }
        OutfitEntity outfit = found.get();

        publicImageKey(outfit).ifPresent(key -> {
            moveImage(key, false);
            onRollback(() -> moveImage(key, true));
        });

        outfit.setDeleted(false);
        outfit.setDeletedAt(null);
        outfit.setDeletedBy(null);
        outfit.setUpdated(Instant.now());
        outfitRepository.save(outfit);

        auditService.record(jda, AuditService.Action.OUTFIT_RESTORED, actorId, guildId, AuditService.TARGET_OUTFIT, id, null);
        logger.info("Restored outfit {} (by {})", id, actorId);
        return true;
    }

    /**
     * Reads a deleted outfit's quarantined image, for staff to view privately.
     *
     * @return empty if the outfit's image isn't stored in our bucket or can't be read
     */
    public Optional<StoredImage> getDeletedImage(OutfitEntity outfit) {
        return publicImageKey(outfit).flatMap(key -> {
            try {
                byte[] data = storageService.getObjectBytes(SeaweedStorageService.QUARANTINE_PREFIX + key);
                return Optional.of(new StoredImage(key.substring(key.lastIndexOf('/') + 1), data));
            } catch (Exception e) {
                logger.warn("Could not read quarantined image for outfit {}", outfit.getId(), e);
                return Optional.empty();
            }
        });
    }

    public record StoredImage(String fileName, byte[] data) {
    }

    /**
     * The outfit's image key in our bucket, if its link points at one. Links to other hosts
     * (from before v3) have nothing for the bot to move.
     */
    private Optional<String> publicImageKey(OutfitEntity outfit) {
        return storageService.extractObjectKey(outfit.getLink())
                .filter(key -> key.startsWith(SeaweedStorageService.PUBLIC_PREFIX));
    }

    private void moveImage(String publicKey, boolean toQuarantine) {
        try {
            if (toQuarantine) {
                storageService.quarantine(publicKey);
            } else {
                storageService.release(publicKey);
            }
        } catch (Exception e) {
            throw new OutfitImageException("Could not " + (toQuarantine ? "quarantine" : "release") + " image " + publicKey, e);
        }
    }

    private void onRollback(Runnable undo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        undo.run();
                    } catch (Exception e) {
                        logger.error("Could not undo image move after rollback", e);
                    }
                }
            }
        });
    }

    /**
     * The outfit's image could not be moved, so the outfit was left unchanged.
     */
    public static class OutfitImageException extends RuntimeException {
        public OutfitImageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public List<String> getDistinctTags() {
        return outfitRepository.findDistinctTags();
    }

    public long countActive() {
        return outfitRepository.countByDeletedFalse();
    }
}
