package ch.dfx.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ManagerRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ManagerRunnable.class);

  private boolean isProcessing = false;

  private final boolean isServerOnly;

  private int masterNodeErrorCounter = 0;

  /**
   * 
   */
  public ManagerRunnable(boolean isServerOnly) {
    this.isServerOnly = isServerOnly;
  }

  @Override
  public String getName() {
    return ManagerRunnable.class.getSimpleName();
  }

  @Override
  public boolean isProcesing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.trace("run() ...");

    if (!isServerOnly) {
      doRun();

      checkErrorCounter();
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun() ...");

    isProcessing = true;

    try {
      executeMasternode();
    } finally {
      isProcessing = false;
    }
  }

  /**
   * 
   */
  private void executeMasternode() {
    LOGGER.trace("executeMasternode() ...");

    try {
      OpenTransactionManager masternodeManager = new OpenTransactionManager();
      masternodeManager.execute();
    } catch (DfxException e) {
      masterNodeErrorCounter++;
      LOGGER.error("executeMasternode: masterNodeErrorCounter=" + masterNodeErrorCounter, e.getMessage());
    } catch (Exception e) {
      masterNodeErrorCounter++;
      LOGGER.error("executeMasternode: masterNodeErrorCounter=" + masterNodeErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < masterNodeErrorCounter) {
      LOGGER.error("Too many errors, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
