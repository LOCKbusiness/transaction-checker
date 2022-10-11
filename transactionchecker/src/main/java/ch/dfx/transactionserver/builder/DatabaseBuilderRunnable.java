package ch.dfx.transactionserver.builder;

import java.io.File;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.DatabaseChecker;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class DatabaseBuilderRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBuilderRunnable.class);

  private final File lockFile;

  private final boolean isServerOnly;

  private int databaseErrorCounter = 0;
  private int depositErrorCounter = 0;
  private int balanceErrorCounter = 0;

  /**
   * 
   */
  public DatabaseBuilderRunnable(
      @Nonnull File lockFile,
      boolean isServerOnly) {
    Objects.requireNonNull(lockFile, "null lockFile is not allowed");
    this.lockFile = lockFile;
    this.isServerOnly = isServerOnly;
  }

  @Override
  public String getName() {
    return DatabaseBuilderRunnable.class.getSimpleName();
  }

  @Override
  public void run() {
    LOGGER.trace("run() ...");

    if (!isServerOnly) {
      executeDatabase();
      checkDatabase();

      executeDeposit();
      executeBalance();

      checkErrorCounter();
    }

    checkLockFile();
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
      DepositBuilder depositManager = new DepositBuilder();
      depositManager.build();
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
      BalanceBuilder balanceManager = new BalanceBuilder();
      balanceManager.build();
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
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private void checkLockFile() {
    LOGGER.trace("checkLockFile() ...");

    if (!lockFile.exists()) {
      LOGGER.error("Lockfile missing, will exit now");
      System.exit(-1);
    }
  }
}
