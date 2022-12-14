CREATE SCHEMA IF NOT EXISTS testnet AUTHORIZATION SA;
CREATE SCHEMA IF NOT EXISTS testnet_custom AUTHORIZATION SA;

-- ============================================================================
-- SCHEMA: PUBLIC
-- ============================================================================

-- ============
-- PUBLIC.BLOCK
-- ============
CREATE TABLE IF NOT EXISTS block (
  number BIGINT      PRIMARY KEY NOT NULL,
  hash   VARCHAR(64)             NOT NULL
);

CREATE UNIQUE INDEX idx1_block ON block(hash);

-- ==================
-- PUBLIC.TRANSACTION
-- ==================
CREATE TABLE IF NOT EXISTS transaction (
  block_number     BIGINT      NOT NULL,
  number           BIGINT      NOT NULL,
  txid             VARCHAR(64) NOT NULL,
  custom_type_code CHAR(1)     NOT NULL
);

CREATE UNIQUE INDEX idx1_transaction ON transaction(txid);
CREATE UNIQUE INDEX idx2_transaction ON transaction(block_number, number);

-- ==============
-- PUBLIC.ADDRESS
-- ==============
CREATE TABLE IF NOT EXISTS address (
  number BIGINT PRIMARY KEY NOT NULL,
  address       VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX idx1_address ON address(address);

-- ==============================
-- PUBLIC.ADDRESS_TRANSACTION_OUT
-- ==============================
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

-- =============================
-- PUBLIC.ADDRESS_TRANSACTION_IN
-- =============================
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

-- ============================================================================
-- SCHEMA: TESTNET
-- ============================================================================

-- =======================
-- TESTNET.STAKING_ADDRESS
-- =======================
CREATE TABLE IF NOT EXISTS testnet.staking_address (
  token_number             INT    NOT NULL,
  liquidity_address_number BIGINT NOT NULL,
  reward_address_number    BIGINT NOT NULL DEFAULT -1,
  start_block_number       BIGINT NOT NULL,
  start_transaction_number BIGINT NOT NULL,
  change_time              TIMESTAMP WITH TIME ZONE
                           GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_staking_address ON testnet.staking_address(token_number, liquidity_address_number, reward_address_number);
CREATE UNIQUE INDEX idx2_staking_address ON testnet.staking_address(liquidity_address_number, reward_address_number);
CREATE INDEX idx3_staking_address ON testnet.staking_address(token_number);

-- ===============
-- TESTNET.DEPOSIT
-- ===============
CREATE TABLE IF NOT EXISTS testnet.deposit (
  token_number             INT    NOT NULL,
  liquidity_address_number BIGINT NOT NULL,
  deposit_address_number   BIGINT NOT NULL,
  customer_address_number  BIGINT NOT NULL,
  start_block_number       BIGINT NOT NULL,
  start_transaction_number BIGINT NOT NULL,
  change_time              TIMESTAMP WITH TIME ZONE
                           GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_deposit ON testnet.deposit(token_number, liquidity_address_number, customer_address_number, deposit_address_number);
CREATE UNIQUE INDEX idx2_deposit ON testnet.deposit(liquidity_address_number, customer_address_number, deposit_address_number);
CREATE INDEX idx3_deposit ON testnet.deposit(token_number);

-- ===============
-- TESTNET.BALANCE
-- ===============
CREATE TABLE IF NOT EXISTS testnet.balance (
  token_number      INT           NOT NULL,
  address_number    BIGINT        NOT NULL,
  block_number      BIGINT        NOT NULL,
  transaction_count BIGINT        NOT NULL,
  vout              DECIMAL(20,8) NOT NULL,
  vin               DECIMAL(20,8) NOT NULL,
  change_time       TIMESTAMP WITH TIME ZONE
                    GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_balance ON testnet.balance(token_number, address_number);
CREATE INDEX idx2_balance ON testnet.balance(token_number);

-- ===============
-- TESTNET.STAKING
-- ===============
CREATE TABLE IF NOT EXISTS testnet.staking (
  token_number             INT           NOT NULL,
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

CREATE UNIQUE INDEX idx1_staking ON testnet.staking(token_number, liquidity_address_number, deposit_address_number, customer_address_number);
CREATE UNIQUE INDEX idx2_staking ON testnet.staking(liquidity_address_number, deposit_address_number, customer_address_number);
CREATE INDEX idx3_staking ON testnet.staking(token_number);

-- ===================================
-- TESTNET.STAKING_WITHDRAWAL_RESERVED
-- ===================================
CREATE TABLE IF NOT EXISTS testnet.staking_withdrawal_reserved (
  token_number     INT         NOT NULL,
  withdrawal_id    BIGINT      NOT NULL,
  transaction_id   VARCHAR(64) NOT NULL,
  customer_address VARCHAR(64) NOT NULL,
  vout             DECIMAL(20,8) NOT NULL,
  create_time      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_time      TIMESTAMP WITH TIME ZONE
                   GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_staking_withdrawal_reserved ON testnet.staking_withdrawal_reserved(withdrawal_id);
CREATE UNIQUE INDEX idx2_staking_withdrawal_reserved ON testnet.staking_withdrawal_reserved(transaction_id);
CREATE INDEX idx3_staking_withdrawal_reserved ON testnet.staking_withdrawal_reserved(token_number);

-- ===========================
-- TESTNET.API_DUPLICATE_CHECK
-- ===========================
CREATE TABLE IF NOT EXISTS testnet.api_duplicate_check (
  withdrawal_id  BIGINT      NOT NULL,
  transaction_id VARCHAR(64) NOT NULL,
  change_time    TIMESTAMP WITH TIME ZONE
                 GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_api_duplicate_check ON testnet.api_duplicate_check(withdrawal_id);
CREATE UNIQUE INDEX idx2_api_duplicate_check ON testnet.api_duplicate_check(transaction_id);

-- ============================
-- TESTNET.MASTERNODE_WHITELIST
-- ============================
CREATE TABLE IF NOT EXISTS testnet.masternode_whitelist (
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

CREATE UNIQUE INDEX idx1_masternode_whitelist ON testnet.masternode_whitelist(wallet_id, idx, owner_address);
CREATE UNIQUE INDEX idx2_masternode_whitelist ON testnet.masternode_whitelist(owner_address);

-- ============================================================================
-- SCHEMA: TESTNET_CUSTOM
-- ============================================================================

-- ====================
-- TESTNET_CUSTOM.TOKEN
-- ====================
CREATE TABLE IF NOT EXISTS testnet_custom.token (
  number      INT         NOT NULL,
  name        VARCHAR(20) NOT NULL,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_time TIMESTAMP WITH TIME ZONE
              GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_token ON testnet_custom.token(number);

-- ===================
-- TESTNET_CUSTOM.TYPE
-- ===================
CREATE TABLE IF NOT EXISTS testnet_custom.type (
  number      INT         NOT NULL,
  type_code   CHAR(1)     NOT NULL,
  name        VARCHAR(30) NOT NULL,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_time TIMESTAMP WITH TIME ZONE
              GENERATED ALWAYS AS CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx1_type ON testnet_custom.type(number);

-- ====================================
-- TESTNET_CUSTOM.ACCOUNT_TO_ACCOUNT_IN
-- ====================================
CREATE TABLE IF NOT EXISTS testnet_custom.account_to_account_in (
  block_number        BIGINT        NOT NULL,
  transaction_number  BIGINT        NOT NULL,
  type_number         INT           NOT NULL,
  address_number      BIGINT        NOT NULL,
  amount              DECIMAL(20,8) NOT NULL,
  token_number        INT           NOT NULL
);

CREATE UNIQUE INDEX idx1_account_to_account_in ON testnet_custom.account_to_account_in(block_number, transaction_number, type_number, address_number);

CREATE INDEX idx2_account_to_account_in ON testnet_custom.account_to_account_in(block_number, transaction_number);
CREATE INDEX idx3_account_to_account_in ON testnet_custom.account_to_account_in(address_number);

-- =====================================
-- TESTNET_CUSTOM.ACCOUNT_TO_ACCOUNT_OUT
-- =====================================
CREATE TABLE IF NOT EXISTS testnet_custom.account_to_account_out (
  block_number        BIGINT        NOT NULL,
  transaction_number  BIGINT        NOT NULL,
  type_number         INT           NOT NULL,
  address_number      BIGINT        NOT NULL,
  amount              DECIMAL(20,8) NOT NULL,
  token_number        INT           NOT NULL
);

CREATE UNIQUE INDEX idx1_account_to_account_out ON testnet_custom.account_to_account_out(block_number, transaction_number, type_number, address_number);

CREATE INDEX idx2_account_to_account_out ON testnet_custom.account_to_account_out(block_number, transaction_number);
CREATE INDEX idx3_account_to_account_out ON testnet_custom.account_to_account_out(address_number);
