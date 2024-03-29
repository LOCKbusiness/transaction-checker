package ch.dfx.transactionserver;

import java.io.File;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.h2.tools.Server;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventCollector;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.common.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.defichain.handler.DefiWalletHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.ManagerRunnable;
import ch.dfx.process.ProcessInfoProvider;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.database.DatabaseRunnable;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;
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
      TransactionCheckerUtils.setupGlobalProvider(network, environment, args);

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

      DatabaseBlockHelper databaseBlockHelper = new DatabaseBlockHelper(network);
      DatabaseAddressHandler databaseAddressHandler = new DatabaseAddressHandler(network);

      Connection connection = databaseManager.openConnection();
      databaseBlockHelper.openStatements(connection);

      DatabaseBuilder databaseBuilder = new DatabaseBuilder(network, databaseBlockHelper, databaseAddressHandler);
      databaseBuilder.build(connection);

      databaseBlockHelper.closeStatements();
      databaseManager.closeConnection(connection);

      shutdown(true);
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
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(isServerOnly)));

      // ...
      setupMessageEventHandling();

      startDatabaseServer();

      // ...
      if (!isServerOnly) {
        loadWallet();
      }

      // ...
      int runPeriodDatabase = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.RUN_PERIOD_DATABASE, 30);
      int runPeriodAPI = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.RUN_PERIOD_API, 60);
      int runPeriodWatchdog = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.RUN_PERIOD_WATCHDOG, 600);

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
          SchedulerProvider.getInstance().add(managerRunnable, 120, runPeriodAPI, TimeUnit.SECONDS);
        }

        if (60 <= runPeriodWatchdog) {
          ProcessInfoProvider processInfoProvider = new ProcessInfoProvider();
          SchedulerProvider.getInstance().add(processInfoProvider, 15, runPeriodWatchdog, TimeUnit.SECONDS);
        }
      }

      // ...
      String startMessage = "[Transaction Check Server] Process is running";
      LOGGER.info(startMessage);

      sendTelegramMessage(startMessage);
    }
  }

  /**
   * 
   */
  private void setupMessageEventHandling() {
    LOGGER.debug("setupMessageEventHandling()");

    MessageEventBus.getInstance().register(messageEventCollector);

    int runPeriodMessageEvent = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.RUN_PERIOD_MESSAGE_EVENT, 60);

    if (60 <= runPeriodMessageEvent) {
      SchedulerProvider.getInstance().add(messageEventProvider, 30, runPeriodMessageEvent, TimeUnit.SECONDS);
    }
  }

  /**
   * 
   */
  private void shutdown(boolean isServerOnly) {
    LOGGER.debug("shutdown()");

    SchedulerProvider.getInstance().shutdown();

    if (!isServerOnly) {
      unloadWallet();
    }

    stopDatabaseServer();

    deleteProcessLockfile();

    // ...
    String shutdownMessage = "[Transaction Check Server] Process shutdown";
    LOGGER.info(shutdownMessage);

    sendTelegramMessage(shutdownMessage);

    // ...
    LogManager.shutdown();
  }

  /**
   * 
   */
  private void loadWallet() throws DfxException {
    LOGGER.debug("loadWallet()");

    if (NetworkEnum.STAGNET != network) {
      String wallet = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_NAME);

      if (null != wallet) {
        DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

        DefiWalletHandler walletHandler = new DefiWalletHandler(network, dataProvider);
        walletHandler.loadWallet(wallet);
      }
    } else {
      LOGGER.debug("Staging not allowed to load the wallet");
    }
  }

  /**
   * 
   */
  private void unloadWallet() {
    LOGGER.debug("unloadWallet()");

    try {
      if (NetworkEnum.STAGNET != network) {
        String wallet = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_NAME);

        if (null != wallet) {
          DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

          DefiWalletHandler walletHandler = new DefiWalletHandler(network, dataProvider);
          walletHandler.unloadWallet(wallet);
        }
      } else {
        LOGGER.debug("Staging not allowed to unload the wallet");
      }
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
      if (NetworkEnum.STAGNET != network) {
        String tcpPort = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_SERVER_TCP_PORT);
        String databaseDirectory = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_DIR);
        LOGGER.debug("TCP PORT: " + tcpPort);
        LOGGER.debug("DATABASE DIRECTORY: " + databaseDirectory);

        tcpServer = Server.createTcpServer("-tcpPort", tcpPort, "-baseDir", databaseDirectory, "-tcpAllowOthers");
        tcpServer.start();

        LOGGER.debug("=====================================");
        LOGGER.info("Transaction Server started: Port " + tcpPort);
        LOGGER.debug("=====================================");
      } else {
        LOGGER.debug("Staging not allowed to start the Transaction Server");
      }
    } catch (Exception e) {
      throw new DfxException("startDatabaseServer", e);
    }
  }

  /**
   * 
   */
  private void stopDatabaseServer() {
    LOGGER.debug("stopDatabaseServer()");

    if (NetworkEnum.STAGNET != network) {
      if (null != tcpServer) {
        tcpServer.stop();
      }

      LOGGER.debug("==========================");
      LOGGER.info("Transaction Server stopped");
      LOGGER.debug("==========================");
    } else {
      LOGGER.debug("Staging not allowed to stop the Transaction Server");
    }
  }

  /**
   * 
   */
  private void sendTelegramMessage(@Nonnull String message) {
    MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));
    messageEventProvider.run();
  }
}
