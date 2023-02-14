package ch.dfx.supervision;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
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
    String vaultId1 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT1_ID, "");
    String checkRatio1 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT1_CHECK_RATIO, "");
    DefiVaultManager vaultManager1 = new DefiVaultManager(messageEventProvider, vaultId1, checkRatio1);

    String vaultId2 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT2_ID, "");
    String checkRatio2 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT2_CHECK_RATIO, "");
    DefiVaultManager vaultManager2 = new DefiVaultManager(messageEventProvider, vaultId2, checkRatio2);

    String vaultId3 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT3_ID, "");
    String checkRatio3 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT3_CHECK_RATIO, "");
    DefiVaultManager vaultManager3 = new DefiVaultManager(messageEventProvider, vaultId3, checkRatio3);

    vaultManager1.checkRatio();
    vaultManager2.checkRatio();
    vaultManager3.checkRatio();
  }
}
