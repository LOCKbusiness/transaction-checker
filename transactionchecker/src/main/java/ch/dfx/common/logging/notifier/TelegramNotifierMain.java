package ch.dfx.common.logging.notifier;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;

/**
 * 
 */
public class TelegramNotifierMain {
  private static final Logger LOGGER = LogManager.getLogger(TelegramNotifierMain.class);

  private static final String IDENTIFIER = "telegramnotifier";

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

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
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      String telegramToken = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN);
      String telegramChatId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID);

      if (null != telegramToken
          && null != telegramChatId) {
        TelegramNotifier telegramNotifier = new TelegramNotifier();
        telegramNotifier.sendMessage(telegramToken, telegramChatId, "Hello World");
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }
}
