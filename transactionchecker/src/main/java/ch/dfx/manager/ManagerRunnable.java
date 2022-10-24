package ch.dfx.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ManagerRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ManagerRunnable.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;

  // ...
  private boolean isProcessing = false;

  private final boolean isServerOnly;

  private int openTransactionErrorCounter = 0;

  /**
   * 
   */
  public ManagerRunnable(boolean isServerOnly) {
    this.apiAccessHandler = new ApiAccessHandlerImpl();

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

    try {
      if (!isServerOnly) {
        doRun();

        checkErrorCounter();
      }
    } catch (Throwable t) {
      openTransactionErrorCounter++;
      LOGGER.error("run", t);
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun() ...");

    isProcessing = true;

    try {
      executeOpenTransaction();
    } finally {
      isProcessing = false;
    }
  }

  /**
   * 
   */
  private void executeOpenTransaction() {
    LOGGER.trace("executeOpenTransaction() ...");

    try {
      // ...
      apiAccessHandler.signIn();

      // ...
      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      OpenTransactionManager openTransactionManager = new OpenTransactionManager(apiAccessHandler, dataProvider);
      openTransactionManager.execute();
    } catch (DfxException e) {
      openTransactionErrorCounter++;
      LOGGER.error("executeOpenTransaction: openTransactionErrorCounter=" + openTransactionErrorCounter, e.getMessage());
    } catch (Exception e) {
      openTransactionErrorCounter++;
      LOGGER.error("executeOpenTransaction: openTransactionErrorCounter=" + openTransactionErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < openTransactionErrorCounter) {
      LOGGER.error("Too many errors, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
