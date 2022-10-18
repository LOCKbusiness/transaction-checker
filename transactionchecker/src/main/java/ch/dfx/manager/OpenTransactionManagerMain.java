package ch.dfx.manager;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.handler.DefiWalletHandler;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class OpenTransactionManagerMain {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerMain.class);

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
      String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      DefiWalletHandler walletHandler = new DefiWalletHandler(dataProvider);
      walletHandler.loadWallet(wallet);

      // ...
      OpenTransactionManager transactionManager = new OpenTransactionManager();
      transactionManager.execute();
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }
}
