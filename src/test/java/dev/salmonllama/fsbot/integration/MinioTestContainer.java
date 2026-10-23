package dev.salmonllama.fsbot.integration;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The MinIO container used by integration tests.
 * <p>
 * MinIO no longer publishes community images anonymously: minio/minio was removed from Docker Hub
 * and quay.io/minio now requires a login. Chainguard's MinIO image is free and anonymously
 * pullable on its latest tag. It runs as a non-root user, so data goes under /tmp.
 */
final class MinioTestContainer {

    static final DockerImageName IMAGE = DockerImageName.parse("cgr.dev/chainguard/minio:latest")
            .asCompatibleSubstituteFor("minio/minio");

    private MinioTestContainer() {
    }

    static MinIOContainer create() {
        MinIOContainer container = new MinIOContainer(IMAGE)
                .withUserName("minioadmin")
                .withPassword("minioadmin");
        container.withCommand("server", "--console-address", ":9001", "/tmp/minio-data");
        return container;
    }
}
