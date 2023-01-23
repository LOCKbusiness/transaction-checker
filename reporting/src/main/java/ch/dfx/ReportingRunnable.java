package ch.dfx;

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
  private final DatabaseBalanceHelper databaseBalanceHelper;

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
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
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
      databaseBalanceHelper.openStatements(connection);

      List<String> logInfoList = new ArrayList<>();

      createStakingBalanceReport(connection, logInfoList);
      createLiquidityMasternodeStakingBalanceReport(connection, logInfoList);
      createVaultReport(connection, logInfoList);

      createStatistikReport(connection);

      writeLogInfo(logInfoList);

      // ...
      databaseBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();
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
      String yieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_YIELDMACHINE_SHEET);

      if (null != rootPath
          && null != balanceFileName
          && null != stakingBalanceSheet
          && null != yieldmachineBalanceSheet) {
        BalanceReporting stakingBalanceReporting = new BalanceReporting(network, databaseBlockHelper, databaseBalanceHelper, logInfoList);
        stakingBalanceReporting.report(connection, TokenEnum.DFI, rootPath, balanceFileName, stakingBalanceSheet);
        stakingBalanceReporting.report(connection, TokenEnum.DUSD, rootPath, balanceFileName, yieldmachineBalanceSheet);

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
            new LiquidityMasternodeStakingReporting(network, databaseBlockHelper, databaseBalanceHelper, logInfoList);
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
            new VaultReporting(network, databaseBlockHelper, databaseBalanceHelper, logInfoList);
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
        StatistikReporting statistikReporting = new StatistikReporting(network, databaseBlockHelper, databaseBalanceHelper);
        statistikReporting.report(connection, TokenEnum.DFI, rootPath, statistikFileName, statistikDfiDataSheet);
        statistikReporting.report(connection, TokenEnum.DUSD, rootPath, statistikFileName, statistikDusdDataSheet);
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
