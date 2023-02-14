package ch.dfx.account;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;

import java.sql.Connection;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class AccountReportingMain {
  private static final Logger LOGGER = LogManager.getLogger(AccountReportingMain.class);

  private static final String IDENTIFIER = "accountreporting";

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
      TransactionCheckerUtils.setupGlobalProvider(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      H2DBManager databaseManager = new H2DBManagerImpl();
      Connection connection = databaseManager.openConnection();

      // ...
      DatabaseBlockHelper databaseBlockHelper = new DatabaseBlockHelper(network);
      databaseBlockHelper.openStatements(connection);

      DatabaseBalanceHelper databaseStakingBalanceHelper = new DatabaseBalanceHelper(network);
      databaseStakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);

      // ...
      AccountReporting reporting = new AccountReporting(network, databaseBlockHelper, databaseStakingBalanceHelper);
      reporting.reportStaking(connection, "data", "kontoauszug.xlsx", "Staking (DFI)");
      reporting.reportYieldmachine(connection, "data", "kontoauszug.xlsx", "YM (DFI)", TokenEnum.DFI);
      reporting.reportYieldmachine(connection, "data", "kontoauszug.xlsx", "YM (DUSD)", TokenEnum.DUSD);

      // ...
      databaseStakingBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();
      databaseManager.closeConnection(connection);
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }
}
