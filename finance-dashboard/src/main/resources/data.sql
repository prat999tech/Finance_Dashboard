-- ─────────────────────────────────────────────────────────────────────────────
-- Seed data for dev profile (H2)
-- All passwords are: "password123" (BCrypt hashed)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO users (name, email, password, role, status, created_at, updated_at) VALUES
('Alice Admin',   'admin@finance.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN',   'ACTIVE', NOW(), NOW()),
('Alan Analyst',  'analyst@finance.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ANALYST', 'ACTIVE', NOW(), NOW()),
('Vera Viewer',   'viewer@finance.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'VIEWER',  'ACTIVE', NOW(), NOW()),
('Ivan Inactive', 'inactive@finance.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'VIEWER',  'INACTIVE', NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- Sample transactions (user_id = 1 = Alice Admin)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO transactions (amount, type, category, date, notes, user_id, created_at, updated_at) VALUES
-- INCOME
(5000.00, 'INCOME',  'Salary',      '2025-01-05', 'January salary',          1, NOW(), NOW()),
(5000.00, 'INCOME',  'Salary',      '2025-02-05', 'February salary',         1, NOW(), NOW()),
(5000.00, 'INCOME',  'Salary',      '2025-03-05', 'March salary',            1, NOW(), NOW()),
(5000.00, 'INCOME',  'Salary',      '2025-04-05', 'April salary',            1, NOW(), NOW()),
(5000.00, 'INCOME',  'Salary',      '2025-05-05', 'May salary',              1, NOW(), NOW()),
(5000.00, 'INCOME',  'Salary',      '2025-06-05', 'June salary',             1, NOW(), NOW()),
(1200.00, 'INCOME',  'Freelance',   '2025-01-15', 'Website project',         1, NOW(), NOW()),
(800.00,  'INCOME',  'Freelance',   '2025-03-20', 'Logo design work',        1, NOW(), NOW()),
(300.00,  'INCOME',  'Investments', '2025-02-28', 'Dividend payment',        1, NOW(), NOW()),
(450.00,  'INCOME',  'Investments', '2025-05-31', 'Quarterly dividend',      1, NOW(), NOW()),
(150.00,  'INCOME',  'Other',       '2025-04-10', 'Cashback rewards',        1, NOW(), NOW()),

-- EXPENSE
(1200.00, 'EXPENSE', 'Rent',        '2025-01-01', 'January rent',            1, NOW(), NOW()),
(1200.00, 'EXPENSE', 'Rent',        '2025-02-01', 'February rent',           1, NOW(), NOW()),
(1200.00, 'EXPENSE', 'Rent',        '2025-03-01', 'March rent',              1, NOW(), NOW()),
(1200.00, 'EXPENSE', 'Rent',        '2025-04-01', 'April rent',              1, NOW(), NOW()),
(1200.00, 'EXPENSE', 'Rent',        '2025-05-01', 'May rent',                1, NOW(), NOW()),
(1200.00, 'EXPENSE', 'Rent',        '2025-06-01', 'June rent',               1, NOW(), NOW()),
(350.00,  'EXPENSE', 'Food',        '2025-01-20', 'Monthly groceries',       1, NOW(), NOW()),
(280.00,  'EXPENSE', 'Food',        '2025-02-18', 'Groceries + dining',      1, NOW(), NOW()),
(410.00,  'EXPENSE', 'Food',        '2025-03-22', 'Groceries + meal preps',  1, NOW(), NOW()),
(195.00,  'EXPENSE', 'Transport',   '2025-01-31', 'Fuel and metro pass',     1, NOW(), NOW()),
(210.00,  'EXPENSE', 'Transport',   '2025-03-31', 'Fuel',                    1, NOW(), NOW()),
(99.00,   'EXPENSE', 'Utilities',   '2025-01-28', 'Electric + internet',     1, NOW(), NOW()),
(105.00,  'EXPENSE', 'Utilities',   '2025-02-28', 'Electric + internet',     1, NOW(), NOW()),
(110.00,  'EXPENSE', 'Utilities',   '2025-03-28', 'Electric + internet',     1, NOW(), NOW()),
(500.00,  'EXPENSE', 'Healthcare',  '2025-02-10', 'Dental check-up',         1, NOW(), NOW()),
(75.00,   'EXPENSE', 'Healthcare',  '2025-04-15', 'Pharmacy',                1, NOW(), NOW()),
(650.00,  'EXPENSE', 'Shopping',    '2025-03-15', 'New laptop accessories',  1, NOW(), NOW()),
(120.00,  'EXPENSE', 'Shopping',    '2025-05-20', 'Clothing',                1, NOW(), NOW()),
(45.00,   'EXPENSE', 'Entertainment','2025-01-25','Streaming subscriptions', 1, NOW(), NOW()),
(45.00,   'EXPENSE', 'Entertainment','2025-02-25','Streaming subscriptions', 1, NOW(), NOW()),
(45.00,   'EXPENSE', 'Entertainment','2025-03-25','Streaming subscriptions', 1, NOW(), NOW()),
(45.00,   'EXPENSE', 'Entertainment','2025-04-25','Streaming subscriptions', 1, NOW(), NOW()),
(45.00,   'EXPENSE', 'Entertainment','2025-05-25','Streaming subscriptions', 1, NOW(), NOW());
