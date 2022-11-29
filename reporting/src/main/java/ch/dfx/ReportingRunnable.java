package ch.dfx;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.logging.notifier.TelegramNotifier;
import ch.dfx.reporting.LiquidityMasternodeStakingReporting;
import ch.dfx.reporting.StakingBalanceReporting;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ReportingRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ReportingRunnable.class);

  // ...
  private final H2DBManager databaseManager;

  private boolean isProcessing = false;

  /**
   * 
   */
  public ReportingRunnable(@Nonnull H2DBManager databaseManager) {
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
      StakingBalanceReporting stakingBalanceReporting = new StakingBalanceReporting(databaseManager, logInfoList);
      stakingBalanceReporting.report();
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
      LiquidityMasternodeStakingReporting liquidityMasternodeStakingReporting = new LiquidityMasternodeStakingReporting(databaseManager, logInfoList);
      liquidityMasternodeStakingReporting.report();
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
