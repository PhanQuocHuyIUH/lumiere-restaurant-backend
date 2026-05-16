-- =====================================================
-- V7: Simulate 3 months of restaurant operations.
--
-- Rules:
--   Operating hours: 08:00–24:00 strictly.
--   Shifts: 2 per day.
--     Day   shift: 08:00–16:00
--     Night shift: 16:00–24:00
--   Cashier rotation: alternates daily between
--     cashier01 and cashier02.
--   Opening totals:
--     Day shift  — random starting float 500,000–2,000,000 VND.
--     Night shift — carries over the day shift closing_total.
--   Closing total = opening_total + cash collected in shift.
--   All payments: method=CASH, provider=CASH, status=SUCCESS.
--   Kitchen tasks: fully populated with snapshot columns.
--   Tax: per-item INCLUSIVE rate (800 bps food / 1000 bps alcohol).
--        order tax_mode = MIXED (menu contains both rates).
-- =====================================================

DO $$
DECLARE
    -- Date range
    v_start_date DATE;
    v_end_date   DATE;
    v_curr_date  DATE;
    v_day_num    INT;
    v_is_weekend BOOLEAN;

    -- Staff lookups
    v_cashier1_id BIGINT;
    v_cashier2_id BIGINT;
    v_manager_id  BIGINT;
    v_waiter_ids  BIGINT[];

    -- Shift rotation per day
    v_day_cashier_id   BIGINT;
    v_night_cashier_id BIGINT;
    v_day_shift_id     BIGINT;
    v_night_shift_id   BIGINT;

    -- Shift time bounds
    v_day_open    TIMESTAMPTZ;
    v_day_close   TIMESTAMPTZ;
    v_night_open  TIMESTAMPTZ;
    v_night_close TIMESTAMPTZ;

    -- Shift financials (BIGINT = VND integers)
    v_day_opening_total    BIGINT;
    v_night_opening_total  BIGINT;
    v_day_cash_collected   BIGINT;
    v_night_cash_collected BIGINT;

    -- Order generation
    v_num_orders        INT;
    v_order_id          BIGINT;
    v_revision_id       BIGINT;
    v_order_time        TIMESTAMPTZ;
    v_table_id          BIGINT;
    v_waiter_id         BIGINT;
    v_order_gross_total BIGINT;
    v_order_net_total   BIGINT;
    v_order_tax_total   BIGINT;
    v_num_items         INT;
    v_qty               INT;
    v_order_item_id     BIGINT;

    -- Menu item snapshot fields
    v_mi_id       BIGINT;
    v_mi_price    BIGINT;
    v_mi_name     TEXT;
    v_mi_img      TEXT;
    v_mi_cook     INT;
    v_mi_tax_mode TEXT;
    v_mi_tax_bps  INT;

    -- Per-item tax breakdown (INCLUSIVE reverse-extract)
    v_item_gross_sub BIGINT;
    v_item_net_sub   BIGINT;
    v_item_tax_sub   BIGINT;

    -- Pre-fetched lookup arrays
    v_tables   BIGINT[];
    v_menu_ids BIGINT[];

    -- Helpers
    v_n_tables INT;
    v_n_menus  INT;
    v_n_waiter INT;
BEGIN
    v_start_date := (CURRENT_DATE - INTERVAL '3 months')::DATE;
    v_end_date   := CURRENT_DATE;

    -- ── Load static lookups ───────────────────────
    SELECT id INTO v_cashier1_id FROM identity.staff WHERE username = 'cashier01' LIMIT 1;
    SELECT id INTO v_cashier2_id FROM identity.staff WHERE username = 'cashier02' LIMIT 1;
    SELECT id INTO v_manager_id  FROM identity.staff WHERE role = 'MANAGER'  LIMIT 1;

    SELECT ARRAY_AGG(id ORDER BY id) INTO v_waiter_ids
    FROM identity.staff WHERE role = 'WAITER' AND status = 'ACTIVE';

    SELECT ARRAY_AGG(id ORDER BY id) INTO v_tables
    FROM table_mgmt.tables WHERE deleted_at IS NULL;

    SELECT ARRAY_AGG(id ORDER BY id) INTO v_menu_ids
    FROM menu.menu_items
    WHERE item_type = 'SINGLE' AND is_available = TRUE AND deleted_at IS NULL;

    v_n_tables := array_length(v_tables,   1);
    v_n_menus  := array_length(v_menu_ids, 1);
    v_n_waiter := array_length(v_waiter_ids, 1);

    IF v_n_tables IS NULL OR v_n_menus IS NULL OR v_n_waiter IS NULL THEN
        RAISE EXCEPTION 'V7 simulation requires tables, menu items, and waiter accounts to be seeded first.';
    END IF;

    -- ── Main loop: one iteration per day ──────────
    v_curr_date := v_start_date;
    v_day_num   := 0;

    WHILE v_curr_date <= v_end_date LOOP

        v_is_weekend := EXTRACT(ISODOW FROM v_curr_date) IN (6, 7);

        -- Rotate cashiers each day
        IF v_day_num % 2 = 0 THEN
            v_day_cashier_id   := v_cashier1_id;
            v_night_cashier_id := v_cashier2_id;
        ELSE
            v_day_cashier_id   := v_cashier2_id;
            v_night_cashier_id := v_cashier1_id;
        END IF;

        -- Shift timestamps
        v_day_open    := v_curr_date::TIMESTAMPTZ + INTERVAL '8 hours';
        v_day_close   := v_curr_date::TIMESTAMPTZ + INTERVAL '16 hours';
        v_night_open  := v_curr_date::TIMESTAMPTZ + INTERVAL '16 hours';
        v_night_close := v_curr_date::TIMESTAMPTZ + INTERVAL '24 hours';

        -- Day shift opening float: 500,000–2,000,000 VND rounded to nearest 1,000
        v_day_opening_total :=
            (floor(random() * 1501) * 1000)::BIGINT + 500000;

        -- ── Create day shift ─────────────────────
        INSERT INTO shift.cashier_shifts (
            cashier_id, opened_by, opened_at, closed_at,
            opening_total_vnd, notes, created_at, updated_at
        ) VALUES (
            v_day_cashier_id, v_manager_id,
            v_day_open, v_day_close,
            v_day_opening_total,
            'Day shift (08:00–16:00)',
            v_day_open, v_day_open
        ) RETURNING id INTO v_day_shift_id;

        -- ── Day shift orders ─────────────────────
        v_num_orders := CASE WHEN v_is_weekend
            THEN (floor(random() * 11) + 15)::INT   -- 15–25
            ELSE (floor(random() * 11) + 10)::INT   -- 10–20
        END;
        v_day_cash_collected := 0;

        FOR i IN 1..v_num_orders LOOP
            -- Order time: 08:00 to 14:30 (leaves ≥90 min before shift end)
            v_order_time := v_day_open + (random() * 6.5) * INTERVAL '1 hour';
            v_table_id   := v_tables[1 + (floor(random() * v_n_tables))::INT];
            v_waiter_id  := v_waiter_ids[1 + (floor(random() * v_n_waiter))::INT];

            -- Create order; totals updated after items loop
            INSERT INTO ordering.orders (
                table_id, status, total_amount_vnd,
                subtotal_amount_vnd, tax_amount_vnd,
                tax_mode, tax_rate_bps, tax_snapshot_version,
                confirmed_by, served_by,
                created_at, confirmed_at, ready_at, served_at, paid_at
            ) VALUES (
                v_table_id, 'PAID', 0,
                0, 0, 'NO_TAX', 0, 1,
                v_waiter_id, v_waiter_id,
                v_order_time,
                v_order_time + INTERVAL '2 minutes',
                v_order_time + INTERVAL '20 minutes',
                v_order_time + INTERVAL '25 minutes',
                v_order_time + INTERVAL '55 minutes'
            ) RETURNING id INTO v_order_id;

            -- Single revision created by staff
            INSERT INTO ordering.order_revisions (
                order_id, revision_number, revision_source,
                created_by_staff, created_at
            ) VALUES (
                v_order_id, 1, 'STAFF',
                v_waiter_id, v_order_time
            ) RETURNING id INTO v_revision_id;

            v_order_gross_total := 0;
            v_order_net_total   := 0;
            v_order_tax_total   := 0;
            v_num_items := (floor(random() * 4) + 2)::INT;  -- 2–5 items

            FOR j IN 1..v_num_items LOOP
                v_mi_id := v_menu_ids[1 + (floor(random() * v_n_menus))::INT];

                SELECT price_vnd, name, image_url, cook_time,
                       COALESCE(item_tax_mode, 'INCLUSIVE'),
                       COALESCE(item_tax_rate_bps, 800)
                INTO   v_mi_price, v_mi_name, v_mi_img, v_mi_cook,
                       v_mi_tax_mode, v_mi_tax_bps
                FROM   menu.menu_items WHERE id = v_mi_id;

                v_qty := (floor(random() * 2) + 1)::INT;  -- 1–2

                -- INCLUSIVE reverse-extract: net = ROUND(gross * 10000 / (10000 + bps))
                v_item_gross_sub := v_mi_price * v_qty;
                v_item_net_sub   := ROUND(v_item_gross_sub * 10000.0 / (10000 + v_mi_tax_bps))::BIGINT;
                v_item_tax_sub   := v_item_gross_sub - v_item_net_sub;

                INSERT INTO ordering.order_items (
                    revision_id, menu_item_id, quantity, unit_price_vnd,
                    unit_tax_mode, unit_tax_rate_bps,
                    net_subtotal_vnd, tax_subtotal_vnd,
                    status, created_at
                ) VALUES (
                    v_revision_id, v_mi_id, v_qty, v_mi_price,
                    v_mi_tax_mode, v_mi_tax_bps,
                    v_item_net_sub, v_item_tax_sub,
                    'SERVED', v_order_time
                ) RETURNING id INTO v_order_item_id;

                v_order_gross_total := v_order_gross_total + v_item_gross_sub;
                v_order_net_total   := v_order_net_total   + v_item_net_sub;
                v_order_tax_total   := v_order_tax_total   + v_item_tax_sub;

                -- Kitchen task with full snapshot
                INSERT INTO kitchen.kitchen_tasks (
                    order_item_id,
                    order_id, table_id, menu_item_id,
                    menu_item_name, menu_item_image_url,
                    quantity, order_item_note, order_note,
                    expected_cook_time,
                    status, started_at, completed_at, created_at
                ) VALUES (
                    v_order_item_id,
                    v_order_id, v_table_id, v_mi_id,
                    v_mi_name, v_mi_img,
                    v_qty, NULL, NULL,
                    v_mi_cook,
                    'DONE',
                    v_order_time + INTERVAL '3 minutes',
                    v_order_time + INTERVAL '18 minutes',
                    v_order_time
                );
            END LOOP;

            -- Update order with final totals; MIXED because menu has 8% and 10% items
            UPDATE ordering.orders SET
                total_amount_vnd    = v_order_gross_total,
                subtotal_amount_vnd = v_order_net_total,
                tax_amount_vnd      = v_order_tax_total,
                tax_mode            = 'MIXED',
                tax_rate_bps        = 800,
                tax_snapshot_version = 1
            WHERE id = v_order_id;

            INSERT INTO payment.payments (
                order_id, shift_id, amount_vnd,
                subtotal_amount_vnd, tax_amount_vnd, tax_mode, tax_rate_bps,
                payment_method, provider, status,
                cashier_id, created_at, paid_at
            ) VALUES (
                v_order_id, v_day_shift_id, v_order_gross_total,
                v_order_net_total, v_order_tax_total, 'MIXED', 800,
                'CASH', 'CASH', 'SUCCESS',
                v_day_cashier_id,
                v_order_time + INTERVAL '53 minutes',
                v_order_time + INTERVAL '55 minutes'
            );

            v_day_cash_collected := v_day_cash_collected + v_order_gross_total;
        END LOOP;

        -- Close day shift: closing_total = opening_total + cash collected
        UPDATE shift.cashier_shifts SET
            closing_total_vnd = v_day_opening_total + v_day_cash_collected,
            closing_notes     = 'Shift closed normally.',
            closed_by         = v_manager_id,
            updated_at        = v_day_close
        WHERE id = v_day_shift_id;

        -- Night shift opening_total = day shift closing_total (carry-over)
        v_night_opening_total := v_day_opening_total + v_day_cash_collected;

        -- ── Create night shift ────────────────────
        INSERT INTO shift.cashier_shifts (
            cashier_id, opened_by, opened_at, closed_at,
            opening_total_vnd, notes, created_at, updated_at
        ) VALUES (
            v_night_cashier_id, v_manager_id,
            v_night_open, v_night_close,
            v_night_opening_total,
            'Night shift (16:00–24:00)',
            v_night_open, v_night_open
        ) RETURNING id INTO v_night_shift_id;

        -- ── Night shift orders ────────────────────
        v_num_orders := CASE WHEN v_is_weekend
            THEN (floor(random() * 11) + 20)::INT   -- 20–30
            ELSE (floor(random() * 11) + 15)::INT   -- 15–25
        END;
        v_night_cash_collected := 0;

        FOR i IN 1..v_num_orders LOOP
            -- Order time: 16:00 to 22:30 (leaves ≥90 min before midnight)
            v_order_time := v_night_open + (random() * 6.5) * INTERVAL '1 hour';
            v_table_id   := v_tables[1 + (floor(random() * v_n_tables))::INT];
            v_waiter_id  := v_waiter_ids[1 + (floor(random() * v_n_waiter))::INT];

            INSERT INTO ordering.orders (
                table_id, status, total_amount_vnd,
                subtotal_amount_vnd, tax_amount_vnd,
                tax_mode, tax_rate_bps, tax_snapshot_version,
                confirmed_by, served_by,
                created_at, confirmed_at, ready_at, served_at, paid_at
            ) VALUES (
                v_table_id, 'PAID', 0,
                0, 0, 'NO_TAX', 0, 1,
                v_waiter_id, v_waiter_id,
                v_order_time,
                v_order_time + INTERVAL '2 minutes',
                v_order_time + INTERVAL '20 minutes',
                v_order_time + INTERVAL '25 minutes',
                v_order_time + INTERVAL '55 minutes'
            ) RETURNING id INTO v_order_id;

            INSERT INTO ordering.order_revisions (
                order_id, revision_number, revision_source,
                created_by_staff, created_at
            ) VALUES (
                v_order_id, 1, 'STAFF',
                v_waiter_id, v_order_time
            ) RETURNING id INTO v_revision_id;

            v_order_gross_total := 0;
            v_order_net_total   := 0;
            v_order_tax_total   := 0;
            v_num_items := (floor(random() * 4) + 2)::INT;

            FOR j IN 1..v_num_items LOOP
                v_mi_id := v_menu_ids[1 + (floor(random() * v_n_menus))::INT];

                SELECT price_vnd, name, image_url, cook_time,
                       COALESCE(item_tax_mode, 'INCLUSIVE'),
                       COALESCE(item_tax_rate_bps, 800)
                INTO   v_mi_price, v_mi_name, v_mi_img, v_mi_cook,
                       v_mi_tax_mode, v_mi_tax_bps
                FROM   menu.menu_items WHERE id = v_mi_id;

                v_qty := (floor(random() * 2) + 1)::INT;

                v_item_gross_sub := v_mi_price * v_qty;
                v_item_net_sub   := ROUND(v_item_gross_sub * 10000.0 / (10000 + v_mi_tax_bps))::BIGINT;
                v_item_tax_sub   := v_item_gross_sub - v_item_net_sub;

                INSERT INTO ordering.order_items (
                    revision_id, menu_item_id, quantity, unit_price_vnd,
                    unit_tax_mode, unit_tax_rate_bps,
                    net_subtotal_vnd, tax_subtotal_vnd,
                    status, created_at
                ) VALUES (
                    v_revision_id, v_mi_id, v_qty, v_mi_price,
                    v_mi_tax_mode, v_mi_tax_bps,
                    v_item_net_sub, v_item_tax_sub,
                    'SERVED', v_order_time
                ) RETURNING id INTO v_order_item_id;

                v_order_gross_total := v_order_gross_total + v_item_gross_sub;
                v_order_net_total   := v_order_net_total   + v_item_net_sub;
                v_order_tax_total   := v_order_tax_total   + v_item_tax_sub;

                INSERT INTO kitchen.kitchen_tasks (
                    order_item_id,
                    order_id, table_id, menu_item_id,
                    menu_item_name, menu_item_image_url,
                    quantity, order_item_note, order_note,
                    expected_cook_time,
                    status, started_at, completed_at, created_at
                ) VALUES (
                    v_order_item_id,
                    v_order_id, v_table_id, v_mi_id,
                    v_mi_name, v_mi_img,
                    v_qty, NULL, NULL,
                    v_mi_cook,
                    'DONE',
                    v_order_time + INTERVAL '3 minutes',
                    v_order_time + INTERVAL '18 minutes',
                    v_order_time
                );
            END LOOP;

            UPDATE ordering.orders SET
                total_amount_vnd    = v_order_gross_total,
                subtotal_amount_vnd = v_order_net_total,
                tax_amount_vnd      = v_order_tax_total,
                tax_mode            = 'MIXED',
                tax_rate_bps        = 800,
                tax_snapshot_version = 1
            WHERE id = v_order_id;

            INSERT INTO payment.payments (
                order_id, shift_id, amount_vnd,
                subtotal_amount_vnd, tax_amount_vnd, tax_mode, tax_rate_bps,
                payment_method, provider, status,
                cashier_id, created_at, paid_at
            ) VALUES (
                v_order_id, v_night_shift_id, v_order_gross_total,
                v_order_net_total, v_order_tax_total, 'MIXED', 800,
                'CASH', 'CASH', 'SUCCESS',
                v_night_cashier_id,
                v_order_time + INTERVAL '53 minutes',
                v_order_time + INTERVAL '55 minutes'
            );

            v_night_cash_collected := v_night_cash_collected + v_order_gross_total;
        END LOOP;

        -- Close night shift
        UPDATE shift.cashier_shifts SET
            closing_total_vnd = v_night_opening_total + v_night_cash_collected,
            closing_notes     = 'Shift closed normally.',
            closed_by         = v_manager_id,
            updated_at        = v_night_close
        WHERE id = v_night_shift_id;

        v_curr_date := v_curr_date + INTERVAL '1 day';
        v_day_num   := v_day_num   + 1;
    END LOOP;
END $$;
