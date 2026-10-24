package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.services.OutfitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Votes and display counts against real PostgreSQL. The vote insert uses ON CONFLICT, which H2
 * doesn't support, so these cases live here rather than in a @DataJpaTest.
 */
@Testcontainers
@SpringBootTest
class OutfitVoteConcurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("fashionscape_votes")
            .withUsername("fsbot_test")
            .withPassword("fsbot_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private OutfitService outfitService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID targetId;

    @BeforeEach
    void setUp() {
        outfitRepository.deleteAll();
        targetId = outfitRepository.save(new OutfitEntity(null, "https://media.fashionscape.org/vote-target.png",
                "submitter", "tag", null, "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1", "channel-1")).getId();
    }

    private void runConcurrently(int tasks, java.util.function.IntConsumer task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                int n = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    task.accept(n);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }
    }

    private int voteRows() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outfit_votes WHERE outfit_id = ?", Integer.class, targetId);
    }

    private int voteCount() {
        return outfitRepository.findVoteCount(targetId).orElseThrow();
    }

    @Test
    @DisplayName("New outfits start with no votes; a second click takes the vote back")
    void testToggleVote() {
        assertEquals(0, voteCount());
        assertEquals(Optional.of(new OutfitService.VoteResult(true, 1)), outfitService.toggleVote(targetId.toString(), "voter-1"));
        assertEquals(Optional.of(new OutfitService.VoteResult(true, 2)), outfitService.toggleVote(targetId.toString(), "voter-2"));
        assertEquals(Optional.of(new OutfitService.VoteResult(false, 1)), outfitService.toggleVote(targetId.toString(), "voter-1"));
        assertEquals(1, voteRows());
    }

    @Test
    @DisplayName("Saving the entity never overwrites vote_count or display_count")
    void testEntitySaveKeepsCounters() {
        outfitService.toggleVote(targetId.toString(), "voter-1");
        outfitService.recordShown(targetId);

        OutfitEntity loaded = outfitRepository.findById(targetId).orElseThrow();
        loaded.setDisplayCount(99);
        loaded.setMeta("edited");
        outfitRepository.save(loaded);

        OutfitEntity after = outfitRepository.findById(targetId).orElseThrow();
        assertEquals(1, after.getVoteCount());
        assertEquals(1, after.getDisplayCount());
        assertEquals("edited", after.getMeta());
    }

    @Test
    @DisplayName("50 different users voting at once are all counted")
    void testDistinctUsersVoteConcurrently() throws Exception {
        runConcurrently(50, n -> outfitService.toggleVote(targetId.toString(), "voter-" + n));

        assertEquals(50, voteCount());
        assertEquals(50, voteRows());
    }

    @Test
    @DisplayName("One user clicking many times at once never counts more than one vote")
    void testSameUserClicksConcurrently() throws Exception {
        runConcurrently(10, n -> outfitService.toggleVote(targetId.toString(), "same-voter"));

        int count = voteCount();
        assertTrue(count == 0 || count == 1, "vote_count was " + count);
        assertEquals(voteRows(), count);
    }

    @Test
    @DisplayName("Voting on a deleted outfit changes nothing")
    void testVoteOnDeletedOutfit() {
        OutfitEntity outfit = outfitRepository.findById(targetId).orElseThrow();
        outfit.setDeleted(true);
        outfitRepository.save(outfit);

        assertTrue(outfitService.toggleVote(targetId.toString(), "voter-1").isEmpty());
        assertEquals(0, voteCount());
        assertEquals(0, voteRows());
    }

    @Test
    @DisplayName("Each /outfit random showing adds exactly one to display_count")
    void testRecordShownConcurrently() throws Exception {
        runConcurrently(25, n -> outfitService.recordShown(targetId));

        assertEquals(25, outfitRepository.findById(targetId).orElseThrow().getDisplayCount());
    }
}
