-- FashionScape Bot v3 - Outfit votes
-- Flyway V4: One vote per user per outfit, counted atomically

CREATE TABLE IF NOT EXISTS outfit_votes (
    outfit_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (outfit_id, user_id),
    CONSTRAINT fk_outfit_votes_outfit FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE
);

-- No votes exist before v3, so every outfit starts at 0
ALTER TABLE outfits ADD COLUMN vote_count INTEGER NOT NULL DEFAULT 0;
