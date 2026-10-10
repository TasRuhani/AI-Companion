package com.ai.companion.service;

import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.repository.CompanionIdentityRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;
import java.util.List;

@Service
public class CompanionPortraitService {

    private static final Logger log =
            LoggerFactory.getLogger(CompanionPortraitService.class);
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 40_000_000L;

    private final CompanionIdentityRepository identityRepository;
    private final Path storageDirectory;
    private final long maxFileSize;

    public CompanionPortraitService(
            CompanionIdentityRepository identityRepository,
            @Value("${companion.portrait.storage-dir}") String storageDirectory,
            @Value("${companion.portrait.max-file-size:5MB}") DataSize maxFileSize
    ) {
        this.identityRepository = identityRepository;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize.toBytes();
    }

    @PostConstruct
    void createStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not create the companion portrait storage directory.", ex);
        }
    }

    @Transactional
    public CompanionIdentity upload(Long identityId, MultipartFile file) {
        CompanionIdentity identity = findIdentity(identityId);
        byte[] bytes = validate(file);
        String previousPortraitId = identity.getPortraitId();
        String portraitId = UUID.randomUUID().toString();
        Path destination = portraitPath(portraitId);

        try {
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            identity.setPortraitId(portraitId);
            identityRepository.saveAndFlush(identity);
        } catch (IOException | RuntimeException ex) {
            identity.setPortraitId(previousPortraitId);
            deleteQuietly(destination, "new portrait after a failed upload");
            if (ex instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new IllegalStateException(
                    "Could not save the companion portrait.", ex);
        }

        afterTransactionCommit(
                () -> {
                    if (previousPortraitId != null) {
                        deleteQuietly(portraitPath(previousPortraitId), "replaced portrait");
                    }
                },
                () -> deleteQuietly(destination, "new portrait after a rolled-back upload"));
        return identity;
    }

    @Transactional(readOnly = true)
    public Path getPortrait(Long identityId) {
        CompanionIdentity identity = findIdentity(identityId);
        if (identity.getPortraitId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "This companion has no portrait.");
        }
        Path path = portraitPath(identity.getPortraitId());
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "The companion portrait could not be found.");
        }
        return path;
    }

    @Transactional
    public void remove(Long identityId) {
        CompanionIdentity identity = findIdentity(identityId);
        String portraitId = identity.getPortraitId();
        if (portraitId == null) {
            return;
        }

        identity.setPortraitId(null);
        identityRepository.saveAndFlush(identity);
        afterTransactionCommit(
                () -> deleteQuietly(portraitPath(portraitId), "removed portrait"),
                () -> { });
    }

    public void removeUnreferenced(List<String> portraitIds) {
        IllegalStateException cleanupFailure = null;
        for (String portraitId : portraitIds) {
            if (portraitId != null && !portraitId.isBlank()) {
                try {
                    Files.deleteIfExists(portraitPath(portraitId));
                } catch (IOException | IllegalStateException ex) {
                    log.error("Could not remove an associated portrait after reset-all.", ex);
                    if (cleanupFailure == null) {
                        cleanupFailure = new IllegalStateException(
                                "One or more associated portrait files could not be removed.", ex);
                    } else {
                        cleanupFailure.addSuppressed(ex);
                    }
                }
            }
        }
        if (cleanupFailure != null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database data was reset, but one or more associated portraits could not be removed.",
                    cleanupFailure);
        }
    }

    private CompanionIdentity findIdentity(Long identityId) {
        return identityRepository.findById(identityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Companion identity not found."));
    }

    private byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a PNG image to upload.");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "The portrait exceeds the upload size limit.");
        }
        if (!"image/png".equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only PNG images are supported.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > maxFileSize || !hasPngSignature(bytes)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "The uploaded file is not a valid PNG image.");
            }
            validatePngData(bytes);
            return bytes;
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The uploaded PNG image could not be read.", ex);
        }
    }

    private void validatePngData(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw invalidPng();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidPng();
            }

            ImageReader reader = readers.next();
            try {
                if (!"png".equalsIgnoreCase(reader.getFormatName())) {
                    throw invalidPng();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION
                        || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "The PNG dimensions are too large.");
                }
                if (reader.read(0) == null) {
                    throw invalidPng();
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private ResponseStatusException invalidPng() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "The uploaded file is not a valid PNG image.");
    }

    private boolean hasPngSignature(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (bytes[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private Path portraitPath(String portraitId) {
        try {
            UUID.fromString(portraitId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Stored portrait identifier is invalid.", ex);
        }
        Path path = storageDirectory.resolve(portraitId + ".png").normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalStateException("Portrait path is outside its storage directory.");
        }
        return path;
    }

    private void deleteQuietly(Path path, String description) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.error("Could not clean up {} from portrait storage.", description, ex);
        }
    }

    private void afterTransactionCommit(Runnable afterCommit, Runnable afterRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            afterCommit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        afterCommit.run();
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            afterRollback.run();
                        }
                    }
                });
    }
}
