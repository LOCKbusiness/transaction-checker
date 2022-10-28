package ch.dfx.httpserver;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.httpserver.handler.APISignInHandler;
import ch.dfx.httpserver.handler.APITransactionRequestHandler;
import ch.dfx.httpserver.handler.APIWithdrawalRequestHandler;

/**
 * Only for testing purposes, simulate API Calls ...
 */
public class HttpServerMain {
  private static final Logger LOGGER = LogManager.getLogger(HttpServerMain.class);

  private static final int PORT = 8080;

  private static HttpServer httpServer = null;

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
      System.setProperty("logFilename", "httpserver-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-httpserver.xml");

      Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
      ((DefaultShutdownCallbackRegistry) factory.getShutdownCallbackRegistry()).stop();

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

      // ...
      APISignInHandler apiSignInHandler = new APISignInHandler();
      APITransactionRequestHandler apiTransactionRequestHandler = new APITransactionRequestHandler();
      APIWithdrawalRequestHandler apiWithdrawalRequestHandler = new APIWithdrawalRequestHandler();

      // ...
      File[] transactionFileNameArray = new File[] {
          Path.of("data", "httpserver", "get", "transaction-20221028-145601-825128.json").toFile()
      };

      File[] withdrawalFileNameArray = new File[] {
          Path.of("data", "httpserver", "get", "withdrawal-20221028-145601-847129.json").toFile()
      };

      apiTransactionRequestHandler.setJSONFileArray(transactionFileNameArray);
      apiWithdrawalRequestHandler.setJSONFileArray(withdrawalFileNameArray);

      // ...
      httpServer =
          ServerBootstrap.bootstrap()
              .setListenerPort(PORT)
              .setServerInfo("My HTTP Testserver")
              .registerHandler("/v1/auth/sign-in", apiSignInHandler)
              .registerHandler("/v1/transaction/*", apiTransactionRequestHandler)
              .registerHandler("/v1/withdrawal/*", apiWithdrawalRequestHandler)
              .create();

      httpServer.start();

      LOGGER.info("=========================================");
      LOGGER.info("HTTP Server started: Port " + PORT);
      LOGGER.info("=========================================");
    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }

  /**
   * 
   */
  private static void shutdown() {
    LOGGER.debug("shutdown()");

    httpServer.stop();

    LOGGER.debug("finish");

    LogManager.shutdown();
  }
}
