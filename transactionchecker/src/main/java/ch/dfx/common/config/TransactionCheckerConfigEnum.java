package ch.dfx.common.config;

import javax.annotation.Nonnull;

/**
 * 
 */
public enum TransactionCheckerConfigEnum implements ConfigEntry {
  SERVER_ID("server_id"),
  RAW_TRANSACTION_MAX_SIZE("raw_transaction_max_size"),

  // E-Mail ...
  EMAIL_USER("email.user"),
  EMAIL_PASSWORD("email.password"),
  EMAIL_FROM("email.from"),
  EMAIL_TO("email.to"),

  // Telegram ...
  TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN("telegram.automaticInformationBot.token"),
  TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID("telegram.automaticInformationBot.chatId"),

  // Run Period f�r Scheduler ...
  RUN_PERIOD_MESSAGE_EVENT("scheduler.run_period_message_event"),
  RUN_PERIOD_WATCHDOG("scheduler.run_period_watchdog"),
  RUN_PERIOD_DATABASE("scheduler.run_period_database"),
  RUN_PERIOD_API("scheduler.run_period_api"),
  RUN_PERIOD_REPORT("scheduler.run_period_report"),
  RUN_PERIOD_DEFIMANAGER("scheduler.run_period_defimanager"),

  // LOCK API ...
  LOCK_API_URL("lock.api_url"),
  LOCK_ADDRESS("lock.address"),
  LOCK_SIGNATURE("lock.signature"),
  LOCK_API_TEST_TOKEN("lock.api_test_token"),
  LOCK_SIMULATE_SEND("lock.simulate_send"),

  // H2 Database ...
  H2_SYNC_LOOP("database.sync_loop"),
  H2_SYNC_COMMIT("database.sync_commit"),

  H2_DB_DIR("database.[ENVIRONMENT].db_dir"),
  H2_DB_NAME("database.[ENVIRONMENT].db_name"),

  H2_SERVER_TCP_HOST("database.[ENVIRONMENT].server_tcp_host"),
  H2_SERVER_TCP_PORT("database.[ENVIRONMENT].server_tcp_port"),

  H2_USERNAME("database.[ENVIRONMENT].[HOST_ID].username"),
  H2_PASSWORD("database.[ENVIRONMENT].[HOST_ID].password"),

  // RMI ...
  RMI_HOST("rmi.host"),
  RMI_PORT("rmi.port"),

  // Watchdog ...
  DEFICHAIN_WATCHDOG_EXECUTABLE("watchdog.[ENVIRONMENT].[HOST_ID].defichain_executable"),
  WATCHDOG_EXECUTABLE("watchdog.[ENVIRONMENT].[HOST_ID].transactionserver_executable"),

  // DefiChain ...
  DFI_URL("defichain.[ENVIRONMENT].[HOST_ID].url"),

  DFI_RPC_USERNAME("defichain.rpc_username"),
  DFI_RPC_PASSWORD("defichain.rpc_password"),

  DFI_WALLET_NAME("defichain.wallet_name"),
  DFI_WALLET_PASSWORD("defichain.wallet_password"),

  DFI_WALLET_SIGN_ADDRESS("defichain.wallet_sign_address"),
  DFI_VERIFY_ADDRESS("defichain.verify_address");

  // ...
  private final String absoluteName;

  /**
   * 
   */
  private TransactionCheckerConfigEnum(@Nonnull String absoluteName) {
    this.absoluteName = absoluteName;
  }

  /**
   * 
   */
  @Override
  public String getAbsolutName() {
    return absoluteName;
  }
}
