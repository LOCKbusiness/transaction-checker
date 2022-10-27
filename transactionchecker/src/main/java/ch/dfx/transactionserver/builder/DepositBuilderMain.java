package ch.dfx.transactionserver.builder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;

/**
 * 
 */
public class DepositBuilderMain {
  private static final Logger LOGGER = LogManager.getLogger(DepositBuilderMain.class);

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = false;

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "depositbuilder-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      H2DBManager databaseManager = new H2DBManagerImpl();

      // ...
      DepositBuilder depositBuilder = new DepositBuilder(databaseManager);
      depositBuilder.build();
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
