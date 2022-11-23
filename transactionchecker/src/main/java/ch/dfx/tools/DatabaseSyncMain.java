package ch.dfx.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;

/**
 * Copy table content from remote to local:
 * 
 * - API_DUPLICATE_CHECK
 */
public class DatabaseSyncMain {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseSyncMain.class);

  private static final String IDENTIFIER = "database-sync";

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
      DatabaseSync databaseSync = new DatabaseSync();
      databaseSync.sync();
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }
}
