package com.ai.companion.service;

import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.repository.CompanionIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanionPortraitServiceTests {

    @TempDir
    Path temporaryDirectory;

    private CompanionIdentityRepository identityRepository;
    private CompanionPortraitService portraitService;
    private CompanionIdentity identity;

    @BeforeEach
    void setUp() {
        identityRepository = mock(CompanionIdentityRepository.class);
        identity = new CompanionIdentity();
        identity.setId(17L);
        identity.setName("Companion");
        when(identityRepository.findById(17L)).thenReturn(Optional.of(identity));
        when(identityRepository.saveAndFlush(any(CompanionIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        portraitService = new CompanionPortraitService(
                identityRepository,
                temporaryDirectory.toString(),
                DataSize.ofMegabytes(5));
        portraitService.createStorageDirectory();
    }

    @Test
    void uploadAndReplacePersistsOnlyTheCurrentPortrait() throws IOException {
        var first = portraitService.upload(17L, png("first.png", 0xffff0000));
        String firstId = first.getPortraitId();
        Path firstPath = portraitService.getPortrait(17L);
        assertTrue(Files.isRegularFile(firstPath));

        var second = portraitService.upload(17L, png("second.png", 0xff0000ff));
        Path secondPath = portraitService.getPortrait(17L);

        assertNotEquals(firstId, second.getPortraitId());
        assertFalse(Files.exists(firstPath));
        assertTrue(Files.isRegularFile(secondPath));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void replacementKeepsPreviousPortraitUntilTransactionCommits() throws IOException {
        portraitService.upload(17L, png("first.png", 0xffff0000));
        Path previousPortrait = portraitService.getPortrait(17L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            portraitService.upload(17L, png("second.png", 0xff0000ff));
            assertTrue(Files.exists(previousPortrait));

            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            synchronizations.forEach(
                    synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_COMMITTED));

            assertFalse(Files.exists(previousPortrait));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsMalformedPngAndUnsupportedMimeType() {
        var malformed = new MockMultipartFile(
                "file", "broken.png", "image/png", new byte[]{1, 2, 3});
        var unsupportedMime = new MockMultipartFile(
                "file", "image.png", "image/jpeg", new byte[]{1, 2, 3});

        assertThrows(ResponseStatusException.class,
                () -> portraitService.upload(17L, malformed));
        assertThrows(ResponseStatusException.class,
                () -> portraitService.upload(17L, unsupportedMime));
        assertNull(identity.getPortraitId());
    }

    @Test
    void rejectsPortraitsAboveConfiguredLimit() {
        byte[] oversizedContent = new byte[5 * 1024 * 1024 + 1];
        var oversized = new MockMultipartFile(
                "file", "large.png", "image/png", oversizedContent);

        assertThrows(ResponseStatusException.class,
                () -> portraitService.upload(17L, oversized));
        assertNull(identity.getPortraitId());
    }

    @Test
    void removeClearsOnlyTheAssociatedPortrait() throws IOException {
        portraitService.upload(17L, png("portrait.png", 0xff00ff00));
        Path portrait = portraitService.getPortrait(17L);
        Path unrelatedPortrait = temporaryDirectory.resolve(UUID.randomUUID() + ".png");
        Files.write(unrelatedPortrait, new byte[]{1, 2, 3});

        portraitService.remove(17L);

        assertNull(identity.getPortraitId());
        assertFalse(Files.exists(portrait));
        assertTrue(Files.exists(unrelatedPortrait));
        assertThrows(ResponseStatusException.class,
                () -> portraitService.getPortrait(17L));
        verify(identityRepository, times(2)).saveAndFlush(identity);
    }

    private MockMultipartFile png(String filename, int color) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, color);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile(
                "file", filename, "image/png", output.toByteArray());
    }
}
