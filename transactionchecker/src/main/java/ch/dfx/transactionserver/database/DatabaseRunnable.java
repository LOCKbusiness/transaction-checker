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
import ch.dfx.transactionserver.builder.StakingBuilder;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class DatabaseRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseRunnable.class);

  // ...
  private final H2DBManager databaseManager;

  // ...
  private final File processLockfile;

  private final boolean isServerOnly;

  private boolean isProcessing = false;

  // ...
  private int databaseErrorCounter = 0;
  private int depositErrorCounter = 0;
  private int balanceErrorCounter = 0;
  private int stakingErrorCounter = 0;

  /**
   * 
   */
  public DatabaseRunnable(
      @Nonnull H2DBManager databaseManager,
      @Nonnull File processLockfile,
      boolean isServerOnly) {
    Objects.requireNonNull(databaseManager, "null databaseManager is not allowed");
    Objects.requireNonNull(processLockfile, "null processLockfile is not allowed");

    this.databaseManager = databaseManager;
    this.processLockfile = processLockfile;
    this.isServerOnly = isServerOnly;
  }

  @Override
  public String getName() {
    return DatabaseRunnable.class.getSimpleName();
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
      executeStaking();
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
      DatabaseBuilder databaseBuilder = new DatabaseBuilder(databaseManager);
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
      DatabaseChecker databaseChecker = new DatabaseChecker(databaseManager);

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
      DepositBuilder depositBuilder = new DepositBuilder(databaseManager);
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
      BalanceBuilder balanceBuilder = new BalanceBuilder(databaseManager);
      balanceBuilder.build();
    } catch (DfxException e) {
      balanceErrorCounter++;
      LOGGER.error("executeBalance: balanceErrorCounter=" + balanceErrorCounter, e.getMessage());
    } catch (Exception e) {
      balanceErrorCounter++;
    }
  }

  /**
   * 
   */
  private void executeStaking() {
    LOGGER.trace("executeStaking() ...");

    try {
      StakingBuilder stakingBuilder = new StakingBuilder(databaseManager);
      stakingBuilder.build();
    } catch (DfxException e) {
      stakingErrorCounter++;
      LOGGER.error("executeStaking: stakingErrorCounter=" + stakingErrorCounter, e.getMessage());
    } catch (Exception e) {
      stakingErrorCounter++;
      LOGGER.error("executeStaking: stakingErrorCounter=" + stakingErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < databaseErrorCounter
        || 2 < depositErrorCounter
        || 2 < balanceErrorCounter
        || 2 < stakingErrorCounter) {
      LOGGER.error("Too many errors, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }

  /**
   * 
   */
  private void checkLockFile() {
    LOGGER.trace("checkLockFile() ...");

    if (!processLockfile.exists()) {
      LOGGER.error("Process lockfile missing, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
