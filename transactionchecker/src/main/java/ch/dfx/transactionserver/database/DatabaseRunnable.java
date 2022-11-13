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
import ch.dfx.transactionserver.builder.MasternodeBuilder;
import ch.dfx.transactionserver.builder.StakingBuilder;
import ch.dfx.transactionserver.cleaner.StakingWithdrawalReservedCleaner;
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
  private int databaseBuilderErrorCounter = 0;
  private int depositBuilderErrorCounter = 0;
  private int balanceBuilderErrorCounter = 0;
  private int stakingBuilderErrorCounter = 0;
  private int masternodeBuilderErrorCounter = 0;
  private int stakingWithdrawalReservedCleanerErrorCounter = 0;

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
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.trace("run() ...");

    isProcessing = true;

    try {
      if (!isServerOnly) {
        doRun();

        checkErrorCounter();
      }

      checkProcessLockfile();
    } catch (Throwable t) {
      databaseBuilderErrorCounter++;
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

    // ...
    executeDatabase();
    checkDatabase();

    // ...
    executeDeposit();
    executeBalance();
    executeStaking();
    executeMasternode();

    // ...
    executeStakingWithdrawalReservedCleaner();
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
      databaseBuilderErrorCounter++;
      LOGGER.error("Database Builder: errorCounter=" + databaseBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      databaseBuilderErrorCounter++;
      LOGGER.error("Database Builder: errorCounter=" + databaseBuilderErrorCounter, e);
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
        databaseBuilderErrorCounter = 0;
      }
    } catch (DfxException e) {
      databaseBuilderErrorCounter++;
      LOGGER.error("Database Checker: errorCounter=" + databaseBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      databaseBuilderErrorCounter++;
      LOGGER.error("Database Checker: errorCounter=" + databaseBuilderErrorCounter, e);
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
      depositBuilderErrorCounter++;
      LOGGER.error("Deposit Builder: errorCounter=" + depositBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      depositBuilderErrorCounter++;
      LOGGER.error("Deposit Builder: errorCounter=" + depositBuilderErrorCounter, e);
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
      balanceBuilderErrorCounter++;
      LOGGER.error("Balance Builder: errorCounter=" + balanceBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      balanceBuilderErrorCounter++;
      LOGGER.error("Balance Builder: errorCounter=" + balanceBuilderErrorCounter, e);
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
      stakingBuilderErrorCounter++;
      LOGGER.error("Staking Builder: errorCounter=" + stakingBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      stakingBuilderErrorCounter++;
      LOGGER.error("Staking Builder: errorCounter=" + stakingBuilderErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void executeMasternode() {
    LOGGER.trace("executeMasternode() ...");

    try {
      MasternodeBuilder masternodeBuilder = new MasternodeBuilder(databaseManager);
      masternodeBuilder.build();
    } catch (DfxException e) {
      masternodeBuilderErrorCounter++;
      LOGGER.error("Masternode Builder: errorCounter=" + masternodeBuilderErrorCounter, e.getMessage());
    } catch (Exception e) {
      masternodeBuilderErrorCounter++;
      LOGGER.error("Masternode Builder: errorCounter=" + masternodeBuilderErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void executeStakingWithdrawalReservedCleaner() {
    LOGGER.trace("executeStakingWithdrawalReservedCleaner() ...");

    try {
      StakingWithdrawalReservedCleaner stakingWithdrawalReservedCleaner = new StakingWithdrawalReservedCleaner(databaseManager);
      stakingWithdrawalReservedCleaner.clean();
    } catch (DfxException e) {
      stakingWithdrawalReservedCleanerErrorCounter++;
      LOGGER.error("StakingWithdrawalReserved Cleaner: errorCounter=" + stakingWithdrawalReservedCleanerErrorCounter, e.getMessage());
    } catch (Exception e) {
      masternodeBuilderErrorCounter++;
      LOGGER.error("StakingWithdrawalReserved Cleaner: errorCounter=" + stakingWithdrawalReservedCleanerErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < databaseBuilderErrorCounter
        || 2 < depositBuilderErrorCounter
        || 2 < balanceBuilderErrorCounter
        || 2 < stakingBuilderErrorCounter
        || 2 < masternodeBuilderErrorCounter
        || 2 < stakingWithdrawalReservedCleanerErrorCounter) {
      LOGGER.error("Too many errors, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }

  /**
   * 
   */
  private void checkProcessLockfile() {
    LOGGER.trace("checkProcessLockfile() ...");

    if (!processLockfile.exists()) {
      LOGGER.error("Process lockfile missing, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
