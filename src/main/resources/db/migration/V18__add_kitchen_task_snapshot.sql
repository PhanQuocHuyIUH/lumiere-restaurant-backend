ALTER TABLE kitchen.kitchen_tasks
    ADD COLUMN IF NOT EXISTS order_id BIGINT,
    ADD COLUMN IF NOT EXISTS table_id BIGINT,
    ADD COLUMN IF NOT EXISTS menu_item_id BIGINT,
    ADD COLUMN IF NOT EXISTS menu_item_name TEXT,
    ADD COLUMN IF NOT EXISTS menu_item_image_url TEXT,
    ADD COLUMN IF NOT EXISTS quantity INT,
    ADD COLUMN IF NOT EXISTS order_item_note TEXT,
    ADD COLUMN IF NOT EXISTS order_note TEXT,
    ADD COLUMN IF NOT EXISTS expected_cook_time INT;

UPDATE kitchen.kitchen_tasks kt
SET
    order_id = o.id,
    table_id = o.table_id,
    menu_item_id = oi.menu_item_id,
    menu_item_name = mi.name,
    menu_item_image_url = mi.image_url,
    quantity = oi.quantity,
    order_item_note = oi.note,
    order_note = o.note,
    expected_cook_time = mi.cook_time
FROM ordering.order_items oi
JOIN ordering.order_revisions orv ON orv.id = oi.revision_id
JOIN ordering.orders o ON o.id = orv.order_id
LEFT JOIN menu.menu_items mi ON mi.id = oi.menu_item_id
WHERE kt.order_item_id = oi.id;

CREATE INDEX IF NOT EXISTS idx_kitchen_tasks_order_id ON kitchen.kitchen_tasks(order_id);
CREATE INDEX IF NOT EXISTS idx_kitchen_tasks_menu_item_id ON kitchen.kitchen_tasks(menu_item_id);