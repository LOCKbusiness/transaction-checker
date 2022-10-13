package ch.dfx.message;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class MessageHandlerMain {
  private static final Logger LOGGER = LogManager.getLogger(MessageHandlerMain.class);

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
      System.setProperty("logFilename", "messagehandler-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      MessageHandler messageHandler = new MessageHandler(dataProvider);
      String signedMessage = messageHandler.signMessage(
          "0400000001a8d1d47b70c71af9be820aa28cac5f255a558eb16258e04038df6c7d64ab84400100000000ffffffff02d10000000000000016001433c1e60a5a5c2907b94032284f746c4c141e95ef003fdef50500000000160014233b41533e850221bc97c46d62da13f3ff7b9b440000000000");
      LOGGER.debug("Signed Message: " + signedMessage);

      Boolean isValid = messageHandler.verifyMessage(signedMessage, "Hello World");
      LOGGER.debug("Message is valid: " + isValid);

    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
