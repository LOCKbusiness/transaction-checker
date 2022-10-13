package ch.dfx.manager;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class MasternodeManagerMain {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeManagerMain.class);

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
      System.setProperty("logFilename", "masternodemanager-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      MasternodeManager masternodeManager = new MasternodeManager();
      masternodeManager.execute();
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }
}
