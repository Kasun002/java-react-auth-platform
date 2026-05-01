CREATE TABLE addresses (
    id            BIGSERIAL    PRIMARY KEY,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    street        VARCHAR(255),
    postal_code   VARCHAR(20),
    state         VARCHAR(50),
    country       VARCHAR(255) NOT NULL,
    user_id       BIGINT       REFERENCES users(id) ON DELETE CASCADE
);
