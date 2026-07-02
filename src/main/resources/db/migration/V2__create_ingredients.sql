CREATE TABLE ingredients (
    id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    CONSTRAINT ck_ingredients_type
        CHECK (type IN ('WRAP', 'PROTEIN', 'VEGGIES', 'CHEESE', 'SAUCE'))
);

CREATE INDEX idx_ingredients_type ON ingredients (type);
