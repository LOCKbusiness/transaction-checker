package ch.dfx.lockbusiness.stakingbalances.api;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class ApiAccessManagerMain {
  private static final Logger LOGGER = LogManager.getLogger(ApiAccessManagerMain.class);

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "apiaccessmanager-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-transactionchecker.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment, args);

      // ...
      ApiAccessManager accessManager = new ApiAccessManager();
      accessManager.signIn();

      accessManager.getCustomerWalletList();
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }
}
