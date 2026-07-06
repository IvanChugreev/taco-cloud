ALTER TABLE ingredients
    ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    ADD CONSTRAINT ck_ingredients_price CHECK (price >= 0);

UPDATE ingredients
SET price = CASE id
    WHEN 'FLTO' THEN 1.00
    WHEN 'COTO' THEN 1.00
    WHEN 'GRBF' THEN 2.50
    WHEN 'CARN' THEN 2.75
    WHEN 'TMTO' THEN 0.75
    WHEN 'LETC' THEN 0.50
    WHEN 'CHED' THEN 1.25
    WHEN 'JACK' THEN 1.25
    WHEN 'SLSA' THEN 0.75
    WHEN 'SRCR' THEN 0.75
    ELSE price
END;

ALTER TABLE orders
    ADD COLUMN order_uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN total_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN comment VARCHAR(500),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT uk_orders_order_uuid UNIQUE (order_uuid),
    ADD CONSTRAINT ck_orders_total_price CHECK (total_price >= 0);

ALTER TABLE orders RENAME COLUMN placed_at TO created_at;
ALTER INDEX idx_orders_placed_at RENAME TO idx_orders_created_at;

UPDATE orders SET updated_at = created_at;

UPDATE orders
SET status = CASE status
    WHEN 'PLACED' THEN 'CREATED'
    WHEN 'DELIVERED' THEN 'COMPLETED'
    ELSE status
END;

ALTER TABLE orders
    ALTER COLUMN status SET DEFAULT 'CREATED',
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT ck_orders_status CHECK (
        status IN ('CREATED', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'REJECTED', 'CANCELLED')
    );

UPDATE orders AS current_order
SET total_price = calculated.total_price
FROM (
    SELECT order_items.order_id, COALESCE(SUM(ingredients.price), 0.00) AS total_price
    FROM order_items
    JOIN taco_ingredients ON taco_ingredients.taco_id = order_items.taco_id
    JOIN ingredients ON ingredients.id = taco_ingredients.ingredient_id
    GROUP BY order_items.order_id
) AS calculated
WHERE current_order.id = calculated.order_id;
