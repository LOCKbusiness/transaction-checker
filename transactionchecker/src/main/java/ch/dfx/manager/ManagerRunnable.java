package ch.dfx.manager;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ManagerRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ManagerRunnable.class);

  // ...
  private final H2DBManager databaseManager;
  private final ApiAccessHandler apiAccessHandler;

  // ...
  private final String network;
  private final boolean isServerOnly;

  private boolean isProcessing = false;

  // ...
  private int openTransactionErrorCounter = 0;

  /**
   * 
   */
  public ManagerRunnable(
      @Nonnull H2DBManager databaseManager,
      @Nonnull String network,
      boolean isServerOnly) {
    Objects.requireNonNull(databaseManager, "null databaseManager is not allowed");

    this.databaseManager = databaseManager;
    this.network = network;
    this.isServerOnly = isServerOnly;

    this.apiAccessHandler = new ApiAccessHandlerImpl(network);

  }

  @Override
  public String getName() {
    return ManagerRunnable.class.getSimpleName();
  }

  @Override
  public boolean isProcessing() {
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

      OpenTransactionManager openTransactionManager =
          new OpenTransactionManager(network, apiAccessHandler, databaseManager, dataProvider);
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
