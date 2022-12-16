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

-- ============================================================================
-- SCHEMA: TESTNET
-- ============================================================================

-- =======================
-- TESTNET.STAKING_ADDRESS
-- =======================
INSERT INTO testnet.staking_address (token_number, liquidity_address_number, start_block_number, start_transaction_number)
VALUES (0, 1, 0, 0);

-- ===============
-- TESTNET.STAKING
-- ===============
INSERT INTO testnet.staking (token_number, liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout)
VALUES (0, 1, 2, 3, 0, 500.00000000, 0, 100.00000000);

-- ==================
-- TESTNET.MASTERNODE
-- ==================
INSERT INTO testnet.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 1, 'tf1q5mypk8mlcfxqlkh2ntnggk7e0v36e74davrr3z');

INSERT INTO testnet.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 2, 'tf1qqkzs9mrlwq0qy0dw74wpvyuup0gwhak7jppxcv');
