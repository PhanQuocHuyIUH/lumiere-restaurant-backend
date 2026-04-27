-- =====================================================
-- Inventory schema + Recipe support
-- =====================================================

CREATE SCHEMA IF NOT EXISTS inventory;

-- Enum: đơn vị đo lường nguyên liệu (G = gam, ML = mililít, UNIT = đơn vị lẻ)
CREATE TYPE ingredient_unit_enum AS ENUM ('G','ML','UNIT');

-- Bảng nguyên liệu (stock gộp luôn vào đây)
CREATE TABLE inventory.ingredients (
    id                  BIGINT               GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(255)         NOT NULL,
    unit                ingredient_unit_enum NOT NULL,
    current_qty         NUMERIC(12,2)        NOT NULL DEFAULT 0 CHECK (current_qty >= 0),
    low_stock_threshold NUMERIC(12,2)        NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
    image_url           TEXT,
    created_at          TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_ingredients_name ON inventory.ingredients(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock
    ON inventory.ingredients(current_qty, low_stock_threshold)
    WHERE deleted_at IS NULL;

-- Enum: loại giao dịch kho
CREATE TYPE stock_txn_type_enum AS ENUM ('IMPORT','ADJUSTMENT','MANUAL_REPORT');

-- Bảng lịch sử giao dịch kho (audit only)
CREATE TABLE inventory.stock_transactions (
    id               BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ingredient_id    BIGINT              NOT NULL REFERENCES inventory.ingredients(id),
    txn_type         stock_txn_type_enum NOT NULL,
    quantity_before  NUMERIC(12,2)       NOT NULL,
    quantity_change  NUMERIC(12,2)       NOT NULL,
    quantity_after   NUMERIC(12,2)       NOT NULL,
    note             TEXT,
    performed_by     BIGINT              REFERENCES identity.staff(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stock_txn_ingredient ON inventory.stock_transactions(ingredient_id);
CREATE INDEX idx_stock_txn_created    ON inventory.stock_transactions(created_at DESC);

-- Bảng định lượng: liên kết MenuItem với Ingredient
CREATE TABLE menu.menu_item_ingredients (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_item_id  BIGINT        NOT NULL REFERENCES menu.menu_items(id) ON DELETE CASCADE,
    ingredient_id BIGINT        NOT NULL REFERENCES inventory.ingredients(id) ON DELETE RESTRICT,
    quantity      NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_menu_item_ingredient UNIQUE (menu_item_id, ingredient_id)
);
CREATE INDEX idx_recipe_item       ON menu.menu_item_ingredients(menu_item_id);
CREATE INDEX idx_recipe_ingredient ON menu.menu_item_ingredients(ingredient_id);
