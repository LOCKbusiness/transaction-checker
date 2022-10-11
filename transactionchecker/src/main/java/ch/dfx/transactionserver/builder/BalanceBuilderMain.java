package ch.dfx.transactionserver.builder;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;

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

  private static final DecimalFormat germanDecimalFormat = new DecimalFormat("#,##0.00000000");

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
      System.setProperty("logFilename", "balance-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-balance.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment, args);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      BalanceBuilder balanceBuilder = new BalanceBuilder();
      List<BalanceDTO> balanceDTOList = balanceBuilder.build();

      StringBuilder checkBuilder = new StringBuilder();

      for (BalanceDTO balanceDTO : balanceDTOList) {
        LiquidityDTO liquidityDTO = balanceDTO.getLiquidityDTO();
        DepositDTO depositDTO = balanceDTO.getDepositDTO();

        if (null != liquidityDTO) {
          checkBuilder.append("");
          checkBuilder.append("\t").append(liquidityDTO.getAddress());
          checkBuilder.append("\t").append(format(balanceDTO.getVout().subtract(balanceDTO.getVin())));
          checkBuilder.append("\t").append(format(balanceDTO.getVout()));
          checkBuilder.append("\t").append(format(balanceDTO.getVin()));
          checkBuilder.append("\t").append(balanceDTO.getTransactionCount());
          checkBuilder.append("\n");
          checkBuilder.append("\n");
        }

        if (null != depositDTO) {
          checkBuilder.append(depositDTO.getCustomerAddress());
          checkBuilder.append("\t").append(depositDTO.getDepositAddress());
          checkBuilder.append("\t").append(format(balanceDTO.getVout().subtract(balanceDTO.getVin())));
          checkBuilder.append("\t").append(format(balanceDTO.getVout()));
          checkBuilder.append("\t").append(format(balanceDTO.getVin()));
          checkBuilder.append("\t").append(balanceDTO.getTransactionCount());
          checkBuilder.append("\n");
        }
      }

      LOGGER.debug("\n" + checkBuilder);
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private static String format(@Nonnull BigDecimal value) {
    return germanDecimalFormat.format(value);
  }
}
