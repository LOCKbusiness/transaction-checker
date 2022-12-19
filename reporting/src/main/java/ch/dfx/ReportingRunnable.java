package ch.dfx;

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
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ReportingRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ReportingRunnable.class);

  // ...
  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private boolean isProcessing = false;

  /**
   * 
   */
  public ReportingRunnable(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;
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

    List<String> logInfoList = new ArrayList<>();

    executeStakingBalanceReport(logInfoList);
    executeLiquidityMasternodeStakingBalanceReport(logInfoList);

    writeLogInfo(logInfoList);
  }

  /**
   * 
   */
  private void executeStakingBalanceReport(@Nonnull List<String> logInfoList) {
    LOGGER.trace("executeStakingBalanceReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String balanceFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_FILENAME);
      String stakingBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_STAKING_SHEET);
      String yieldmachineBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_BALANCE_YIELDMACHINE_SHEET);

      if (null != rootPath
          && null != balanceFileName
          && null != stakingBalanceSheet
          && null != yieldmachineBalanceSheet) {
        BalanceReporting stakingBalanceReporting = new BalanceReporting(network, databaseManager, logInfoList);
        stakingBalanceReporting.report(TokenEnum.DFI, rootPath, balanceFileName, stakingBalanceSheet);
        stakingBalanceReporting.report(TokenEnum.DUSD, rootPath, balanceFileName, yieldmachineBalanceSheet);
      }
    } catch (Exception e) {
      LOGGER.error("executeStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void executeLiquidityMasternodeStakingBalanceReport(@Nonnull List<String> logInfoList) {
    LOGGER.trace("executeLiquidityMasternodeStakingBalanceReport() ...");

    try {
      String rootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String checkFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_FILENAME);
      String checkSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_SHEET);

      if (null != rootPath
          && null != checkFileName
          && null != checkSheet) {
        LiquidityMasternodeStakingReporting liquidityMasternodeStakingReporting =
            new LiquidityMasternodeStakingReporting(network, databaseManager, logInfoList);
        liquidityMasternodeStakingReporting.report(TokenEnum.DFI, rootPath, checkFileName, checkSheet);
      }
    } catch (Exception e) {
      LOGGER.error("executeLiquidityMasternodeStakingBalanceReport", e);
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
