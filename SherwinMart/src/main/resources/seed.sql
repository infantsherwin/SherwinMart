-- SherwinMart seed data
-- Password for the admin/demo accounts below is: password
-- Hash is a standard bcrypt(10) digest — verified against jBCrypt at runtime.

INSERT INTO users (id, name, email, password_hash, role, created_at)
SELECT 1, 'Admin', 'admin@sherwinmart.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoxfnvVv8pfdtb2SmnKh', 'ADMIN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO users (id, name, email, password_hash, role, created_at)
SELECT 2, 'Sasha Seller', 'seller@sherwinmart.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoxfnvVv8pfdtb2SmnKh', 'SELLER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 2);

INSERT INTO users (id, name, email, password_hash, role, created_at)
SELECT 3, 'Bailey Buyer', 'buyer@sherwinmart.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoxfnvVv8pfdtb2SmnKh', 'BUYER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 3);

ALTER TABLE users ALTER COLUMN id RESTART WITH 4;

INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url, created_at)
SELECT 1, 2, 'Wireless Mouse', 'Ergonomic 2.4GHz wireless mouse', 799.00, 50, 'Electronics', 'https://example.com/img/mouse.jpg', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 1);

INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url, created_at)
SELECT 2, 2, 'Mechanical Keyboard', 'RGB backlit mechanical keyboard, blue switches', 2999.00, 25, 'Electronics', 'https://example.com/img/keyboard.jpg', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 2);

INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url, created_at)
SELECT 3, 2, 'Cotton T-Shirt', 'Plain crew-neck cotton t-shirt', 399.00, 100, 'Apparel', 'https://example.com/img/tshirt.jpg', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 3);

INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url, created_at)
SELECT 4, 2, 'Steel Water Bottle', '1-litre insulated steel bottle', 599.00, 60, 'Home', 'https://example.com/img/bottle.jpg', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 4);

ALTER TABLE products ALTER COLUMN id RESTART WITH 5;