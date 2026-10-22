/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage;

public record UploadResult(
        String objectName,
        String publicUrl,
        String bucket,
        long size,
        String contentType
) {}
