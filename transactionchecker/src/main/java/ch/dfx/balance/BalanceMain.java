package ch.dfx.balance;

import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.transactionserver.data.DepositBalanceDTO;

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

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = PayoutManagerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "balance-" + network + "-" + environment);
      PayoutManagerUtils.initLog4j("log4j2-balance.xml");

      // ...
      PayoutManagerUtils.loadConfigProperties(network, environment, args);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      Balance balance = new Balance();
      List<DepositBalanceDTO> depositBalanceDTOList = balance.run();

      StringBuilder checkBuilder = new StringBuilder();

      for (DepositBalanceDTO depositBalanceDTO : depositBalanceDTOList) {
        LOGGER.debug("========================================");
        LOGGER.debug("ADDRESS:   " + depositBalanceDTO.getDepositDTO().getDepositAddress());
        LOGGER.debug("VOUT:      " + depositBalanceDTO.getBalanceDTO().getVout());
        LOGGER.debug("VIN:       " + depositBalanceDTO.getBalanceDTO().getVin());
        LOGGER.debug("BALANCE:   " + depositBalanceDTO.getBalanceDTO().getVout().subtract(depositBalanceDTO.getBalanceDTO().getVin()));
        LOGGER.debug("No. of TX: " + depositBalanceDTO.getBalanceDTO().getTransactionCount());

        checkBuilder.append(depositBalanceDTO.getDepositDTO().getCustomerAddress());
        checkBuilder.append("\t").append(depositBalanceDTO.getDepositDTO().getDepositAddress());
        checkBuilder.append("\t").append(depositBalanceDTO.getBalanceDTO().getVout());
        checkBuilder.append("\t").append(depositBalanceDTO.getBalanceDTO().getVin());
        checkBuilder.append("\t").append(depositBalanceDTO.getBalanceDTO().getVout().subtract(depositBalanceDTO.getBalanceDTO().getVin()));
        checkBuilder.append("\n");
      }

      LOGGER.debug("\n" + checkBuilder);
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
