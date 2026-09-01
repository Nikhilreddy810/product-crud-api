CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    product_id    INT NOT NULL,
    action        VARCHAR(20) NOT NULL,
    performed_by  VARCHAR(100) NOT NULL,
    performed_on  TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_log_product_id ON audit_log (product_id);
