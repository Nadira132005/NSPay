-- Template executed by scripts/setup-db.py. Defaults: payments / payadmin / paypassword.
-- All application tables and demo rows belong to the selected database.
-- EXIT is a mysql-client command and is intentionally omitted from this SQL file.
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,NO_BACKSLASH_ESCAPES';
CREATE DATABASE IF NOT EXISTS {{DATABASE}} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS {{USERNAME}}@{{USER_HOST}} IDENTIFIED BY {{PASSWORD}};
ALTER USER {{USERNAME}}@{{USER_HOST}} IDENTIFIED BY {{PASSWORD}};
GRANT ALL PRIVILEGES ON {{GRANT_DATABASE}}.* TO {{USERNAME}}@{{USER_HOST}};
USE {{DATABASE}};

-- Names and types match the JPA entities and PhysicalNamingStrategyStandardImpl.
CREATE TABLE IF NOT EXISTS ACCOUNTS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cardNumber VARCHAR(255) UNIQUE,
    iban VARCHAR(34) UNIQUE,
    cardHolderName VARCHAR(255),
    status VARCHAR(255),
    amount DOUBLE NOT NULL,
    dailyTxLimit INT NOT NULL,
    dailyTxSumLimit DOUBLE NOT NULL,
    dailyTx INT NOT NULL,
    dailyTxSum DOUBLE NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS USERS (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    password VARCHAR(255),
    accountId BIGINT,
    passwordChangeRequired BIT NOT NULL,
    role VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS TRANSACTIONS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cardNumber VARCHAR(255),
    amount DOUBLE,
    `timestamp` DATETIME(6),
    returnCode VARCHAR(255),
    transactionType VARCHAR(255),
    counterpartyIban VARCHAR(255), INDEX idx_transaction_history (cardNumber, `timestamp`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS AUDIT (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user` VARCHAR(255) NOT NULL,
    actiune VARCHAR(255) NOT NULL,
    `timestamp` DATETIME(6) NOT NULL
) ENGINE=InnoDB;

-- UserModel uses Hibernate's AUTO generator, which needs this sequence table.
CREATE TABLE IF NOT EXISTS hibernate_sequence (next_val BIGINT) ENGINE=InnoDB;

-- Fintech demo state: ownership, amounts, documents, and reviews are persisted here.
CREATE TABLE IF NOT EXISTS SAVINGS_POCKETS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    name VARCHAR(255), balance DECIMAL(19,2), targetAmount DECIMAL(19,2)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS SCHEDULED_TRANSFERS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    recipientIban VARCHAR(255), amount DECIMAL(19,2), scheduledAt DATETIME(6), status VARCHAR(255), failureReason VARCHAR(255), executedAt DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS TRADE_ORDERS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    symbol VARCHAR(255), side VARCHAR(255), quantity DECIMAL(19,6), price DECIMAL(19,6), marketPrice DECIMAL(19,6), exchangeRate DECIMAL(19,8), priceAt DATETIME(6), exchangeRateAt DATETIME(6), quoteId VARCHAR(255), total DECIMAL(19,2), status VARCHAR(255), provider VARCHAR(32) NOT NULL DEFAULT 'twelve-data', INDEX idx_trade_owner_time(userId,createdAt,id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS CREDIT_APPLICATIONS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    amount DECIMAL(19,2), monthlyIncome DECIMAL(19,2), termMonths INT NOT NULL, purpose VARCHAR(255), status VARCHAR(255), reviewNote VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS CREDIT_DOCUMENTS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    applicationId BIGINT, kind VARCHAR(255), filename VARCHAR(255), contentType VARCHAR(255), sha256 VARCHAR(255), content LONGBLOB, scanStatus VARCHAR(255), extractedText VARCHAR(4000)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS CUSTOMER_PROFILES (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    email VARCHAR(255), phone VARCHAR(255), country VARCHAR(255), dateOfBirth DATE, occupation VARCHAR(255), onboardingStatus VARCHAR(255),
    reviewedBy BIGINT, reviewedAt DATETIME(6), reviewNote VARCHAR(250), UNIQUE (userId)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS RISK_ASSESSMENTS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    amount DECIMAL(19,2), recipientIban VARCHAR(255), score INT NOT NULL, level VARCHAR(255), reasons VARCHAR(2000), modelVersion VARCHAR(255), historySamples INT NOT NULL, status VARCHAR(255)
) ENGINE=InnoDB;

-- Persisted account restrictions and decision history. Account status is authoritative.
CREATE TABLE IF NOT EXISTS ACCOUNT_RESTRICTIONS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL UNIQUE,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, suspensionCount INT NOT NULL,
 suspendedUntil DATETIME(6), reason VARCHAR(255), INDEX(suspendedUntil)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS SECURITY_REPORTS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, transactionId BIGINT NOT NULL UNIQUE,
 reporterId BIGINT NOT NULL, reason VARCHAR(255), status VARCHAR(255), decisionNote VARCHAR(255),
 reviewedBy BIGINT, reviewedAt DATETIME(6), publicNote VARCHAR(250), privateNote VARCHAR(250), INDEX(userId)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS STATUS_CHANGES (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, actorId BIGINT NOT NULL,
 previousStatus VARCHAR(255), newStatus VARCHAR(255), reason VARCHAR(255), suspendedUntil DATETIME(6),
 suspensionCount INT NOT NULL, publicNote VARCHAR(250), privateNote VARCHAR(250), misalignmentScore INT,
 transactionId BIGINT, INDEX(userId)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS PAYMENT_SIGNALS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL, createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL,
 transactionId BIGINT NOT NULL UNIQUE, username VARCHAR(255), recipientIban VARCHAR(255), transactionType VARCHAR(255), paidAt DATETIME(6), amount DECIMAL(19,2),
 amountScore INT NOT NULL, timingScore INT NOT NULL, velocityScore INT NOT NULL, recipientScore INT NOT NULL, reportingScore INT NOT NULL, misalignmentScore INT NOT NULL,
 limitScore INT NOT NULL DEFAULT 0, dailyTx INT NOT NULL DEFAULT 0, dailyTxSum DOUBLE NOT NULL DEFAULT 0, dailyTxLimit INT NOT NULL DEFAULT 0, dailyTxSumLimit DOUBLE NOT NULL DEFAULT 0,
 historySamples INT NOT NULL, reportCount INT NOT NULL, reported BOOLEAN NOT NULL, reviewStatus VARCHAR(255), reviewedBy BIGINT, reviewedAt DATETIME(6),
 publicNote VARCHAR(250), privateNote VARCHAR(250), modelVersion VARCHAR(255), decisionScore INT,
 INDEX idx_payment_score (misalignmentScore), INDEX idx_payment_review (reviewStatus,paidAt), INDEX idx_payment_owner (userId,reviewStatus,id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS INVESTMENT_WALLETS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL UNIQUE,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, balance DECIMAL(19,2) NOT NULL, positionsInitialized BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS INVESTMENT_COMMANDS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, requestKey VARCHAR(255),
 fingerprint VARCHAR(255), orderId BIGINT, UNIQUE(userId,requestKey)
) ENGINE=InnoDB;

-- Current holdings are an accounting projection; order history remains immutable.
CREATE TABLE IF NOT EXISTS INVESTMENT_POSITIONS (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL,
 provider VARCHAR(32) NOT NULL, symbol VARCHAR(16) NOT NULL,
 quantity DECIMAL(19,6) NOT NULL, costBasis DECIMAL(25,8) NOT NULL, realizedGain DECIMAL(25,8) NOT NULL,
 UNIQUE(userId,provider,symbol)
) ENGINE=InnoDB;
-- Shared short-lived quote IDs support backend replicas without sticky sessions.
CREATE TABLE IF NOT EXISTS MARKET_QUOTES (
 id VARCHAR(36) NOT NULL PRIMARY KEY, provider VARCHAR(32) NOT NULL, symbol VARCHAR(16) NOT NULL,
 expiresAt DATETIME(6) NOT NULL, payload LONGTEXT NOT NULL,
 INDEX idx_quote_lookup(provider,symbol,expiresAt), INDEX idx_quote_expiry(expiresAt)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS PORTFOLIO_PERFORMANCE (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, userId BIGINT NOT NULL,
 createdAt DATETIME(6) NOT NULL, version BIGINT NOT NULL, day DATE NOT NULL,
 totalGain DECIMAL(19,2) NOT NULL, realizedGain DECIMAL(19,2) NOT NULL,
 unrealizedGain DECIMAL(19,2) NOT NULL, holdingsValue DECIMAL(19,2) NOT NULL,
 walletVersion BIGINT NOT NULL, UNIQUE(userId,day)
) ENGINE=InnoDB;

-- Upgrade existing installations without changing balances or removing order history.
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='marketPrice'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN marketPrice DECIMAL(19,6)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='exchangeRate'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN exchangeRate DECIMAL(19,8)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='priceAt'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN priceAt DATETIME(6)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='exchangeRateAt'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN exchangeRateAt DATETIME(6)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='quoteId'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN quoteId VARCHAR(255)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND column_name='provider'), 'SELECT 1', 'ALTER TABLE TRADE_ORDERS ADD COLUMN provider VARCHAR(32) NOT NULL DEFAULT ''twelve-data''' );
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='INVESTMENT_WALLETS' AND column_name='positionsInitialized'), 'SELECT 1', 'ALTER TABLE INVESTMENT_WALLETS ADD COLUMN positionsInitialized BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @migration_sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='TRADE_ORDERS' AND index_name='idx_trade_owner_time'), 'SELECT 1', 'CREATE INDEX idx_trade_owner_time ON TRADE_ORDERS(userId,createdAt,id)');
PREPARE migration FROM @migration_sql; EXECUTE migration; DEALLOCATE PREPARE migration;
ALTER TABLE TRADE_ORDERS MODIFY COLUMN price DECIMAL(19,6);
UPDATE ACCOUNTS SET status='FREEZE' WHERE status='FROZEN';
UPDATE ACCOUNTS SET status='BLOCKED' WHERE status='CLOSED';
INSERT INTO ACCOUNT_RESTRICTIONS (userId, createdAt, version, suspensionCount, suspendedUntil, reason)
SELECT u.id, NOW(), 0, 1, NOW() + INTERVAL 30 DAY, 'Existing suspension migrated to a 30 day period'
FROM USERS u JOIN ACCOUNTS a ON a.id=u.accountId WHERE a.status='SUSPENDED'
ON DUPLICATE KEY UPDATE id=ACCOUNT_RESTRICTIONS.id;
UPDATE CREDIT_APPLICATIONS SET status='APPROVED' WHERE status='APPROVED_DEMO';

START TRANSACTION;
INSERT INTO hibernate_sequence (next_val)
SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM hibernate_sequence);

-- Reserved demo IDs make repeat imports preserve edited rows and balances.
-- Card numbers pass Luhn; Romanian-format IBANs have valid mod-97 check digits.
INSERT INTO ACCOUNTS
    (id, cardNumber, iban, cardHolderName, status, amount, dailyTxLimit, dailyTxSumLimit, dailyTx, dailyTxSum)
VALUES
    (900001, '4111111111111111', 'RO38NSPY0000000000000001', 'NSPay Administrator', 'ACTIVE', 10000, 100, 20000, 0, 0),
    (900002, '5555555555554444', 'RO11NSPY0000000000000002', 'Alice Popescu', 'ACTIVE', 4250, 20, 5000, 0, 0),
    (900003, '4000000000000002', 'RO81NSPY0000000000000003', 'Bob Ionescu', 'ACTIVE', 3000, 20, 5000, 0, 0),
    (900004, '6011111111111117', 'RO54NSPY0000000000000004', 'NSPay Analyst', 'ACTIVE', 2000, 20, 5000, 0, 0),
    (900005, '3530111333300000', 'RO27NSPY0000000000000005', 'New User', 'ACTIVE', 1500, 10, 2000, 0, 0),
    (900006, '4012888888881881', 'RO97NSPY0000000000000006', 'Blocked Account', 'BLOCKED', 800, 10, 2000, 0, 0)
ON DUPLICATE KEY UPDATE id = id;

-- DemoPay123! is BCrypt encoded using the application's BCryptPasswordEncoder.
INSERT INTO USERS (id, name, firstName, lastName, password, accountId, passwordChangeRequired, role)
VALUES
    (900001, 'admin', 'NSPay', 'Administrator', '$2a$10$yIBZKu/dKqrIzUJmAYzZmuhyeHYUFgKdvCQHKWmrrpyCGEfskria.', 900001, 0, 'Admin'),
    (900002, 'alice', 'Alice', 'Popescu', '$2a$10$yIBZKu/dKqrIzUJmAYzZmuhyeHYUFgKdvCQHKWmrrpyCGEfskria.', 900002, 0, 'User'),
    (900003, 'bob', 'Bob', 'Ionescu', '$2a$10$yIBZKu/dKqrIzUJmAYzZmuhyeHYUFgKdvCQHKWmrrpyCGEfskria.', 900003, 0, 'User'),
    (900004, 'analyst', 'NSPay', 'Analyst', '$2a$10$yIBZKu/dKqrIzUJmAYzZmuhyeHYUFgKdvCQHKWmrrpyCGEfskria.', 900004, 0, 'Analyst'),
    (900005, 'newuser', 'New', 'User', '$2a$10$yIBZKu/dKqrIzUJmAYzZmuhyeHYUFgKdvCQHKWmrrpyCGEfskria.', 900005, 1, 'User')
ON DUPLICATE KEY UPDATE id = id;

UPDATE hibernate_sequence
SET next_val = GREATEST(next_val, (SELECT COALESCE(MAX(id), 0) + 100 FROM USERS));

INSERT INTO TRANSACTIONS (id, cardNumber, amount, `timestamp`, returnCode, transactionType, counterpartyIban)
VALUES
    (900001, '5555555555554444', 250, NOW() - INTERVAL 1 DAY, '00', 'TRANSFER_OUT', 'RO81NSPY0000000000000003'),
    (900002, '4000000000000002', 250, NOW() - INTERVAL 1 DAY, '00', 'TRANSFER_IN', 'RO11NSPY0000000000000002'),
    (900003, '4111111111111111', 45.50, NOW() - INTERVAL 2 DAY, '00', 'PAYMENT', NULL),
    (900004, '5555555555554444', 89.90, NOW() - INTERVAL 3 DAY, '00', 'PAYMENT', NULL),
    (900005, '4000000000000002', 50000, NOW() - INTERVAL 4 DAY, '51', 'PAYMENT', NULL),
    (900006, '4012888888881881', 25, NOW() - INTERVAL 5 DAY, '13', 'PAYMENT', NULL)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO AUDIT (id, `user`, actiune, `timestamp`)
VALUES
    (900001, 'admin', 'INITIAL_DATA_CREATED', NOW() - INTERVAL 6 DAY),
    (900002, 'alice', 'LOGIN_SUCCESS', NOW() - INTERVAL 1 DAY),
    (900003, 'alice', 'ACTION: IBAN transfer to Bob', NOW() - INTERVAL 1 DAY),
    (900004, 'bob', 'LOGIN_FAILED', NOW() - INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE id = id;
INSERT INTO SAVINGS_POCKETS (id, userId, createdAt, version, name, balance, targetAmount)
VALUES (900001, 900002, NOW(), 0, 'A weekend away', 500, 1500),
       (900002, 900003, NOW(), 0, 'Rainy-day fund', 250, 1000)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO CUSTOMER_PROFILES (id, userId, createdAt, version, email, phone, country, dateOfBirth, occupation, onboardingStatus)
VALUES (900001, 900002, NOW(), 0, 'alice@example.test', '+40000000000', 'Romania', '1995-05-12', 'Designer', 'PROFILE_COMPLETE')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO CREDIT_APPLICATIONS (id, userId, createdAt, version, amount, monthlyIncome, termMonths, purpose, status)
VALUES (900001, 900002, NOW(), 0, 3000, 1500, 12, 'Home office upgrade', 'DRAFT')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO TRANSACTIONS (id, cardNumber, amount, `timestamp`, returnCode, transactionType)
VALUES (900010, '5555555555554444', 500, NOW() - INTERVAL 2 DAY, '00', 'SAVINGS_OUT'),
       (900011, '4000000000000002', 250, NOW() - INTERVAL 2 DAY, '00', 'SAVINGS_OUT')
ON DUPLICATE KEY UPDATE id = id;
COMMIT;
