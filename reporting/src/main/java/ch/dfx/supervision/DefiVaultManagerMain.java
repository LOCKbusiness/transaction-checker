package ch.dfx.supervision;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventCollector;
import ch.dfx.logging.MessageEventProvider;

/**
 * 
 */
public class DefiVaultManagerMain {
  private static final Logger LOGGER = LogManager.getLogger(DefiVaultManagerMain.class);

  private static final String IDENTIFIER = "vaultmanager";

  // ...
  private final MessageEventCollector messageEventCollector;
  private final MessageEventProvider messageEventProvider;

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
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment);

      // ...
      DefiVaultManagerMain vaultManagerMain = new DefiVaultManagerMain();
      vaultManagerMain.execute();
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }

  /**
   * 
   */
  public DefiVaultManagerMain() {
    this.messageEventCollector = new MessageEventCollector();
    this.messageEventProvider = new MessageEventProvider(messageEventCollector);
  }

  /**
   * 
   */
  private void execute() throws Exception {
    LOGGER.debug("execute");

    // ...
    MessageEventBus.getInstance().register(messageEventCollector);

    // ...
    DefiVaultManager vaultManager = new DefiVaultManager(messageEventProvider);
    vaultManager.checkRatio();
  }
}
