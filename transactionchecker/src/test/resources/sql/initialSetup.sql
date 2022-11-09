-- =====
-- BLOCK
-- =====
CREATE TABLE IF NOT EXISTS block (
  number BIGINT      PRIMARY KEY NOT NULL,
  hash   VARCHAR(64)             NOT NULL
);

CREATE UNIQUE INDEX idx1_block ON block(hash);

-- ===========
-- TRANSACTION
-- ===========
CREATE TABLE IF NOT EXISTS transaction (
  block_number BIGINT      NOT NULL,
  number       BIGINT      NOT NULL,
  txid         VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX idx1_transaction ON transaction(txid);
CREATE UNIQUE INDEX idx2_transaction ON transaction(block_number, number);

-- =======
-- ADDRESS
-- =======
CREATE TABLE IF NOT EXISTS address (
  number BIGINT PRIMARY KEY NOT NULL,
  address       VARCHAR(64) NOT NULL,
  hex           VARCHAR(68) NOT NULL
);

CREATE UNIQUE INDEX idx1_address ON address(address);

-- =======================
-- ADDRESS_TRANSACTION_OUT
-- =======================
CREATE TABLE IF NOT EXISTS address_transaction_out (
  block_number       BIGINT        NOT NULL,
  transaction_number BIGINT        NOT NULL,
  vout_number        BIGINT        NOT NULL,
  address_number     BIGINT        NOT NULL,
  vout               DECIMAL(20,8) NOT NULL,
  type               VARCHAR(20)   NULL
);

CREATE UNIQUE INDEX idx1_address_transaction_out ON address_transaction_out(block_number, transaction_number, vout_number);

CREATE INDEX idx2_address_transaction_out ON address_transaction_out(block_number, transaction_number);
CREATE INDEX idx3_address_transaction_out ON address_transaction_out(address_number);

-- ======================
-- ADDRESS_TRANSACTION_IN
-- ======================
CREATE TABLE IF NOT EXISTS address_transaction_in (
  block_number          BIGINT        NOT NULL,
  transaction_number    BIGINT        NOT NULL,
  vin_number            BIGINT        NOT NULL,
  address_number        BIGINT        NOT NULL,
  in_block_number       BIGINT        NOT NULL,
  in_transaction_number BIGINT        NOT NULL,
  vin                   DECIMAL(20,8) NOT NULL
);

CREATE UNIQUE INDEX idx1_address_transaction_in ON address_transaction_in(block_number, transaction_number, vin_number);

CREATE INDEX idx2_address_transaction_in ON address_transaction_in(block_number, transaction_number);
CREATE INDEX idx3_address_transaction_in ON address_transaction_in(address_number);

-- ===============
-- STAKING_ADDRESS
-- ===============
CREATE TABLE IF NOT EXISTS staking_address (
  liquidity_address_number BIGINT NOT NULL,
  reward_address_number    BIGINT NOT NULL DEFAULT -1,
  start_block_number       BIGINT NOT NULL,
  start_transaction_number BIGINT NOT NULL,
  change_time              TIMESTAMP WITH TIME ZONE
                           GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_staking_address ON staking_address(liquidity_address_number, reward_address_number);

-- =======
-- DEPOSIT
-- =======
CREATE TABLE IF NOT EXISTS deposit (
  liquidity_address_number BIGINT NOT NULL,
  deposit_address_number   BIGINT NOT NULL,
  customer_address_number  BIGINT NOT NULL,
  start_block_number       BIGINT NOT NULL,
  start_transaction_number BIGINT NOT NULL,
  change_time              TIMESTAMP WITH TIME ZONE
                           GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_deposit ON deposit(liquidity_address_number, customer_address_number, deposit_address_number);

-- =======
-- BALANCE
-- =======
CREATE TABLE IF NOT EXISTS balance (
  address_number    BIGINT        NOT NULL,
  block_number      BIGINT        NOT NULL,
  transaction_count BIGINT        NOT NULL,
  vout              DECIMAL(20,8) NOT NULL,
  vin               DECIMAL(20,8) NOT NULL,
  change_time       TIMESTAMP WITH TIME ZONE
                    GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_balance ON balance(address_number);

-- =======
-- STAKING
-- =======
CREATE TABLE IF NOT EXISTS staking (
  liquidity_address_number BIGINT        NOT NULL,
  deposit_address_number   BIGINT        NOT NULL,
  customer_address_number  BIGINT        NOT NULL,
  last_in_block_number     BIGINT        NOT NULL,
  vin                      DECIMAL(20,8) NOT NULL,
  last_out_block_number    BIGINT        NOT NULL,
  vout                     DECIMAL(20,8) NOT NULL,
  change_time              TIMESTAMP WITH TIME ZONE
                           GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_staking ON staking(liquidity_address_number, deposit_address_number, customer_address_number);

-- ===========================
-- STAKING_WITHDRAWAL_RESERVED
-- ===========================
CREATE TABLE IF NOT EXISTS staking_withdrawal_reserved (
  withdrawal_id    BIGINT      NOT NULL,
  transaction_id   VARCHAR(64) NOT NULL,
  customer_address VARCHAR(64) NOT NULL,
  vout             DECIMAL(20,8) NOT NULL,
  create_time      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_time      TIMESTAMP WITH TIME ZONE
                   GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_staking_withdrawal_reserved ON staking_withdrawal_reserved(withdrawal_id);
CREATE UNIQUE INDEX idx2_staking_withdrawal_reserved ON staking_withdrawal_reserved(transaction_id);

-- ===================
-- API_DUPLICATE_CHECK
-- ===================
CREATE TABLE IF NOT EXISTS api_duplicate_check (
  withdrawal_id  BIGINT      NOT NULL,
  transaction_id VARCHAR(64) NOT NULL,
  change_time    TIMESTAMP WITH TIME ZONE
                 GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_api_duplicate_check ON api_duplicate_check(withdrawal_id);
CREATE UNIQUE INDEX idx2_api_duplicate_check ON api_duplicate_check(transaction_id);

-- ====================
-- MASTERNODE_WHITELIST
-- ====================
CREATE TABLE IF NOT EXISTS masternode_whitelist (
  wallet_id             BIGINT      NOT NULL,
  idx                   BIGINT      NOT NULL,
  owner_address         VARCHAR(64) NOT NULL,
  txid                  VARCHAR(64) NULL,
  operator_address      VARCHAR(64) NULL,
  reward_address        VARCHAR(64) NULL,
  creation_block_number BIGINT      NOT NULL DEFAULT -1,
  resign_block_number   BIGINT      NOT NULL DEFAULT -1,
  state                 VARCHAR(20) NULL,
  create_time           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_time           TIMESTAMP WITH TIME ZONE
                        GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_masternode_whitelist ON masternode_whitelist(wallet_id, idx, owner_address);
CREATE UNIQUE INDEX idx2_masternode_whitelist ON masternode_whitelist(owner_address);
