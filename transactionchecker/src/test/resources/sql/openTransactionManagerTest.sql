-- ============================================================================
-- SCHEMA: PUBLIC
-- ============================================================================

-- ==============
-- PUBLIC.ADDRESS
-- ==============
INSERT INTO public.address (number, address)
VALUES (1, 'tf1qqyy88gj7pvf5rn8s62myjw9pd45q865e3ty8fr');

INSERT INTO public.address (number, address)
VALUES (2, 'tf1qe3qedn8zfhysdatxlh77m9me7crsj6k72ah3zp');

INSERT INTO public.address (number, address)
VALUES (3, 'tf1qwufuhfrkyhprnsylxcj566lax7ntnza305sna7');

INSERT INTO public.address (number, address)
VALUES (4, 'df1qgz2xyzqwnsn5979syu6ng9wxlc4c2ac98m377f');

-- Yieldmachine: Liquidity
INSERT INTO public.address (number, address)
VALUES (11, 'df1qasdh79grpyd2wltpamznqp2xdhq6kmkevafxgq');

-- Yieldmachine: Deposit / Customer
INSERT INTO public.address (number, address)
VALUES (12, 'df1q5r9u6px6j04a97y56qfkn2wrjct3yteynlegrg');

INSERT INTO public.address (number, address)
VALUES (13, 'df1qek5mfpxxdz922rd0zpvkue4qtxj6r0qhdt6v7s');

-- Yieldmachine: Deposit / Customer
INSERT INTO public.address (number, address)
VALUES (14, 'df1qa06vpd0ts58223dchtet6e089m4czf4k604drn');


-- ============================================================================
-- SCHEMA: TESTNET
-- ============================================================================

-- ============================
-- TESTNET.MASTERNODE_WHITELIST
-- ============================
INSERT INTO testnet.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 1, 'tf1q5mypk8mlcfxqlkh2ntnggk7e0v36e74davrr3z');

INSERT INTO testnet.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 2, 'tf1qqkzs9mrlwq0qy0dw74wpvyuup0gwhak7jppxcv');

INSERT INTO testnet.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 3, 'tf1q8n5rly23k60eluu85r7yytya563r949nrnkkmf');

-- =======================
-- TESTNET.VAULT_WHITELIST
-- =======================
INSERT INTO testnet.vault_whitelist (id, address, min_ratio, max_ratio, state)
VALUES ('28e90bd7cb840b3988a81c8d855b3ad52739bde70bed3a7554267d25fbfda507', 'tf1q8n5rly23k60eluu85r7yytya563r949nrnkkmf', 169.94, 500.00, 'ACTIVE');

INSERT INTO testnet.vault_whitelist (id, address, min_ratio, max_ratio, state)
VALUES ('b96404aed18e96276ff4e36eb79232c64b704568e6487deb7a6b1b0d4ff61a97', 'tf1q8n5rly23k60eluu85r7yytya563r949nrnkkmf', 169.94, 500.00, 'ACTIVE');

-- ============================================================================
-- SCHEMA: TESTNET_STAKING
-- ============================================================================

-- ===============================
-- TESTNET_STAKING.STAKING_ADDRESS
-- ===============================
INSERT INTO testnet_staking.staking_address (token_number, liquidity_address_number, start_block_number, start_transaction_number)
VALUES (0, 1, 0, 0);

INSERT INTO testnet_yieldmachine.staking_address (token_number, liquidity_address_number, start_block_number, start_transaction_number)
VALUES (0, 11, 0, 0);

-- =======================
-- TESTNET_STAKING.STAKING
-- =======================
INSERT INTO testnet_staking.staking (token_number, liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout)
VALUES (0, 1, 2, 3, 0, 500.00000000, 0, 100.00000000);

INSERT INTO testnet_yieldmachine.staking (token_number, liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout)
VALUES (0, 11, 12, 13, 0, 1000.00000000, 0, 100.00000000);

INSERT INTO testnet_yieldmachine.staking (token_number, liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout)
VALUES (11, 11, 14, 4, 0, 500.00000000, 0, 0.00000000);
