-- FashionScape Bot v3 - Native UUID outfit IDs
-- Flyway V6: Store outfit IDs as uuid instead of VARCHAR

-- Outfits saved before this used 8-character IDs. Give each one a new UUID and carry it
-- over to its votes and audit entries. Their images keep their old storage keys: the
-- link column still points at them.
ALTER TABLE outfit_votes DROP CONSTRAINT fk_outfit_votes_outfit;

CREATE TABLE outfit_id_remap (
    old_id VARCHAR(64) PRIMARY KEY,
    new_id VARCHAR(64) NOT NULL
);

INSERT INTO outfit_id_remap (old_id, new_id)
SELECT id, CAST(gen_random_uuid() AS VARCHAR)
FROM outfits
WHERE id NOT LIKE '________-____-____-____-____________';

UPDATE outfits
SET id = (SELECT new_id FROM outfit_id_remap WHERE old_id = outfits.id)
WHERE id IN (SELECT old_id FROM outfit_id_remap);

UPDATE outfit_votes
SET outfit_id = (SELECT new_id FROM outfit_id_remap WHERE old_id = outfit_votes.outfit_id)
WHERE outfit_id IN (SELECT old_id FROM outfit_id_remap);

UPDATE audit_log
SET target_id = (SELECT new_id FROM outfit_id_remap WHERE old_id = audit_log.target_id)
WHERE target_type = 'outfit' AND target_id IN (SELECT old_id FROM outfit_id_remap);

DROP TABLE outfit_id_remap;

ALTER TABLE outfits ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE outfits ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE outfit_votes ALTER COLUMN outfit_id TYPE UUID USING CAST(outfit_id AS UUID);

ALTER TABLE outfit_votes ADD CONSTRAINT fk_outfit_votes_outfit
    FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE;
