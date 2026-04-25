-- V5: Add image support for stores and products

ALTER TABLE stores   ADD COLUMN logo_url  VARCHAR(500);
ALTER TABLE products ADD COLUMN image_url VARCHAR(500);
