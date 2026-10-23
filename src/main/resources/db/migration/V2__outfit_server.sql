-- FashionScape Bot v3 - Outfit origin and permission integrity
-- Flyway V2: Record the server and channel each outfit was submitted in

-- Every outfit must record where it came from, so server moderators can only
-- moderate outfits submitted in their own server. Legacy data is not migrated
-- here, so there is no backfill.
ALTER TABLE outfits ADD COLUMN server_id VARCHAR(64) NOT NULL;
ALTER TABLE outfits ADD COLUMN channel_id VARCHAR(64) NOT NULL;

CREATE INDEX IF NOT EXISTS idx_outfits_server_id ON outfits(server_id);

-- A user holds each static permission at most once
CREATE UNIQUE INDEX IF NOT EXISTS uq_static_permissions_user_permission ON static_permissions(user_id, permission);
