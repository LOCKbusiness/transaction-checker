package ch.dfx.transactionserver.builder;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingDTO;

/**
 * 
 */
public class StakingBuilderMain {
  private static final Logger LOGGER = LogManager.getLogger(StakingBuilderMain.class);

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
      System.setProperty("logFilename", "balancebuilder-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      StakingBuilder stakingBuilder = new StakingBuilder();
      List<StakingDTO> stakingDTOList = stakingBuilder.build();

      StringBuilder checkBuilder = new StringBuilder();

      for (StakingDTO stakingDTO : stakingDTOList) {
        DepositDTO depositDTO = stakingDTO.getDepositDTO();

        checkBuilder.append("");
        checkBuilder.append("\t").append(null == depositDTO ? stakingDTO.getLiquidityAddressNumber() : depositDTO.getLiquidityAddress());
        checkBuilder.append("\t").append(null == depositDTO ? stakingDTO.getDepositAddressNumber() : depositDTO.getDepositAddress());
        checkBuilder.append("\t").append(null == depositDTO ? stakingDTO.getCustomerAddressNumber() : depositDTO.getCustomerAddress());
        checkBuilder.append("\t").append(format(stakingDTO.getVout().subtract(stakingDTO.getVin())));
        checkBuilder.append("\t").append(format(stakingDTO.getVout()));
        checkBuilder.append("\t").append(format(stakingDTO.getVin()));
        checkBuilder.append("\n");
        checkBuilder.append("\n");
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
