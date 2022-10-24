package ch.dfx.common.enumeration;

/**
 * 
 */
public enum PropertyEnum {
  // General ...
  RAW_TRANSACTION_MAX_SIZE,

  // DefiChain ...
  DFI_RPC_USERNAME,
  DFI_RPC_PASSWORD,

  DFI_WALLET_NAME,
  DFI_WALLET_PASSWORD,
  DFI_WALLET_SIGN_ADDRESS,
  DFI_VERIFY_ADDRESS,

  DFI_URL,

  // H2 Database ...
  H2_DB_DIR,
  H2_DB_NAME,
  H2_USERNAME,
  H2_PASSWORD,

  H2_SYNC_LOOP,
  H2_SYNC_COMMIT,

  H2_SERVER_TCP_HOST,
  H2_SERVER_TCP_PORT,

  // LOCK API ...
  LOCK_API_URL,
  LOCK_ADDRESS,
  LOCK_SIGNATURE,
  LOCK_API_TEST_TOKEN,

  // Run Period f�r Scheduler ...
  RUN_PERIOD_DATABASE,
  RUN_PERIOD_API;
}
