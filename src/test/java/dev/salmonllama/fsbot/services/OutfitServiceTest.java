package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.storage.SeaweedStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutfitServiceTest {

    @Mock
    private OutfitRepository outfitRepository;

    @Mock
    private SeaweedStorageService storageService;

    @Mock
    private AuditService auditService;

    private OutfitService outfitService;

    private static final UUID ID = UUID.fromString("3f2b8c1e-9d4a-4e7b-8c2f-1a6d5e9b0c47");
    private static final String ID_TEXT = ID.toString();

    @BeforeEach
    void setUp() {
        outfitService = new OutfitService(outfitRepository, storageService, auditService);
    }

    private OutfitEntity createSampleOutfit(UUID id) {
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
    void testSubmitUploadsUnderGeneratedId() throws Exception {
        OutfitEntity outfit = createSampleOutfit(null);
        when(outfitRepository.save(outfit)).thenAnswer(inv -> {
            outfit.setId(ID);
            return outfit;
        });
        when(storageService.uploadOutfitFromUrl(ID_TEXT, "https://cdn.discordapp.com/pic.png"))
                .thenReturn(new UploadResult("outfits/" + ID + ".png",
                        "https://media.fashionscape.org/outfits/" + ID + ".png", "bucket", 1024, "image/png"));

        OutfitEntity saved = outfitService.submit(outfit, "https://cdn.discordapp.com/pic.png");

        assertEquals(ID, saved.getId());
        assertEquals("https://media.fashionscape.org/outfits/" + ID + ".png", saved.getLink());
    }

    @Test
    void testSubmitPropagatesUploadFailure() throws Exception {
        OutfitEntity outfit = createSampleOutfit(null);
        when(outfitRepository.save(outfit)).thenAnswer(inv -> {
            outfit.setId(ID);
            return outfit;
        });
        when(storageService.uploadOutfitFromUrl(any(), any())).thenThrow(new java.io.IOException("download failed"));

        assertThrows(java.io.IOException.class, () -> outfitService.submit(outfit, "https://cdn.discordapp.com/pic.png"));
    }

    @Test
    void testGetRandomOutfit() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findRandomOutfit()).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfit();
        assertTrue(result.isPresent());
        assertEquals(ID, result.get().getId());
    }

    @Test
    void testGetRandomOutfitByTag() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findRandomOutfitByTag("osrs")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfitByTag("osrs");
        assertTrue(result.isPresent());
        assertEquals(ID, result.get().getId());

        when(outfitRepository.findRandomOutfit()).thenReturn(Optional.of(outfit));
        Optional<OutfitEntity> blankResult = outfitService.getRandomOutfitByTag("");
        assertTrue(blankResult.isPresent());
    }

    @Test
    void testGetRandomOutfitBySubmitter() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findRandomOutfitBySubmitter("user-1")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfitBySubmitter("user-1");
        assertTrue(result.isPresent());
        assertEquals(ID, result.get().getId());
    }

    @Test
    void testGetRandomOutfitWithBothTagAndSubmitter() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findRandomOutfitByTagAndSubmitter("osrs", "user-1")).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.getRandomOutfit("osrs", "user-1");
        assertTrue(result.isPresent());
        assertEquals(ID, result.get().getId());
    }

    @Test
    void testFindById() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findById(ID)).thenReturn(Optional.of(outfit));

        Optional<OutfitEntity> result = outfitService.findById(ID_TEXT);
        assertTrue(result.isPresent());
        assertEquals(ID, result.get().getId());

        assertTrue(outfitService.findById(null).isEmpty());
        assertTrue(outfitService.findById("   ").isEmpty());
        assertTrue(outfitService.findById("abcd1234").isEmpty());
    }

    @Test
    void testSearch() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<OutfitEntity> emptyPage = new PageImpl<>(List.of(createSampleOutfit(ID)));

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
    void testToggleVoteAdds() {
        when(outfitRepository.existsByIdAndDeletedFalse(ID)).thenReturn(true);
        when(outfitRepository.insertVote(ID, "voter")).thenReturn(1);
        when(outfitRepository.incrementVoteCount(ID)).thenReturn(1);
        when(outfitRepository.findVoteCount(ID)).thenReturn(Optional.of(6));

        Optional<OutfitService.VoteResult> result = outfitService.toggleVote(ID_TEXT, "voter");

        assertEquals(Optional.of(new OutfitService.VoteResult(true, 6)), result);
        verify(outfitRepository, never()).deleteVote(any(), any());
        verify(outfitRepository, never()).save(any());
    }

    @Test
    void testToggleVoteRemovesExistingVote() {
        when(outfitRepository.existsByIdAndDeletedFalse(ID)).thenReturn(true);
        when(outfitRepository.insertVote(ID, "voter")).thenReturn(0);
        when(outfitRepository.deleteVote(ID, "voter")).thenReturn(1);
        when(outfitRepository.findVoteCount(ID)).thenReturn(Optional.of(4));

        Optional<OutfitService.VoteResult> result = outfitService.toggleVote(ID_TEXT, "voter");

        assertEquals(Optional.of(new OutfitService.VoteResult(false, 4)), result);
        verify(outfitRepository).decrementVoteCount(ID);
        verify(outfitRepository, never()).incrementVoteCount(any());
    }

    @Test
    void testToggleVoteOnDeletedOutfit() {
        when(outfitRepository.existsByIdAndDeletedFalse(ID)).thenReturn(false);

        assertTrue(outfitService.toggleVote(ID_TEXT, "voter").isEmpty());
        verify(outfitRepository, never()).insertVote(any(), any());
        verify(outfitRepository, never()).incrementVoteCount(any());
    }

    @Test
    void testToggleVoteIgnoresNonUuidId() {
        assertTrue(outfitService.toggleVote("abcd1234", "voter").isEmpty());
        verifyNoInteractions(outfitRepository);
    }

    @Test
    void testToggleVoteOutfitDeletedMidVote() {
        when(outfitRepository.existsByIdAndDeletedFalse(ID)).thenReturn(true);
        when(outfitRepository.insertVote(ID, "voter")).thenReturn(1);
        when(outfitRepository.incrementVoteCount(ID)).thenReturn(0);

        assertTrue(outfitService.toggleVote(ID_TEXT, "voter").isEmpty());
        verify(outfitRepository).deleteVote(ID, "voter");
    }

    @Test
    void testRecordShown() {
        outfitService.recordShown(ID);
        verify(outfitRepository).incrementDisplayCount(ID);
    }

    private void stubImageKey(OutfitEntity outfit) {
        String key = "outfits/" + outfit.getId() + ".png";
        when(storageService.extractObjectKey(outfit.getLink())).thenReturn(Optional.of(key));
    }

    @Test
    void testDeleteQuarantinesImageThenMarksDeletedAndAudits() throws Exception {
        OutfitEntity outfit = createSampleOutfit(ID);
        stubImageKey(outfit);
        when(outfitRepository.findByIdAndDeletedFalse(ID)).thenReturn(Optional.of(outfit));

        assertTrue(outfitService.delete(ID_TEXT, "user-1", "server-1", null));

        var order = inOrder(storageService, outfitRepository, auditService);
        order.verify(storageService).quarantine("outfits/" + ID + ".png");
        order.verify(outfitRepository).save(outfit);
        order.verify(auditService).record(isNull(), eq(AuditService.Action.OUTFIT_DELETED), eq("user-1"), eq("server-1"),
                eq(AuditService.TARGET_OUTFIT), eq(ID_TEXT), eq("Deleted by its submitter"));
        assertTrue(outfit.isDeleted());
        assertEquals("user-1", outfit.getDeletedBy());
        assertNotNull(outfit.getDeletedAt());
    }

    @Test
    void testDeleteByStaffNamesSubmitterInAudit() {
        OutfitEntity outfit = createSampleOutfit(ID);
        stubImageKey(outfit);
        when(outfitRepository.findByIdAndDeletedFalse(ID)).thenReturn(Optional.of(outfit));

        outfitService.delete(ID_TEXT, "staff-1", "server-1", null);

        verify(auditService).record(any(), eq(AuditService.Action.OUTFIT_DELETED), eq("staff-1"), any(),
                any(), eq(ID_TEXT), eq("Deleted by staff (submitter <@user-1>)"));
    }

    @Test
    void testDeleteChangesNothingWhenImageMoveFails() throws Exception {
        OutfitEntity outfit = createSampleOutfit(ID);
        stubImageKey(outfit);
        when(outfitRepository.findByIdAndDeletedFalse(ID)).thenReturn(Optional.of(outfit));
        doThrow(new RuntimeException("SeaweedFS down")).when(storageService).quarantine("outfits/" + ID + ".png");

        assertThrows(OutfitService.OutfitImageException.class,
                () -> outfitService.delete(ID_TEXT, "user-1", "server-1", null));

        assertFalse(outfit.isDeleted());
        assertNull(outfit.getDeletedBy());
        verify(outfitRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void testDeleteWithExternalImageSkipsStorage() throws Exception {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(storageService.extractObjectKey(outfit.getLink())).thenReturn(Optional.empty());
        when(outfitRepository.findByIdAndDeletedFalse(ID)).thenReturn(Optional.of(outfit));

        assertTrue(outfitService.delete(ID_TEXT, "user-1", null, null));

        verify(storageService, never()).quarantine(any());
        assertTrue(outfit.isDeleted());
    }

    @Test
    void testDeleteMissingOrAlreadyDeleted() {
        when(outfitRepository.findByIdAndDeletedFalse(ID)).thenReturn(Optional.empty());

        assertFalse(outfitService.delete(ID_TEXT, "user-1", null, null));
        verifyNoInteractions(storageService, auditService);
    }

    @Test
    void testRestoreReleasesImageAndClearsDeletion() throws Exception {
        OutfitEntity outfit = createSampleOutfit(ID);
        outfit.setDeleted(true);
        outfit.setDeletedBy("user-1");
        outfit.setDeletedAt(Instant.now());
        stubImageKey(outfit);
        when(outfitRepository.findById(ID)).thenReturn(Optional.of(outfit));

        assertTrue(outfitService.restore(ID_TEXT, "staff-1", "home", null));

        var order = inOrder(storageService, outfitRepository, auditService);
        order.verify(storageService).release("outfits/" + ID + ".png");
        order.verify(outfitRepository).save(outfit);
        order.verify(auditService).record(isNull(), eq(AuditService.Action.OUTFIT_RESTORED), eq("staff-1"), eq("home"),
                eq(AuditService.TARGET_OUTFIT), eq(ID_TEXT), isNull());
        assertFalse(outfit.isDeleted());
        assertNull(outfit.getDeletedBy());
        assertNull(outfit.getDeletedAt());
    }

    @Test
    void testRestoreActiveOutfitDoesNothing() {
        OutfitEntity outfit = createSampleOutfit(ID);
        when(outfitRepository.findById(ID)).thenReturn(Optional.of(outfit));

        assertFalse(outfitService.restore(ID_TEXT, "staff-1", null, null));
        verifyNoInteractions(storageService, auditService);
    }

    @Test
    void testGetDeletedImageReadsQuarantinedObject() throws Exception {
        OutfitEntity outfit = createSampleOutfit(ID);
        stubImageKey(outfit);
        when(storageService.getObjectBytes("deleted/outfits/" + ID + ".png")).thenReturn(new byte[]{1, 2, 3});

        Optional<OutfitService.StoredImage> image = outfitService.getDeletedImage(outfit);

        assertTrue(image.isPresent());
        assertEquals(ID + ".png", image.get().fileName());
        assertArrayEquals(new byte[]{1, 2, 3}, image.get().data());
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
