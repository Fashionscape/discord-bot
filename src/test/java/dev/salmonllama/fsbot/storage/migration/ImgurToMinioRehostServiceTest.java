/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage.migration;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImgurToMinioRehostServiceTest {

    @Mock
    private OutfitRepository outfitRepository;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<byte[]> httpResponse;

    private ImgurToMinioRehostService rehostService;

    @BeforeEach
    void setUp() {
        rehostService = new ImgurToMinioRehostService(outfitRepository, minioStorageService, httpClient);
    }

    @Test
    void rehostAllDryRunDoesNotUploadOrUpdateDatabase() {
        OutfitEntity outfit1 = new OutfitEntity("id-1", "https://i.imgur.com/abc.png", "user1", "tag", null, "User", Instant.now(), Instant.now(), false, false, 0, null);
        OutfitEntity outfit2 = new OutfitEntity("id-2", "https://i.imgur.com/xyz.jpg", "user2", "tag", null, "User", Instant.now(), Instant.now(), false, false, 0, null);

        when(outfitRepository.findByLinkContaining("imgur.com")).thenReturn(List.of(outfit1, outfit2));

        ImgurToMinioRehostService.RehostReport report = rehostService.rehostAll(true, 0, 0);

        assertEquals(2, report.totalFound());
        assertEquals(2, report.processed());
        assertEquals(2, report.succeeded());
        assertEquals(0, report.failed());
        assertTrue(report.dryRun());
        verify(outfitRepository, never()).save(any(OutfitEntity.class));
        verifyNoInteractions(minioStorageService);
    }

    @Test
    void rehostAllLiveSuccessfullyRehostsOutfits() throws Exception {
        OutfitEntity outfit = new OutfitEntity("id-100", "https://i.imgur.com/test.png", "user1", "tag", null, "User", Instant.now(), Instant.now(), false, false, 0, null);
        when(outfitRepository.findByLinkContaining("imgur.com")).thenReturn(List.of(outfit));

        byte[] fakeData = new byte[]{10, 20, 30};
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(fakeData);
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("image/png")), (k, v) -> true);
        when(httpResponse.headers()).thenReturn(headers);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        when(minioStorageService.resolveExtension("image/png", "https://i.imgur.com/test.png")).thenReturn("png");
        when(minioStorageService.buildOutfitObjectName("id-100", "png")).thenReturn("outfits/id-100.png");

        UploadResult uploadResult = new UploadResult("outfits/id-100.png", "https://media.fashionscape.org/outfits/id-100.png", "fashionscape-outfits", fakeData.length, "image/png");
        when(minioStorageService.upload(eq("outfits/id-100.png"), eq(fakeData), eq("image/png"))).thenReturn(uploadResult);

        ImgurToMinioRehostService.RehostReport report = rehostService.rehostAll(false, 0, 0);

        assertEquals(1, report.totalFound());
        assertEquals(1, report.succeeded());
        assertEquals(0, report.failed());
        assertEquals("https://media.fashionscape.org/outfits/id-100.png", outfit.getLink());
        verify(outfitRepository).save(outfit);
    }

    @Test
    void rehostAllHandlesHttp404Gracefully() throws Exception {
        OutfitEntity outfit = new OutfitEntity("id-404", "https://i.imgur.com/deleted.png", "user1", "tag", null, "User", Instant.now(), Instant.now(), false, false, 0, null);
        when(outfitRepository.findByLinkContaining("imgur.com")).thenReturn(List.of(outfit));

        when(httpResponse.statusCode()).thenReturn(404);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ImgurToMinioRehostService.RehostReport report = rehostService.rehostAll(false, 0, 0);

        assertEquals(1, report.totalFound());
        assertEquals(0, report.succeeded());
        assertEquals(1, report.failed());
        assertEquals(1, report.failures().size());
        assertEquals("id-404", report.failures().get(0).outfitId());
        assertTrue(report.failures().get(0).reason().contains("404"));
        assertEquals("https://i.imgur.com/deleted.png", outfit.getLink()); // unmodified
        verify(outfitRepository, never()).save(any(OutfitEntity.class));
    }

    @Test
    void rehostAllHandlesNetworkExceptionGracefully() throws Exception {
        OutfitEntity outfit = new OutfitEntity("id-err", "https://i.imgur.com/error.png", "user1", "tag", null, "User", Instant.now(), Instant.now(), false, false, 0, null);
        when(outfitRepository.findByLinkContaining("imgur.com")).thenReturn(List.of(outfit));

        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection timed out"));

        ImgurToMinioRehostService.RehostReport report = rehostService.rehostAll(false, 0, 0);

        assertEquals(1, report.totalFound());
        assertEquals(0, report.succeeded());
        assertEquals(1, report.failed());
        assertEquals(1, report.failures().size());
        assertTrue(report.failures().get(0).reason().contains("Connection timed out"));
        verify(outfitRepository, never()).save(any(OutfitEntity.class));
    }

    @Test
    void rehostAllRespectsLimit() {
        OutfitEntity outfit1 = new OutfitEntity("id-1", "https://i.imgur.com/1.png", "u", "t", null, "n", Instant.now(), Instant.now(), false, false, 0, null);
        OutfitEntity outfit2 = new OutfitEntity("id-2", "https://i.imgur.com/2.png", "u", "t", null, "n", Instant.now(), Instant.now(), false, false, 0, null);
        OutfitEntity outfit3 = new OutfitEntity("id-3", "https://i.imgur.com/3.png", "u", "t", null, "n", Instant.now(), Instant.now(), false, false, 0, null);

        when(outfitRepository.findByLinkContaining("imgur.com")).thenReturn(new java.util.ArrayList<>(List.of(outfit1, outfit2, outfit3)));

        ImgurToMinioRehostService.RehostReport report = rehostService.rehostAll(true, 2, 0);

        assertEquals(3, report.totalFound());
        assertEquals(2, report.processed());
        assertEquals(2, report.succeeded());
    }
}
