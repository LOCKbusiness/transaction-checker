package ch.dfx.transactionserver.database;

import java.io.File;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.builder.BalanceBuilder;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.builder.DepositBuilder;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class DatabaseRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseRunnable.class);

  private boolean isProcessing = false;

  private final File lockFile;

  private final boolean isServerOnly;

  private int databaseErrorCounter = 0;
  private int depositErrorCounter = 0;
  private int balanceErrorCounter = 0;

  /**
   * 
   */
  public DatabaseRunnable(
      @Nonnull File lockFile,
      boolean isServerOnly) {
    Objects.requireNonNull(lockFile, "null lockFile is not allowed");
    this.lockFile = lockFile;
    this.isServerOnly = isServerOnly;
  }

  @Override
  public String getName() {
    return DatabaseRunnable.class.getSimpleName();
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

      checkLockFile();
    } catch (Throwable t) {
      databaseErrorCounter++;
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
      executeDatabase();
      checkDatabase();

      executeDeposit();
      executeBalance();
    } finally {
      isProcessing = false;
    }
  }

  /**
   * 
   */
  private void executeDatabase() {
    LOGGER.trace("executeDatabase() ...");

    try {
      DatabaseBuilder databaseBuilder = new DatabaseBuilder();
      databaseBuilder.build();
    } catch (DfxException e) {
      databaseErrorCounter++;
      LOGGER.error("executeDatabase: databaseErrorCounter=" + databaseErrorCounter, e.getMessage());
    } catch (Exception e) {
      databaseErrorCounter++;
      LOGGER.error("executeDatabase: databaseErrorCounter=" + databaseErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkDatabase() {
    LOGGER.trace("checkDatabase() ...");

    try {
      DatabaseChecker databaseChecker = new DatabaseChecker();

      if (databaseChecker.check()) {
        databaseErrorCounter = 0;
      }
    } catch (DfxException e) {
      databaseErrorCounter++;
      LOGGER.error("checkDatabase: databaseErrorCounter=" + databaseErrorCounter, e.getMessage());
    } catch (Exception e) {
      databaseErrorCounter++;
      LOGGER.error("checkDatabase: databaseErrorCounter=" + databaseErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void executeDeposit() {
    LOGGER.trace("executeDeposit() ...");

    try {
      DepositBuilder depositBuilder = new DepositBuilder();
      depositBuilder.build();
    } catch (DfxException e) {
      depositErrorCounter++;
      LOGGER.error("executeDeposit: depositErrorCounter=" + depositErrorCounter, e.getMessage());
    } catch (Exception e) {
      depositErrorCounter++;
      LOGGER.error("executeDeposit: depositErrorCounter=" + depositErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void executeBalance() {
    LOGGER.trace("executeBalance() ...");

    try {
      BalanceBuilder balanceBuilder = new BalanceBuilder();
      balanceBuilder.build();
    } catch (DfxException e) {
      balanceErrorCounter++;
      LOGGER.error("executeBalance: balanceErrorCounter=" + balanceErrorCounter, e.getMessage());
    } catch (Exception e) {
      balanceErrorCounter++;
      LOGGER.error("executeBalance: balanceErrorCounter=" + balanceErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < databaseErrorCounter
        || 2 < depositErrorCounter
        || 2 < balanceErrorCounter) {
      LOGGER.error("Too many errors, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }

  /**
   * 
   */
  private void checkLockFile() {
    LOGGER.trace("checkLockFile() ...");

    if (!lockFile.exists()) {
      LOGGER.error("Lockfile missing, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
