-- ==============================================================================
-- V12: Seed Order History (3 Months of Data)
-- ==============================================================================

DO $$
DECLARE
    v_start_date DATE := CURRENT_DATE - INTERVAL '90 days';
    v_end_date DATE := CURRENT_DATE;
    v_curr_date DATE;
    v_num_orders INT;
    v_order_time TIMESTAMPTZ;
    v_table_id BIGINT;
    v_waiter_id BIGINT;
    v_cashier_id BIGINT;
    
    v_order_id BIGINT;
    v_revision_id BIGINT;
    v_menu_item RECORD;
    v_num_items INT;
    v_qty INT;
    v_order_item_id BIGINT;
    
    v_item_idx INT;
    v_total_amount NUMERIC(12,2);
    
    -- Cache for random selection
    v_tables BIGINT[];
    v_waiters BIGINT[];
    v_cashiers BIGINT[];
    -- Đã xóa dòng: v_menu_items RECORD[];
BEGIN
    -- Pre-fetch lookup data
    SELECT ARRAY_AGG(id) INTO v_tables FROM table_mgmt.tables;
    SELECT ARRAY_AGG(id) INTO v_waiters FROM identity.staff WHERE role = 'WAITER' AND status = 'ACTIVE';
    SELECT ARRAY_AGG(id) INTO v_cashiers FROM identity.staff WHERE role = 'CASHIER' AND status = 'ACTIVE';

    -- Pre-fetch SINGLE menu items that are available
    -- Create a temporary table or just use a cursor. Array of records isn't directly supported in PL/pgSQL easily,
    -- so we'll use a dynamic query or just select a random ID each time. Since there are only ~40 items, random order is fast.

    v_curr_date := v_start_date;

    WHILE v_curr_date <= v_end_date LOOP
        -- Decide number of orders for the day (e.g., 15 to 30)
        -- Weekend (Saturday=6, Sunday=0 in EXTRACT DOW) has more orders
        IF EXTRACT(ISODOW FROM v_curr_date) IN (6, 7) THEN
            v_num_orders := floor(random() * (35 - 25 + 1) + 25)::INT; -- 25 to 35
        ELSE
            v_num_orders := floor(random() * (25 - 15 + 1) + 15)::INT; -- 15 to 25
        END IF;

        FOR i IN 1..v_num_orders LOOP
            -- Determine order time: Lunch (11:00 - 14:00) or Dinner (17:00 - 22:00)
            IF random() < 0.4 THEN
                v_order_time := v_curr_date + (11 + random() * 3) * INTERVAL '1 hour';
            ELSE
                v_order_time := v_curr_date + (17 + random() * 5) * INTERVAL '1 hour';
            END IF;

            -- Select random table, waiter, cashier
            v_table_id := v_tables[floor(random() * array_length(v_tables, 1) + 1)];
            v_waiter_id := v_waiters[floor(random() * array_length(v_waiters, 1) + 1)];
            v_cashier_id := v_cashiers[floor(random() * array_length(v_cashiers, 1) + 1)];

            -- 1. Create Order
            INSERT INTO ordering.orders (
                table_id, status, total_amount, confirmed_by, served_by, 
                created_at, confirmed_at, ready_at, served_at, paid_at
            ) VALUES (
                v_table_id, 'PAID', 0, v_waiter_id, v_waiter_id,
                v_order_time, 
                v_order_time + INTERVAL '2 minutes', 
                v_order_time + INTERVAL '20 minutes', 
                v_order_time + INTERVAL '25 minutes', 
                v_order_time + INTERVAL '55 minutes'
            ) RETURNING id INTO v_order_id;

            -- 2. Create Order Revision
            INSERT INTO ordering.order_revisions (
                order_id, revision_number, revision_source, created_by_staff, created_at
            ) VALUES (
                v_order_id, 1, 'STAFF', v_waiter_id, v_order_time
            ) RETURNING id INTO v_revision_id;

            v_total_amount := 0;
            v_num_items := floor(random() * (5 - 2 + 1) + 2)::INT; -- 2 to 5 items

            -- 3. Create Order Items and Kitchen Tasks
            FOR v_item_idx IN 1..v_num_items LOOP
                -- Pick a random SINGLE item
                SELECT id, price INTO v_menu_item FROM menu.menu_items 
                WHERE item_type = 'SINGLE' AND is_available = TRUE
                ORDER BY random() LIMIT 1;
                
                v_qty := floor(random() * 2 + 1)::INT; -- 1 to 2
                
                INSERT INTO ordering.order_items (
                    revision_id, menu_item_id, quantity, unit_price, status, created_at
                ) VALUES (
                    v_revision_id, v_menu_item.id, v_qty, v_menu_item.price, 'SERVED', v_order_time
                ) RETURNING id INTO v_order_item_id;

                v_total_amount := v_total_amount + (v_qty * v_menu_item.price);

                -- Insert Kitchen Task
                INSERT INTO kitchen.kitchen_tasks (
                    order_item_id, status, started_at, completed_at, created_at
                ) VALUES (
                    v_order_item_id, 'DONE', 
                    v_order_time + INTERVAL '5 minutes', 
                    v_order_time + INTERVAL '20 minutes', 
                    v_order_time
                );
            END LOOP;

            -- 4. Update Order Total Amount
            UPDATE ordering.orders 
            SET total_amount = v_total_amount 
            WHERE id = v_order_id;

            -- 5. Create Payment
            INSERT INTO payment.payments (
                order_id, amount, payment_method, provider, status, 
                idempotency_key, cashier_id, created_at, paid_at
            ) VALUES (
                v_order_id, v_total_amount, 'CASH', 'CASH', 'SUCCESS',
                gen_random_uuid()::text, v_cashier_id, v_order_time + INTERVAL '53 minutes', v_order_time + INTERVAL '55 minutes'
            );

        END LOOP;

        v_curr_date := v_curr_date + INTERVAL '1 day';
    END LOOP;
END $$;