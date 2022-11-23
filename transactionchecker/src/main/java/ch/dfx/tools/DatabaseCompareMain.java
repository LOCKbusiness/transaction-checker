package ch.dfx.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;

/**
 * Compare table content from remote and local:
 * 
 * - MASTERNODE_WHITELIST
 * - STAKING_ADDRESS
 * - BALANCE
 * - DEPOSIT
 * - STAKING
 */
public class DatabaseCompareMain {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseCompareMain.class);

  private static final String IDENTIFIER = "database-compare";

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(false, false, true);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      DatabaseCompare databaseCompare = new DatabaseCompare();
      databaseCompare.compare();
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }
}
