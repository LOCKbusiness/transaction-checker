package ch.dfx.supervision;

import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventCollector;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.config.ReportingConfigEnum;

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
      TransactionCheckerUtils.setupGlobalProvider(network, environment, args);

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
    String vaultId1 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ID, "");
    List<String> checkRatio1List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT1_CHECK_RATIO);
    DefiVaultManager vaultManager1 = new DefiVaultManager(messageEventProvider, vaultId1, checkRatio1List);

    String vaultId2 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ID, "");
    List<String> checkRatio2List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT2_CHECK_RATIO);
    DefiVaultManager vaultManager2 = new DefiVaultManager(messageEventProvider, vaultId2, checkRatio2List);

    String vaultId3 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ID, "");
    List<String> checkRatio3List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT3_CHECK_RATIO);
    DefiVaultManager vaultManager3 = new DefiVaultManager(messageEventProvider, vaultId3, checkRatio3List);

    vaultManager1.checkCollateralizationRatio();
    vaultManager2.checkCollateralizationRatio();
    vaultManager3.checkCollateralizationRatio();

//    vaultManager2.checkDFIRatio();
  }
}
