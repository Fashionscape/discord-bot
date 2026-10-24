package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitRepository extends JpaRepository<OutfitEntity, String>, JpaSpecificationExecutor<OutfitEntity> {

    List<OutfitEntity> findByDeletedFalse();

    Optional<OutfitEntity> findByIdAndDeletedFalse(String id);

    List<OutfitEntity> findBySubmitterAndDeletedFalse(String submitter);

    List<OutfitEntity> findByTagIgnoreCaseAndDeletedFalse(String tag);

    Page<OutfitEntity> findByDeletedFalse(Pageable pageable);

    Page<OutfitEntity> findByTagIgnoreCaseAndDeletedFalse(String tag, Pageable pageable);

    Page<OutfitEntity> findBySubmitterAndDeletedFalse(String submitter, Pageable pageable);

    Page<OutfitEntity> findByTagIgnoreCaseAndSubmitterAndDeletedFalse(String tag, String submitter, Pageable pageable);

    long countByDeletedFalse();

    long countByTagIgnoreCaseAndDeletedFalse(String tag);

    long countBySubmitterAndDeletedFalse(String submitter);

    @Query(value = "SELECT * FROM outfits WHERE deleted = false ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<OutfitEntity> findRandomOutfit();

    @Query(value = "SELECT * FROM outfits WHERE deleted = false AND LOWER(tag) = LOWER(:tag) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<OutfitEntity> findRandomOutfitByTag(@Param("tag") String tag);

    @Query(value = "SELECT * FROM outfits WHERE deleted = false AND submitter = :submitter ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<OutfitEntity> findRandomOutfitBySubmitter(@Param("submitter") String submitter);

    @Query(value = "SELECT * FROM outfits WHERE deleted = false AND LOWER(tag) = LOWER(:tag) AND submitter = :submitter ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<OutfitEntity> findRandomOutfitByTagAndSubmitter(@Param("tag") String tag, @Param("submitter") String submitter);

    @Query("SELECT DISTINCT o.tag FROM OutfitEntity o WHERE o.deleted = false AND o.tag IS NOT NULL ORDER BY o.tag ASC")
    List<String> findDistinctTags();

    List<OutfitEntity> findByLinkContaining(String term);

    boolean existsByIdAndDeletedFalse(String id);

    // Votes: each statement is a single atomic SQL update. Call them through OutfitService.toggleVote.

    @Modifying
    @Query(value = "INSERT INTO outfit_votes (outfit_id, user_id, created) VALUES (:outfitId, :userId, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertVote(@Param("outfitId") String outfitId, @Param("userId") String userId);

    @Modifying
    @Query(value = "DELETE FROM outfit_votes WHERE outfit_id = :outfitId AND user_id = :userId", nativeQuery = true)
    int deleteVote(@Param("outfitId") String outfitId, @Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE outfits SET vote_count = vote_count + 1 WHERE id = :id AND deleted = false", nativeQuery = true)
    int incrementVoteCount(@Param("id") String id);

    @Modifying
    @Query(value = "UPDATE outfits SET vote_count = vote_count - 1 WHERE id = :id AND deleted = false AND vote_count > 0", nativeQuery = true)
    int decrementVoteCount(@Param("id") String id);

    @Query(value = "SELECT vote_count FROM outfits WHERE id = :id", nativeQuery = true)
    Optional<Integer> findVoteCount(@Param("id") String id);

    @Modifying
    @Query(value = "UPDATE outfits SET display_count = display_count + 1 WHERE id = :id", nativeQuery = true)
    int incrementDisplayCount(@Param("id") String id);
}
