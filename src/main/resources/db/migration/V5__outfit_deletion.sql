-- FashionScape Bot v3 - Deletion details
-- Flyway V5: Record when and by whom an outfit was deleted. Deleted outfits are kept
-- indefinitely and their images are moved out of public read (see MinioStorageService).

ALTER TABLE outfits ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outfits ADD COLUMN deleted_by VARCHAR(64);
