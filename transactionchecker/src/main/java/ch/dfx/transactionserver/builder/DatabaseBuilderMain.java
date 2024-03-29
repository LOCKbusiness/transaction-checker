package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;
import ch.dfx.transactionserver.ymbuilder.YmBalanceBuilder;
import ch.dfx.transactionserver.ymbuilder.YmDepositBuilder;
import ch.dfx.transactionserver.ymbuilder.YmStakingBuilder;

/**
 * 
 */
public class DatabaseBuilderMain {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBuilderMain.class);

  private static final String IDENTIFIER = "databasebuilder";

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
      boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
      boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment, args);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      H2DBManager databaseManager = new H2DBManagerImpl();
      Connection connection = databaseManager.openConnection();

      DatabaseBlockHelper databaseBlockHelper = new DatabaseBlockHelper(network);
      databaseBlockHelper.openStatements(connection);

      DatabaseAddressHandler databaseAddressHandler = new DatabaseAddressHandler(network);

      // ...
      DatabaseBuilder databaseBuilder = new DatabaseBuilder(network, databaseBlockHelper, databaseAddressHandler);
      databaseBuilder.build(connection);

      // ...
      updateStaking(network, connection);
      updateYieldmachine(network, connection);

      // ...
      databaseBlockHelper.closeStatements();
      databaseManager.closeConnection(connection);
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private static void updateStaking(
      @Nonnull NetworkEnum network,
      @Nonnull Connection connection) throws DfxException {
    // ...
    DatabaseBalanceHelper databaseBalanceHelper = new DatabaseBalanceHelper(network);
    databaseBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);

    // ...
    DepositBuilder depositBuilder = new DepositBuilder(network, databaseBalanceHelper);
    depositBuilder.build(connection);

    // ...
    BalanceBuilder balanceBuilder = new BalanceBuilder(network, databaseBalanceHelper);
    balanceBuilder.build(connection, TokenEnum.DFI);

    // ...
    StakingBuilder stakingBuilder = new StakingBuilder(network, databaseBalanceHelper);
    stakingBuilder.build(connection, TokenEnum.DFI);

    databaseBalanceHelper.closeStatements();
  }

  /**
   * 
   */
  private static void updateYieldmachine(
      @Nonnull NetworkEnum network,
      @Nonnull Connection connection) throws DfxException {
    // ...
    DatabaseBalanceHelper databaseBalanceHelper = new DatabaseBalanceHelper(network);
    databaseBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

    // ...
    YmDepositBuilder ymDepositBuilder = new YmDepositBuilder(network, databaseBalanceHelper);
    ymDepositBuilder.build(connection);

    // ...
    YmBalanceBuilder ymBalanceBuilder = new YmBalanceBuilder(network, databaseBalanceHelper);
    ymBalanceBuilder.build(connection);

    // ...
    YmStakingBuilder ymStakingBuilder = new YmStakingBuilder(network, databaseBalanceHelper);
    ymStakingBuilder.build(connection);

    databaseBalanceHelper.closeStatements();
  }
}
