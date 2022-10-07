package ch.dfx.balance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionAnalyzerMain {
  private static final Logger LOGGER = LogManager.getLogger(TransactionAnalyzerMain.class);

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
      String environment = PayoutManagerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "balance-" + network + "-" + environment);
      PayoutManagerUtils.initLog4j("log4j2-balance.xml");

      // ...
      PayoutManagerUtils.loadConfigProperties(network, environment, args);

      // ...
      TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer();
      transactionAnalyzer.run();
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
