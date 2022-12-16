package ch.dfx.transactionserver;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.h2.tools.Server;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.handler.DefiWalletHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventCollector;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.manager.ManagerRunnable;
import ch.dfx.process.ProcessInfoProvider;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.database.DatabaseRunnable;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class TransactionServerMain {
  private static final Logger LOGGER = LogManager.getLogger(TransactionServerMain.class);

  public static final String IDENTIFIER = "transactionserver";

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final MessageEventCollector messageEventCollector;
  private final MessageEventProvider messageEventProvider;

  // ...
  private Server tcpServer = null;

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
      boolean isCompact = Stream.of(args).anyMatch(a -> "--compact".equals(a));
      boolean isInitialSetup = Stream.of(args).anyMatch(a -> "--initialsetup".equals(a));
      boolean isServerOnly = Stream.of(args).anyMatch(a -> "--serveronly".equals(a));

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2-transactionserver.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      TransactionServerMain transactionServer = new TransactionServerMain(network);

      // ...
      if (isCompact) {
        transactionServer.compact();
      } else if (isInitialSetup) {
        transactionServer.initialSetup();
      } else {
        transactionServer.execute(isMainnet, isServerOnly);
      }
    } catch (Throwable t) {
      LOGGER.error("Fatal Error", t);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public TransactionServerMain(@Nonnull NetworkEnum network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();

    this.messageEventCollector = new MessageEventCollector();
    this.messageEventProvider = new MessageEventProvider(messageEventCollector);
  }

  /**
   * 
   */
  private void compact() throws DfxException {
    LOGGER.debug("compact");

    try {
      if (createProcessLockfile()) {
        databaseManager.compact();
      }
    } finally {
      deleteProcessLockfile();
    }
  }

  /**
   * 
   */
  private void initialSetup() throws DfxException {
    LOGGER.debug("initialSetup");

    if (createProcessLockfile()) {
      startDatabaseServer();

      DatabaseBuilder databaseBuilder = new DatabaseBuilder(network, databaseManager);
      databaseBuilder.build();

      shutdown();
    }
  }

  /**
   * 
   */
  private void execute(
      boolean isMainnet,
      boolean isServerOnly) throws DfxException {
    LOGGER.debug("execute");

    if (createProcessLockfile()) {
      // ...
      Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
      ((DefaultShutdownCallbackRegistry) factory.getShutdownCallbackRegistry()).stop();

      // ...
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

      // ...
      setupMessageEventHandling();

      startDatabaseServer();

      // ...
      if (NetworkEnum.STAGNET != network) {
        loadWallet();
      }

      // ...
      int runPeriodDatabase = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_DATABASE, 30);
      int runPeriodAPI = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_API, 60);
      int runPeriodWatchdog = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_WATCHDOG, 600);

      LOGGER.debug("run period database: " + runPeriodDatabase);
      LOGGER.debug("run period api:      " + runPeriodAPI);
      LOGGER.debug("run period watchdog: " + runPeriodWatchdog);

      if (30 <= runPeriodDatabase) {
        DatabaseRunnable databaseRunnable = new DatabaseRunnable(network, databaseManager, getProcessLockfile(), isServerOnly);
        SchedulerProvider.getInstance().add(databaseRunnable, 5, runPeriodDatabase, TimeUnit.SECONDS);
      }

      if (!isServerOnly) {
        if (10 <= runPeriodAPI) {
          ManagerRunnable managerRunnable = new ManagerRunnable(network, databaseManager);
          SchedulerProvider.getInstance().add(managerRunnable, 15, runPeriodAPI, TimeUnit.SECONDS);
        }

        if (60 <= runPeriodWatchdog) {
          ProcessInfoProvider processInfoProvider = new ProcessInfoProvider();
          SchedulerProvider.getInstance().add(processInfoProvider, 10, runPeriodWatchdog, TimeUnit.SECONDS);
        }
      }

      // ...
      String startMessage = "[Transaction Check Server] Process is running";
      MessageEventBus.getInstance().postEvent(new MessageEvent(startMessage));
      LOGGER.info(startMessage);
    }
  }

  /**
   * 
   */
  private void setupMessageEventHandling() {
    LOGGER.debug("setupMessageEventHandling()");

    MessageEventBus.getInstance().register(messageEventCollector);

    int runPeriodMessageEvent = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_MESSAGE_EVENT, 60);

    if (60 <= runPeriodMessageEvent) {
      SchedulerProvider.getInstance().add(messageEventProvider, 30, runPeriodMessageEvent, TimeUnit.SECONDS);
    }
  }

  /**
   * 
   */
  private void shutdown() {
    LOGGER.debug("shutdown()");

    SchedulerProvider.getInstance().shutdown();

    if (NetworkEnum.STAGNET != network) {
      unloadWallet();
    }

    stopDatabaseServer();

    deleteProcessLockfile();

    // ...
    String shutdownMessage = "[Transaction Check Server] Process shutdown";
    MessageEventBus.getInstance().postEvent(new MessageEvent(shutdownMessage));
    LOGGER.info(shutdownMessage);
    messageEventProvider.run();

    // ...
    LogManager.shutdown();
  }

  /**
   * 
   */
  private void loadWallet() throws DfxException {
    LOGGER.debug("loadWallet()");

    String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
    DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    DefiWalletHandler walletHandler = new DefiWalletHandler(network, dataProvider);
    walletHandler.loadWallet(wallet);
  }

  /**
   * 
   */
  private void unloadWallet() {
    LOGGER.debug("unloadWallet()");

    try {
      String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      DefiWalletHandler walletHandler = new DefiWalletHandler(network, dataProvider);
      walletHandler.unloadWallet(wallet);
    } catch (Exception e) {
      LOGGER.error("unloadWallet", e);
    }
  }

  /**
   * 
   */
  private boolean createProcessLockfile() throws DfxException {
    LOGGER.debug("createProcessLockfile()");

    boolean lockFileCreated;

    try {
      File processLockFile = getProcessLockfile();

      LOGGER.debug("Process lockfile: " + processLockFile.getAbsolutePath());

      if (processLockFile.exists()) {
        lockFileCreated = false;
        LOGGER.error("Another Server still running: " + processLockFile.getAbsolutePath());
      } else {
        lockFileCreated = processLockFile.createNewFile();
      }

      return lockFileCreated;
    } catch (Exception e) {
      throw new DfxException("createProcessLockfile", e);
    }
  }

  /**
   * 
   */
  private void deleteProcessLockfile() {
    LOGGER.debug("deleteProcessLockfile()");

    File processLockFile = getProcessLockfile();

    if (processLockFile.exists()) {
      processLockFile.delete();
    }
  }

  /**
   * 
   */
  private File getProcessLockfile() {
    String processLockFilename = TransactionCheckerUtils.getProcessLockFilename(IDENTIFIER, network);
    return new File(processLockFilename);
  }

  /**
   * 
   */
  private void startDatabaseServer() throws DfxException {
    LOGGER.debug("startDatabaseServer()");

    try {
      String tcpPort = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_SERVER_TCP_PORT);
      String databaseDirectory = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_DB_DIR);
      LOGGER.debug("TCP PORT: " + tcpPort);
      LOGGER.debug("DATABASE DIRECTORY: " + databaseDirectory);

      tcpServer = Server.createTcpServer("-tcpPort", tcpPort, "-baseDir", databaseDirectory, "-tcpAllowOthers");
      tcpServer.start();

      LOGGER.debug("=====================================");
      LOGGER.info("Transaction Server started: Port " + tcpPort);
      LOGGER.debug("=====================================");
    } catch (Exception e) {
      throw new DfxException("startDatabaseServer", e);
    }
  }

  /**
   * 
   */
  private void stopDatabaseServer() {
    LOGGER.debug("stopDatabaseServer()");

    if (null != tcpServer) {
      tcpServer.stop();
    }

    LOGGER.debug("==========================");
    LOGGER.info("Transaction Server stopped");
    LOGGER.debug("==========================");
  }
}
