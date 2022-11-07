package ch.dfx;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    LOGGER.trace("run() ...");

    isProcessing = true;

    try {
      doRun();
    } catch (Throwable t) {
      LOGGER.error("run", t);
    } finally {
      isProcessing = false;
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun() ...");

    executeStakingBalanceReport();
    executeLiquidityMasternodeStakingBalanceReport();
  }

  /**
   * 
   */
  private void executeStakingBalanceReport() {
    LOGGER.trace("executeStakingBalanceReport() ...");

    try {
      StakingBalanceReporting stakingBalanceReporting = new StakingBalanceReporting(databaseManager);
      stakingBalanceReporting.report();
    } catch (Exception e) {
      LOGGER.error("executeStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void executeLiquidityMasternodeStakingBalanceReport() {
    LOGGER.trace("executeLiquidityMasternodeStakingBalanceReport() ...");

    try {
      LiquidityMasternodeStakingReporting liquidityMasternodeStakingReporting = new LiquidityMasternodeStakingReporting(databaseManager);
      liquidityMasternodeStakingReporting.report();
    } catch (Exception e) {
      LOGGER.error("executeLiquidityMasternodeStakingBalanceReport", e);
    }
  }
}
