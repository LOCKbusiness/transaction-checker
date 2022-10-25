package ch.dfx.transactionserver.builder;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * Mainnet Check Address:
 * --address=dSPPfAPY8BA3TQdqfZRnzJ7212HPWunDms
 * --address=df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9
 * --address=dSPPfAPY8BA3TQdqfZRnzJ7212HPWunDms
 * --address=df1qxylxrcxg0gn5zwvyjydxhzr3lttlarklwrl4n9
 * --address=df1qd07y226vhfluma0axy2j8fnsvcgw8p0p4fd0fu
 * --address=8PzFNiwZQop5Cgqi844GTppWZq8QUtUdZF
 * --address=dJs8vikW87E1M3e5oe4N6hUpBs89Dhh77S
 */
public class BalanceBuilderMain {
  private static final Logger LOGGER = LogManager.getLogger(BalanceBuilderMain.class);

  /**
   * 
   */
  public static void main(String[] args) {

    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "balancebuilder-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      BalanceBuilder balanceBuilder = new BalanceBuilder();
      balanceBuilder.build();
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
