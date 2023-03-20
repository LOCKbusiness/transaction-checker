package ch.dfx.config;

import javax.annotation.Nonnull;

import ch.dfx.common.config.ConfigEntry;

/**
 * 
 */
public enum ReportingConfigEnum implements ConfigEntry {
  // Telegram ...
  TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN("telegram.automaticInformationBot.token"),
  TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID("telegram.automaticInformationBot.chatId"),

  TELEGRAM_AUTOMATIC_VAULT_INFORMATION_TOKEN("telegram.automaticVaultInformationBot.token"),
  TELEGRAM_AUTOMATIC_VAULT_INFORMATION_CHAT_ID("telegram.automaticVaultInformationBot.chatId"),

  // Run Period f�r Scheduler ...
  RUN_PERIOD_REPORT("scheduler.run_period_report"),
  RUN_PERIOD_DEFIMANAGER("scheduler.run_period_defimanager"),

  // Yield Machine
  YM_LIQUIDITY_ADDRESS("yieldmachine.liquidity_address"),
  YM_REWARD_ADDRESS("yieldmachine.reward_address"),

  YM_VAULT1_ADDRESS("yieldmachine.vault1.address"),
  YM_VAULT1_ID("yieldmachine.vault1.id"),
  YM_VAULT1_CHECK_RATIO("yieldmachine.vault1.check_ratio"),

  YM_VAULT2_ADDRESS("yieldmachine.vault2.address"),
  YM_VAULT2_ID("yieldmachine.vault2.id"),
  YM_VAULT2_CHECK_RATIO("yieldmachine.vault2.check_ratio"),

  YM_VAULT3_ADDRESS("yieldmachine.vault3.address"),
  YM_VAULT3_ID("yieldmachine.vault3.id"),
  YM_VAULT3_CHECK_RATIO("yieldmachine.vault3.check_ratio"),

  YM_DUSD_LM_ADDRESS("yieldmachine.liquidity_mining.dusd_address"),
  YM_BTC_LM_ADDRESS("yieldmachine.liquidity_mining.btc_address"),
  YM_ETH_LM_ADDRESS("yieldmachine.liquidity_mining.eth_address"),
  YM_USDT_LM_ADDRESS("yieldmachine.liquidity_mining.usdt_address"),
  YM_USDC_LM_ADDRESS("yieldmachine.liquidity_mining.usdc_address"),
  YM_SPY_LM_ADDRESS("yieldmachine.liquidity_mining.spy_address"),

  // Reporting ...
  GOOGLE_ROOT_PATH("google.[ENVIRONMENT].[HOST_ID].root_path"),

  GOOGLE_BALANCE_FILENAME("google.balance.filename"),
  GOOGLE_BALANCE_STAKING_SHEET("google.balance.staking_sheet"),
  GOOGLE_BALANCE_YIELDMACHINE_SHEET("google.balance.yieldmachine_sheet"),

  GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_FILENAME("google.liquidity_masternode_staking_check.filename"),
  GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_SHEET("google.liquidity_masternode_staking_check.sheet"),

  GOOGLE_VAULT_CHECK_FILENAME("google.vault_check.filename"),
  GOOGLE_VAULT_CHECK_SHEET("google.vault_check.sheet"),

  // Statistk Reporting ...
  GOOGLE_STATISTIK_FILENAME("google.statistik.filename"),
  GOOGLE_STATISTIK_DFI_DATA_SHEET("google.statistik.dfi_data_sheet"),
  GOOGLE_STATISTIK_DUSD_DATA_SHEET("google.statistik.dusd_data_sheet"),

  // Transparency Report ...
  GOOGLE_TRANSPARENCY_REPORT_STAKING_FILENAME("google.transparency_report.staking.filename"),
  GOOGLE_TRANSPARENCY_REPORT_STAKING_TOTAL_SHEET("google.transparency_report.staking.total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_STAKING_CUSTOMER_SHEET("google.transparency_report.staking.customer_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_STAKING_MASTERNODE_SHEET("google.transparency_report.staking.masternode_sheet"),

  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_FILENAME("google.transparency_report.yieldmachine.filename"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_TRANSACTION_SHEET("google.transparency_report.yieldmachine.transaction_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_CUSTOMER_SHEET("google.transparency_report.yieldmachine.customer_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_TOTAL_SHEET("google.transparency_report.yieldmachine.total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DFI_TOTAL_SHEET("google.transparency_report.yieldmachine.dfi_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DUSD_TOTAL_SHEET("google.transparency_report.yieldmachine.dusd_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_BTC_TOTAL_SHEET("google.transparency_report.yieldmachine.btc_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_ETH_TOTAL_SHEET("google.transparency_report.yieldmachine.eth_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_USDT_TOTAL_SHEET("google.transparency_report.yieldmachine.usdt_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_USDC_TOTAL_SHEET("google.transparency_report.yieldmachine.usdc_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_SPY_TOTAL_SHEET("google.transparency_report.yieldmachine.spy_total_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DIAGRAM_SHEET("google.transparency_report.yieldmachine.diagram_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_AMOUNT_SHEET("google.transparency_report.yieldmachine.history_amount_sheet"),
  GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_PRICE_SHEET("google.transparency_report.yieldmachine.history_price_sheet"),

  // Rendite ...
  GOOGLE_YIELD_REPORT_YIELDMACHINE_FILENAME("google.yield_report.filename"),
  GOOGLE_YIELD_REPORT_YIELDMACHINE_RENDITE_SHEET("google.yield_report.rendite_sheet"),
  GOOGLE_YIELD_REPORT_YIELDMACHINE_USDT_DUSD_SHEET("google.yield_report.usdt_dusd_sheet"),

  // H2 Database ...
  H2_DB_DIR("database.[ENVIRONMENT].db_dir"),
  H2_DB_NAME("database.[ENVIRONMENT].db_name"),

  H2_SERVER_TCP_HOST("database.[ENVIRONMENT].server_tcp_host"),
  H2_SERVER_TCP_PORT("database.[ENVIRONMENT].server_tcp_port"),

  H2_USERNAME("database.[ENVIRONMENT].[HOST_ID].username"),
  H2_PASSWORD("database.[ENVIRONMENT].[HOST_ID].password"),

  // DefiChain ...
  DFI_URL("defichain.[ENVIRONMENT].[HOST_ID].url"),

  DFI_RPC_USERNAME("defichain.rpc_username"),
  DFI_RPC_PASSWORD("defichain.rpc_password");

  // ...
  private final String absoluteName;

  /**
   * 
   */
  private ReportingConfigEnum(@Nonnull String absoluteName) {
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
