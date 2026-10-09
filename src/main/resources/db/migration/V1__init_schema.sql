CREATE TABLE app_user (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL
);

CREATE TABLE product (
    id            SERIAL PRIMARY KEY,
    product_name  VARCHAR(255) NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    created_on    TIMESTAMP    NOT NULL,
    modified_by   VARCHAR(100),
    modified_on   TIMESTAMP
);

CREATE INDEX idx_product_name ON product (product_name);

CREATE TABLE item (
    id            SERIAL PRIMARY KEY,
    product_id    INT NOT NULL,
    quantity      INT NOT NULL,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX idx_item_product_id ON item (product_id);
