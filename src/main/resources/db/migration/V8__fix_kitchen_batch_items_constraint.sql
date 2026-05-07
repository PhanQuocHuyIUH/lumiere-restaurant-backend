-- Remove incorrect UNIQUE constraint on batch_id in kitchen_batch_items table

ALTER TABLE kitchen.kitchen_batch_items
DROP CONSTRAINT IF EXISTS kitchen_batch_items_batch_id_key;
