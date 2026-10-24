package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutfitServiceTest {

    @Mock
    private OutfitRepository outfitRepository;

    private OutfitService outfitService;

    @BeforeEach
    void setUp() {
        outfitService = new OutfitService(outfitRepository);
    }

    private OutfitEntity createSampleOutfit(String id) {
        return new OutfitEntity(
                id,
                "https://media.fashionscape.org/outfits/" + id + ".png",
                "user-1",
                "osrs",
                "meta info",
                "DiscordUser",
                Instant.now(),
                Instant.now(),
                false,
                false,
                5,
                null,
                "server-1",
                "channel-1"
        );
    }

    @Test
    void testGetRandomOutfit() {
        OutfitEntity outfit = createSampleOutfit("outfit-1");
        when(outfitRepository.findRandomOutfit()).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfit();
        assertTrue(result.isPresent());
        assertEquals("outfit-1", result.get().getId());
    }

    @Test
    void testGetRandomOutfitByTag() {
        OutfitEntity outfit = createSampleOutfit("outfit-tag");
        when(outfitRepository.findRandomOutfitByTag("osrs")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfitByTag("osrs");
        assertTrue(result.isPresent());
        assertEquals("outfit-tag", result.get().getId());

        when(outfitRepository.findRandomOutfit()).thenReturn(Optional.of(outfit));
        Optional<OutfitEntity> blankResult = outfitService.getRandomOutfitByTag("");
        assertTrue(blankResult.isPresent());
    }

    @Test
    void testGetRandomOutfitBySubmitter() {
        OutfitEntity outfit = createSampleOutfit("outfit-user");
        when(outfitRepository.findRandomOutfitBySubmitter("user-1")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfitBySubmitter("user-1");
        assertTrue(result.isPresent());
        assertEquals("outfit-user", result.get().getId());
    }

    @Test
    void testGetRandomOutfitWithBothTagAndSubmitter() {
        OutfitEntity outfit = createSampleOutfit("outfit-both");
        when(outfitRepository.findRandomOutfitByTagAndSubmitter("osrs", "user-1")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfit("osrs", "user-1");
        assertTrue(result.isPresent());
        assertEquals("outfit-both", result.get().getId());
    }

    @Test
    void testFindById() {
        OutfitEntity outfit = createSampleOutfit("outfit-id");
        when(outfitRepository.findById("outfit-id")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.findById("outfit-id");
        assertTrue(result.isPresent());
        assertEquals("outfit-id", result.get().getId());

        assertTrue(outfitService.findById(null).isEmpty());
        assertTrue(outfitService.findById("   ").isEmpty());
    }

    @Test
    void testSearch() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<OutfitEntity> emptyPage = new PageImpl<>(List.of(createSampleOutfit("o1")));

        when(outfitRepository.findByDeletedFalse(pageRequest)).thenReturn(emptyPage);
        Page<OutfitEntity> all = outfitService.search(null, null, pageRequest);
        assertEquals(1, all.getTotalElements());

        when(outfitRepository.findByTagIgnoreCaseAndDeletedFalse("osrs", pageRequest)).thenReturn(emptyPage);
        Page<OutfitEntity> byTag = outfitService.search("osrs", null, pageRequest);
        assertEquals(1, byTag.getTotalElements());

        when(outfitRepository.findBySubmitterAndDeletedFalse("user-1", pageRequest)).thenReturn(emptyPage);
        Page<OutfitEntity> byUser = outfitService.search(null, "user-1", pageRequest);
        assertEquals(1, byUser.getTotalElements());

        when(outfitRepository.findByTagIgnoreCaseAndSubmitterAndDeletedFalse("osrs", "user-1", pageRequest)).thenReturn(emptyPage);
        Page<OutfitEntity> byBoth = outfitService.search("osrs", "user-1", pageRequest);
        assertEquals(1, byBoth.getTotalElements());
    }

    @Test
    void testUpvote() {
        OutfitEntity outfit = createSampleOutfit("upvote-me");
        outfit.setDisplayCount(10);
        when(outfitRepository.findById("upvote-me")).thenReturn(Optional.of(outfit));
        when(outfitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<OutfitEntity> updated = outfitService.upvote("upvote-me");
        assertTrue(updated.isPresent());
        assertEquals(11, updated.get().getDisplayCount());
    }

    @Test
    void testDeleteAndRestore() {
        OutfitEntity outfit = createSampleOutfit("del-me");
        outfit.setDeleted(false);
        when(outfitRepository.findById("del-me")).thenReturn(Optional.of(outfit));
        when(outfitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean deleted = outfitService.delete("del-me");
        assertTrue(deleted);
        assertTrue(outfit.isDeleted());

        boolean restored = outfitService.restore("del-me");
        assertTrue(restored);
        assertFalse(outfit.isDeleted());

        assertFalse(outfitService.delete("non-existent"));
        assertFalse(outfitService.restore("non-existent"));
    }

    @Test
    void testDistinctTagsAndCount() {
        when(outfitRepository.findDistinctTags()).thenReturn(List.of("fashionscape", "osrs", "rs3"));
        when(outfitRepository.countByDeletedFalse()).thenReturn(42L);

        List<String> tags = outfitService.getDistinctTags();
        assertEquals(3, tags.size());
        assertEquals("fashionscape", tags.get(0));

        assertEquals(42L, outfitService.countActive());
    }
}
