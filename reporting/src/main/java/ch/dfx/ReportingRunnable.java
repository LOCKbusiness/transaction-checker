package ch.dfx;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.logging.notifier.TelegramNotifier;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.LiquidityMasternodeStakingReporting;
import ch.dfx.reporting.VaultReporting;
import ch.dfx.statistik.StatistikReporting;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ReportingRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ReportingRunnable.class);

  // ...
  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseStakingBalanceHelper;
  private final DatabaseBalanceHelper databaseYieldmachineBalanceHelper;

  private boolean isProcessing = false;

  /**
   * 
   */
  public ReportingRunnable(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseStakingBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseYieldmachineBalanceHelper = new DatabaseBalanceHelper(network);
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run() ...");

    long startTime = System.currentTimeMillis();

    isProcessing = true;

    try {
      doRun();
    } catch (Throwable t) {
      LOGGER.error("run", t);
    } finally {
      isProcessing = false;

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseStakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);
      databaseYieldmachineBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

      List<String> logInfoList = new ArrayList<>();

      createStakingBalanceReport(connection, logInfoList);
      createLiquidityMasternodeStakingBalanceReport(connection, logInfoList);
      createVaultReport(connection, logInfoList);

      createStatistikReport(connection);

      writeLogInfo(logInfoList);

      // ...
      databaseBlockHelper.closeStatements();
      databaseStakingBalanceHelper.closeStatements();
      databaseYieldmachineBalanceHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("doRun", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void createStakingBalanceReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createStakingBalanceReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String balanceFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_FILENAME);
      String stakingBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_STAKING_SHEET);
      String dfiYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_DFI_YIELDMACHINE_SHEET);
      String dusdYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_DUSD_YIELDMACHINE_SHEET);
      String btcYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_BTC_YIELDMACHINE_SHEET);
      String ethYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_ETH_YIELDMACHINE_SHEET);
      String usdtYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_USDT_YIELDMACHINE_SHEET);
      String usdcYieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_USDC_YIELDMACHINE_SHEET);

      if (null != rootPath
          && null != balanceFileName
          && null != stakingBalanceSheet
          && null != dfiYieldmachineBalanceSheet
          && null != dusdYieldmachineBalanceSheet
          && null != btcYieldmachineBalanceSheet
          && null != ethYieldmachineBalanceSheet
          && null != usdtYieldmachineBalanceSheet
          && null != usdcYieldmachineBalanceSheet) {
        BalanceReporting stakingBalanceReporting =
            new BalanceReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, logInfoList);
        stakingBalanceReporting.report(connection, TokenEnum.DFI, rootPath, balanceFileName, stakingBalanceSheet);

        BalanceReporting yieldmachineBalanceReporting =
            new BalanceReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper, logInfoList);
        yieldmachineBalanceReporting.report(connection, TokenEnum.DFI, rootPath, balanceFileName, dfiYieldmachineBalanceSheet);
        yieldmachineBalanceReporting.report(connection, TokenEnum.DUSD, rootPath, balanceFileName, dusdYieldmachineBalanceSheet);
        // yieldmachineBalanceReporting.report(connection, TokenEnum.BTC, rootPath, balanceFileName, btcYieldmachineBalanceSheet);
        // yieldmachineBalanceReporting.report(connection, TokenEnum.ETH, rootPath, balanceFileName, ethYieldmachineBalanceSheet);
        // yieldmachineBalanceReporting.report(connection, TokenEnum.USDT, rootPath, balanceFileName, usdtYieldmachineBalanceSheet);
        // yieldmachineBalanceReporting.report(connection, TokenEnum.USDC, rootPath, balanceFileName, usdcYieldmachineBalanceSheet);

        logInfoList.add("");
      }
    } catch (Exception e) {
      LOGGER.error("createStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void createLiquidityMasternodeStakingBalanceReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createLiquidityMasternodeStakingBalanceReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String checkFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_FILENAME);
      String checkSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_SHEET);

      if (null != rootPath
          && null != checkFileName
          && null != checkSheet) {
        LiquidityMasternodeStakingReporting liquidityMasternodeStakingReporting =
            new LiquidityMasternodeStakingReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, logInfoList);
        liquidityMasternodeStakingReporting.report(connection, TokenEnum.DFI, rootPath, checkFileName, checkSheet);

        logInfoList.add("");
      }
    } catch (Exception e) {
      LOGGER.error("createLiquidityMasternodeStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void createVaultReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createVaultReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String checkFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_VAULT_CHECK_FILENAME);
      String checkSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_VAULT_CHECK_SHEET);

      if (null != rootPath
          && null != checkFileName
          && null != checkSheet) {
        VaultReporting vaultReporting =
            new VaultReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, logInfoList);
        vaultReporting.report(connection, TokenEnum.DUSD, rootPath, checkFileName, checkSheet);
      }
    } catch (Exception e) {
      LOGGER.error("createVaultReport", e);
    }
  }

  /**
   * 
   */
  private void createStatistikReport(@Nonnull Connection connection) {
    LOGGER.trace("createStatistikReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String statistikFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_STATISTIK_FILENAME);
      String statistikDfiDataSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_STATISTIK_DFI_DATA_SHEET);
      String statistikDusdDataSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_STATISTIK_DUSD_DATA_SHEET);

      if (null != rootPath
          && null != statistikFileName
          && null != statistikDfiDataSheet
          && null != statistikDusdDataSheet) {
        StatistikReporting statistikStakingReporting =
            new StatistikReporting(network, databaseBlockHelper, databaseStakingBalanceHelper);
        statistikStakingReporting.report(connection, TokenEnum.DFI, rootPath, statistikFileName, statistikDfiDataSheet);

        StatistikReporting statistikYieldmachineReporting =
            new StatistikReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);
        statistikYieldmachineReporting.report(connection, TokenEnum.DUSD, rootPath, statistikFileName, statistikDusdDataSheet);
      }
    } catch (Exception e) {
      LOGGER.error("createStatistikReport", e);
    }
  }

  /**
   * 
   */
  private void writeLogInfo(@Nonnull List<String> logInfoList) {
    LOGGER.trace("writeLogInfo()");

    try {
      StringBuilder logInfoBuilder = new StringBuilder();

      for (String logInfo : logInfoList) {
        logInfoBuilder.append(logInfo).append("\n");
      }

      TelegramNotifier telegramNotifier = new TelegramNotifier();
      telegramNotifier.sendMessage(logInfoBuilder.toString());
    } catch (Exception e) {
      LOGGER.error("writeLogInfo", e);
    }
  }
}
