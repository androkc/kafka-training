--liquibase formatted sql
--changeset Petr:1
CREATE TYPE status_new AS ENUM ('IN_PROGRESS', 'CANCELED', 'APPROVED');
--changeset Petr:2
CREATE CAST (varchar AS status_new) WITH INOUT AS IMPLICIT;
--changeset Petr:3
CREATE TABLE orders (
                        id SERIAL PRIMARY KEY NOT NULL,
                        product_name VARCHAR(255) NOT NULL,
                        bar_code VARCHAR(255) NOT NULL,
                        quantity INT NOT NULL,
                        price NUMERIC(19, 2),
                        amount NUMERIC(19, 2),
                        order_date TIMESTAMP NOT NULL,
                        status status_new NOT NULL
);