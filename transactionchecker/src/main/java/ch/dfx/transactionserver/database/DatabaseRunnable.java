package ch.dfx.transactionserver.database;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.io.File;
import java.sql.Connection;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.builder.BalanceBuilder;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.builder.DepositBuilder;
import ch.dfx.transactionserver.builder.MasternodeBuilder;
import ch.dfx.transactionserver.builder.StakingBuilder;
import ch.dfx.transactionserver.cleaner.StakingWithdrawalReservedCleaner;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;
import ch.dfx.transactionserver.ymbuilder.YmBalanceBuilder;
import ch.dfx.transactionserver.ymbuilder.YmDepositBuilder;
import ch.dfx.transactionserver.ymbuilder.YmStakingBuilder;

/**
 * 
 */
public class DatabaseRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseRunnable.class);

  // ...
  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseStakingBalanceHelper;
  private final DatabaseBalanceHelper databaseYieldmachineBalanceHelper;
  private final DatabaseAddressHandler databaseAddressHandler;

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
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager,
      @Nonnull File processLockfile,
      boolean isServerOnly) {
    Objects.requireNonNull(databaseManager, "null network is not allowed");
    Objects.requireNonNull(databaseManager, "null databaseManager is not allowed");
    Objects.requireNonNull(processLockfile, "null processLockfile is not allowed");

    this.network = network;
    this.databaseManager = databaseManager;
    this.processLockfile = processLockfile;
    this.isServerOnly = isServerOnly;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseStakingBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseYieldmachineBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseAddressHandler = new DatabaseAddressHandler(network);
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run()");

    long startTime = System.currentTimeMillis();

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

      LOGGER.debug("[DatabaseRunnable] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun()");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseStakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);
      databaseYieldmachineBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

      // ...
      if (NetworkEnum.STAGNET != network) {
        executeDatabase(connection);
        checkDatabase(connection);
      }

      // ...
      executeDeposit(connection);
      executeBalance(connection);
      executeStaking(connection);
      executeMasternode(connection);

      // ...
      executeStakingWithdrawalReservedCleaner(connection);

      databaseStakingBalanceHelper.closeStatements();
      databaseYieldmachineBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      databaseBuilderErrorCounter++;
      LOGGER.error("Database Builder: errorCounter=" + databaseBuilderErrorCounter, e.getMessage());
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void executeDatabase(@Nonnull Connection connection) {
    LOGGER.trace("executeDatabase()");

    try {
      DatabaseBuilder databaseBuilder = new DatabaseBuilder(network, databaseBlockHelper, databaseAddressHandler);
      databaseBuilder.build(connection);
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
  private void checkDatabase(@Nonnull Connection connection) {
    LOGGER.trace("checkDatabase()");

    try {
      DatabaseChecker databaseChecker = new DatabaseChecker(network);

      if (databaseChecker.check(connection)) {
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
  private void executeDeposit(@Nonnull Connection connection) {
    LOGGER.trace("executeDeposit()");

    try {
      DepositBuilder depositBuilder = new DepositBuilder(network, databaseStakingBalanceHelper);
      depositBuilder.build(connection);

      YmDepositBuilder ymDepositBuilder = new YmDepositBuilder(network, databaseYieldmachineBalanceHelper);
      ymDepositBuilder.build(connection);

      depositBuilderErrorCounter = 0;
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
  private void executeBalance(@Nonnull Connection connection) {
    LOGGER.trace("executeBalance()");

    try {
      BalanceBuilder balanceBuilder = new BalanceBuilder(network, databaseStakingBalanceHelper);
      balanceBuilder.build(connection, TokenEnum.DFI);

      YmBalanceBuilder ymBalanceBuilder = new YmBalanceBuilder(network, databaseYieldmachineBalanceHelper);
      ymBalanceBuilder.build(connection, TokenEnum.DFI);
      ymBalanceBuilder.build(connection, TokenEnum.DUSD);

      balanceBuilderErrorCounter = 0;
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
  private void executeStaking(@Nonnull Connection connection) {
    LOGGER.trace("executeStaking()");

    try {
      StakingBuilder stakingBuilder = new StakingBuilder(network, databaseStakingBalanceHelper);
      stakingBuilder.build(connection, TokenEnum.DFI);

      YmStakingBuilder ymStakingBuilder = new YmStakingBuilder(network, databaseYieldmachineBalanceHelper);
      ymStakingBuilder.build(connection, TokenEnum.DFI);
      ymStakingBuilder.build(connection, TokenEnum.DUSD);

      stakingBuilderErrorCounter = 0;
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
  private void executeMasternode(@Nonnull Connection connection) {
    LOGGER.trace("executeMasternode()");

    try {
      MasternodeBuilder masternodeBuilder = new MasternodeBuilder(network, databaseBlockHelper);
      masternodeBuilder.build(connection);

      masternodeBuilderErrorCounter = 0;
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
  private void executeStakingWithdrawalReservedCleaner(@Nonnull Connection connection) {
    LOGGER.trace("executeStakingWithdrawalReservedCleaner()");

    try {
      StakingWithdrawalReservedCleaner stakingWithdrawalReservedCleaner =
          new StakingWithdrawalReservedCleaner(network, databaseBlockHelper, databaseStakingBalanceHelper);
      stakingWithdrawalReservedCleaner.clean(connection, TOKEN_STAKING_SCHEMA, TokenEnum.DFI);

      StakingWithdrawalReservedCleaner yieldmaschineWithdrawalReservedCleaner =
          new StakingWithdrawalReservedCleaner(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);
      yieldmaschineWithdrawalReservedCleaner.clean(connection, TOKEN_YIELDMACHINE_SCHEMA, TokenEnum.DFI);
      yieldmaschineWithdrawalReservedCleaner.clean(connection, TOKEN_YIELDMACHINE_SCHEMA, TokenEnum.DUSD);

      stakingWithdrawalReservedCleanerErrorCounter = 0;
    } catch (DfxException e) {
      stakingWithdrawalReservedCleanerErrorCounter++;
      LOGGER.error("StakingWithdrawalReserved Cleaner: errorCounter=" + stakingWithdrawalReservedCleanerErrorCounter, e.getMessage());
    } catch (Exception e) {
      stakingWithdrawalReservedCleanerErrorCounter++;
      LOGGER.error("StakingWithdrawalReserved Cleaner: errorCounter=" + stakingWithdrawalReservedCleanerErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter()");

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
    LOGGER.trace("checkProcessLockfile()");

    if (!processLockfile.exists()) {
      LOGGER.error("Process lockfile missing, will exit now");
      SchedulerProvider.getInstance().exit(-1);
    }
  }
}
