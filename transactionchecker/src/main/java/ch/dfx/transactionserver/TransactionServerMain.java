package ch.dfx.transactionserver;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.h2.tools.Server;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.database.DatabaseRunnable;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class TransactionServerMain {
  private static final Logger LOGGER = LogManager.getLogger(TransactionServerMain.class);

  private static File LOCK_FILE = null;

  private static Server tcpServer = null;

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
      boolean isCompact = Stream.of(args).anyMatch(a -> "--compact".equals(a));
      boolean isInitialSetup = Stream.of(args).anyMatch(a -> "--initialsetup".equals(a));
      boolean isServerOnly = Stream.of(args).anyMatch(a -> "--serveronly".equals(a));

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      LOCK_FILE = new File(
          "transactionserver"
              + "." + network
              + "." + TransactionCheckerUtils.getEnvironment().name().toLowerCase()
              + ".lock");

      // ...
      System.setProperty("logFilename", "transactionserver-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-transactionserver.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      if (isCompact) {
        compact();
      } else if (isInitialSetup) {
        initialSetup();
      } else {
        execute(isServerOnly);
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private static void compact() throws DfxException {
    LOGGER.debug("compact");

    try {
      if (createLockFile()) {
        H2DBManager.getInstance().compact();
      }
    } finally {
      deleteLockFile();
    }
  }

  /**
   * 
   */
  private static void initialSetup() throws DfxException {
    LOGGER.debug("initialSetup");

    if (createLockFile()) {
      startServer();

      DatabaseBuilder databaseBuilder = new DatabaseBuilder();
      databaseBuilder.build();

      shutdown();
    }
  }

  /**
   * 
   */
  private static void execute(boolean isServerOnly) throws DfxException {
    LOGGER.debug("execute");

    if (createLockFile()) {
      // ...
      Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
      ((DefaultShutdownCallbackRegistry) factory.getShutdownCallbackRegistry()).stop();

      // ...
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

      // ...
      startServer();

      // ...
      DatabaseRunnable databaseRunnable = new DatabaseRunnable(LOCK_FILE, isServerOnly);
      SchedulerProvider.getInstance().add(databaseRunnable, 5, 30, TimeUnit.SECONDS);

//      ManagerRunnable managerRunnable = new ManagerRunnable(isServerOnly);
//      SchedulerProvider.getInstance().add(managerRunnable, 15, 60, TimeUnit.SECONDS);
    }
  }

  /**
   * 
   */
  private static void shutdown() {
    LOGGER.debug("shutdown()");

    SchedulerProvider.getInstance().shutdown();

    stopServer();

    deleteLockFile();

    LOGGER.debug("finish");

    LogManager.shutdown();
  }

  /**
   * 
   */
  private static boolean createLockFile() throws DfxException {
    LOGGER.debug("createLockFile()");

    boolean lockFileCreated;

    try {
      LOGGER.debug("Lockfile: " + LOCK_FILE.getAbsolutePath());

      if (LOCK_FILE.exists()) {
        lockFileCreated = false;
        LOGGER.error("Another Server still running: " + LOCK_FILE.getAbsolutePath());
      } else {
        lockFileCreated = LOCK_FILE.createNewFile();
      }

      return lockFileCreated;
    } catch (Exception e) {
      throw new DfxException("createLockFile", e);
    }
  }

  /**
   * 
   */
  private static void deleteLockFile() {
    LOGGER.debug("deleteLockFile()");

    if (LOCK_FILE.exists()) {
      LOCK_FILE.delete();
    }
  }

  /**
   * 
   */
  private static void startServer() throws DfxException {
    LOGGER.debug("startServer()");

    try {
      String tcpPort = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_SERVER_TCP_PORT);
      String databaseDirectory = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_DB_DIR);
      LOGGER.debug("TCP PORT: " + tcpPort);
      LOGGER.debug("DATABASE DIRECTORY: " + databaseDirectory);

      tcpServer = Server.createTcpServer("-tcpPort", tcpPort, "-baseDir", databaseDirectory, "-tcpAllowOthers");
      tcpServer.start();

      LOGGER.info("=========================================");
      LOGGER.info("Transaction Server started: TCP Port " + tcpPort);
      LOGGER.info("=========================================");
    } catch (Exception e) {
      throw new DfxException("startServer", e);
    }
  }

  /**
   * 
   */
  private static void stopServer() {
    LOGGER.debug("stopServer()");

    tcpServer.stop();

    LOGGER.info("==========================");
    LOGGER.info("Transaction Server stopped");
    LOGGER.info("==========================");
  }
}
