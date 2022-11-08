-- =======
-- ADDRESS
-- =======
INSERT INTO public.address (number, address, hex)
VALUES (36031, 'tf1qqyy88gj7pvf5rn8s62myjw9pd45q865e3ty8fr', '0014010873a25e0b1341ccf0d2b64938a16d6803ea99');

INSERT INTO public.address (number, address, hex)
VALUES (36029, 'tf1qe3qedn8zfhysdatxlh77m9me7crsj6k72ah3zp', '0014cc4196cce24dc906f566fdfded9779f607096ade');

INSERT INTO public.address (number, address, hex)
VALUES (17831, 'tf1qwufuhfrkyhprnsylxcj566lax7ntnza305sna7', '00147713cba47625c239c09f36254d6bfd37a6b98bb1');

-- ===============
-- STAKING_ADDRESS
-- ===============
INSERT INTO public.staking_address (liquidity_address_number, start_block_number, start_transaction_number)
VALUES (36031, 0, 0);

-- =======
-- STAKING
-- =======
INSERT INTO public.staking (liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout)
VALUES (36031, 36029, 17831, 1328792, 100058.00000000, 1327289, 10008.00000000);

-- ==========
-- MASTERNODE
-- ==========
INSERT INTO public.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 1, 'tf1q5mypk8mlcfxqlkh2ntnggk7e0v36e74davrr3z');

INSERT INTO public.masternode_whitelist (wallet_id, idx, owner_address)
VALUES (1, 2, 'tf1qqkzs9mrlwq0qy0dw74wpvyuup0gwhak7jppxcv');
