package dev.salmonllama.fsbot.storage;

public record UploadResult(
        String objectName,
        String publicUrl,
        String bucket,
        long size,
        String contentType
) {}
