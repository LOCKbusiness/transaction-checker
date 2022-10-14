package ch.dfx.httpserver;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.httpserver.handler.SignInHandler;
import ch.dfx.httpserver.handler.TransactionHandler;

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
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "httpserver-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-httpserver.xml");

      Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
      ((DefaultShutdownCallbackRegistry) factory.getShutdownCallbackRegistry()).stop();

      // ...
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

      // ...
      httpServer =
          ServerBootstrap.bootstrap()
              .setListenerPort(PORT)
              .setServerInfo("My HTTP Testserver")
              .registerHandler("/v1/auth/sign-in", new SignInHandler())
              .registerHandler("/v1/transaction/open", new TransactionHandler())
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
