package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class OutfitService {

    private static final Logger logger = LoggerFactory.getLogger(OutfitService.class);

    private final OutfitRepository outfitRepository;

    public OutfitService(OutfitRepository outfitRepository) {
        this.outfitRepository = outfitRepository;
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

    @Transactional
    public boolean delete(String id) {
        return outfitRepository.findById(id).map(outfit -> {
            outfit.setDeleted(true);
            outfit.setUpdated(Instant.now());
            outfitRepository.save(outfit);
            logger.info("Soft-deleted outfit {}", id);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean restore(String id) {
        return outfitRepository.findById(id).map(outfit -> {
            outfit.setDeleted(false);
            outfit.setUpdated(Instant.now());
            outfitRepository.save(outfit);
            logger.info("Restored outfit {}", id);
            return true;
        }).orElse(false);
    }

    public List<String> getDistinctTags() {
        return outfitRepository.findDistinctTags();
    }

    public long countActive() {
        return outfitRepository.countByDeletedFalse();
    }
}
