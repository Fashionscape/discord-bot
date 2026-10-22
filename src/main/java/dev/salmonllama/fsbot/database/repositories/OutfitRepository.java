package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
