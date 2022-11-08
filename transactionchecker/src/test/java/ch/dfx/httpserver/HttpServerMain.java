package ch.dfx.httpserver;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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
      boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
      boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));

      // ...
      String network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
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
      File[] transactionFileNameArray = readAllFiles("transaction");
      File[] withdrawalFileNameArray = readAllFiles("withdrawal");

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
  private static File[] readAllFiles(@Nonnull String type) {
    File httpserverGetDirectory = Path.of("data", "httpserver", "get").toFile();

    return httpserverGetDirectory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(type) && name.endsWith(".json");
      }
    });
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
