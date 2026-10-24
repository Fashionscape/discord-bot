package dev.salmonllama.fsbot.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Single-node SeaweedFS with its S3 gateway, matching the seaweed service in docker-compose.yml.
 * The image's default command, {@code weed mini}, runs master, volume, filer and S3 in one
 * process and creates an admin identity from the access key variables.
 */
class SeaweedContainer extends GenericContainer<SeaweedContainer> {

    static final DockerImageName IMAGE = DockerImageName.parse("chrislusf/seaweedfs:4.48");
    static final int S3_PORT = 8333;
    static final String ACCESS_KEY = "seaweedadmin";
    static final String SECRET_KEY = "seaweedadmin";

    SeaweedContainer() {
        super(IMAGE);
        withExposedPorts(S3_PORT);
        withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY);
        withEnv("AWS_SECRET_ACCESS_KEY", SECRET_KEY);
        waitingFor(Wait.forHttp("/healthz").forPort(S3_PORT).withStartupTimeout(Duration.ofSeconds(90)));
    }

    String getS3URL() {
        return "http://" + getHost() + ":" + getMappedPort(S3_PORT);
    }

    String getAccessKey() {
        return ACCESS_KEY;
    }

    String getSecretKey() {
        return SECRET_KEY;
    }
}
