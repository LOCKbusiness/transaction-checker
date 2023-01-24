package ch.dfx.common.enumeration;

/**
 * 
 */
public enum PropertyEnum {
  // Server ID ...
  SERVER_ID,

  // E-Mail ...
  EMAIL_USER,
  EMAIL_PASSWORD,
  EMAIL_FROM,
  EMAIL_TO,

  // Telegram ...
  TELEGRAM_TOKEN,
  TELEGRAM_CHAT_ID,

  // General ...
  RAW_TRANSACTION_MAX_SIZE,

  // RMI ...
  RMI_HOST,
  RMI_PORT,

  // Watchdog ...
  WATCHDOG_EXECUTABLE,
  DEFICHAIN_WATCHDOG_EXECUTABLE,

  // DefiChain ...
  DFI_URL,

  DFI_RPC_USERNAME,
  DFI_RPC_PASSWORD,

  DFI_WALLET_NAME,
  DFI_WALLET_PASSWORD,

  DFI_WALLET_SIGN_ADDRESS,
  DFI_VERIFY_ADDRESS,

  DFI_YM_LIQUIDITY_ADDRESS,
  DFI_YM_VAULT_ADDRESS,
  DFI_YM_VAULT_ID,
  DFI_YM_VAULT_CHECK_RATIO,

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
  RUN_PERIOD_MESSAGE_EVENT,
  RUN_PERIOD_WATCHDOG,
  RUN_PERIOD_DATABASE,
  RUN_PERIOD_API,
  RUN_PERIOD_REPORT,
  RUN_PERIOD_DEFIMANAGER,

  // Reporting ...
  GOOGLE_ROOT_PATH,
  GOOGLE_BALANCE_FILENAME,
  GOOGLE_BALANCE_STAKING_SHEET,
  GOOGLE_BALANCE_YIELDMACHINE_SHEET,
  GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_FILENAME,
  GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_SHEET,
  GOOGLE_VAULT_CHECK_FILENAME,
  GOOGLE_VAULT_CHECK_SHEET,

  // Statistk ...
  GOOGLE_STATISTIK_FILENAME,
  GOOGLE_STATISTIK_DFI_DATA_SHEET,
  GOOGLE_STATISTIK_DUSD_DATA_SHEET;
}
