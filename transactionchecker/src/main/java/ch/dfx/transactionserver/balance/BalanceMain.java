package ch.dfx.transactionserver.balance;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.transactionserver.balance.data.BalanceData;

/**
 * Check Address:
 * --address=dSPPfAPY8BA3TQdqfZRnzJ7212HPWunDms
 * --address=df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9
 * --address=dSPPfAPY8BA3TQdqfZRnzJ7212HPWunDms
 * --address=df1qxylxrcxg0gn5zwvyjydxhzr3lttlarklwrl4n9
 * --address=df1qd07y226vhfluma0axy2j8fnsvcgw8p0p4fd0fu
 * --address=8PzFNiwZQop5Cgqi844GTppWZq8QUtUdZF
 * --address=dJs8vikW87E1M3e5oe4N6hUpBs89Dhh77S
 */
public class BalanceMain {
  private static final Logger LOGGER = LogManager.getLogger(BalanceMain.class);

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
      Optional<String> optionalAddressArgument = Stream.of(args).filter(a -> a.startsWith("--address=")).findFirst();

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = PayoutManagerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "balance-" + network + "-" + environment);
      PayoutManagerUtils.initLog4j("log4j2-balance.xml");

      // ...
      PayoutManagerUtils.loadConfigProperties(network, environment);

      // ...
      if (optionalAddressArgument.isEmpty()) {
        LOGGER.error("usage: --address=[ADDRESS]");
      } else {
        LOGGER.debug("=".repeat(80));
        LOGGER.debug("Network: " + network);
        LOGGER.debug("Environment: " + environment);

        String address = optionalAddressArgument.get().split("=")[1];
        LOGGER.debug("Address: " + address);

        // ...
        Balance balance = new Balance();
        BalanceData balanceData = balance.calcBalance(address);

        LOGGER.debug("VOUT:      " + balanceData.getVout());
        LOGGER.debug("VIN:       " + balanceData.getVin());
        LOGGER.debug("BALANCE:   " + balanceData.getVout().subtract(balanceData.getVin()));
        LOGGER.debug("No. of TX: " + balanceData.getTransactionCount());
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
