package ch.dfx.manager;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.defichain.handler.DefiWalletHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;

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
      boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
      boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", "opentransactionmanager-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment, args);

      // ...
      String wallet = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_NAME);

      if (null != wallet) {
        DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

        DefiWalletHandler walletHandler = new DefiWalletHandler(network, dataProvider);
        walletHandler.loadWallet(wallet);

        // ...
        ApiAccessHandler apiAccessHandler = new ApiAccessHandlerImpl(network);
        apiAccessHandler.signIn();

        // ...
        H2DBManager databaseManager = new H2DBManagerImpl();

        // ...
        OpenTransactionManager transactionManager =
            new OpenTransactionManager(network, apiAccessHandler, databaseManager, dataProvider);
        transactionManager.execute();
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }
}
