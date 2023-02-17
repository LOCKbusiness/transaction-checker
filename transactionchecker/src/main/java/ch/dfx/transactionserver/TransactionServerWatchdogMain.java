package ch.dfx.transactionserver;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventCollector;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.common.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.defichain.handler.DefiStatusHandler;
import ch.dfx.process.ProcessInfoService;
import ch.dfx.process.ProcessInfoServiceImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class TransactionServerWatchdogMain {
  private static final Logger LOGGER = LogManager.getLogger(TransactionServerWatchdogMain.class);

  private static final String IDENTIFIER = "transactionserver-watchdog";

//  private static final int PORT = 8080;

  // ...
  private final NetworkEnum network;

//  private HttpServer httpServer = null;
  private static Registry registry = null;
  private static ProcessInfoService processInfoService = null;

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
      TransactionCheckerUtils.initLog4j("log4j2-transactionserver.xml");

      Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
      ((DefaultShutdownCallbackRegistry) factory.getShutdownCallbackRegistry()).stop();

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment, args);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      MessageEventBus.getInstance().register(new MessageEventCollector());

      // ...
      TransactionServerWatchdogMain transactionServerWatchdogMain = new TransactionServerWatchdogMain(network);
      transactionServerWatchdogMain.watchdog();

      // ...
      String startMessage = "[Transaction Check Server Watchdog] Process is running";
      LOGGER.info(startMessage);

      transactionServerWatchdogMain.sendTelegramMessage(startMessage);

      // ...
      while (true) {
        transactionServerWatchdogMain.waitForDefichainInSync();
        transactionServerWatchdogMain.runProcess();

        Thread.sleep(15 * 60 * 1000);
      }
    } catch (Throwable t) {
      LOGGER.error("Fatal Error", t);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public TransactionServerWatchdogMain(@Nonnull NetworkEnum network) {
    this.network = network;

    this.messageEventCollector = new MessageEventCollector();
    this.messageEventProvider = new MessageEventProvider(messageEventCollector);
  }

  /**
   * 
   */
  private void watchdog() throws DfxException {
    LOGGER.debug("watchdog");

    try {
      if (createProcessLockfile()) {
        // ...
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

        // ...
        setupMessageEventHandling();

        // ...
//        startHttpServer();
        startRMI();
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("watchdog", e);
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
      SchedulerProvider.getInstance().add(messageEventProvider, 60, runPeriodMessageEvent, TimeUnit.SECONDS);
    }
  }

  /**
   * Maybe for future use: HTTP Server instead of RMI - we will see ...
   */
//  private synchronized void startHttpServer() throws DfxException {
//    LOGGER.debug("startHttpServer");
//
//    try {
//      URL url = TransactionServerWatchdogMain.class.getResource("/my.keystore");
//
//      SSLContext sslcontext =
//          SSLContexts.custom()
//              .loadKeyMaterial(url, "secret".toCharArray(), "secret".toCharArray())
//              .build();
//
//      SocketConfig socketConfig =
//          SocketConfig.custom()
//              .setSoTimeout(15000)
//              .setTcpNoDelay(true)
//              .build();
//
//      httpServer =
//          ServerBootstrap.bootstrap()
//              .setLocalAddress(InetAddress.getByName("localhost"))
//              .setListenerPort(PORT)
//              .setSocketConfig(socketConfig)
//              .setSslContext(sslcontext)
//              .setServerInfo("HTTP Server/1.0")
//              .registerHandler("/processinfo", new ProcessInfoHandler())
//              .create();
//
//      httpServer.start();
//
//      LOGGER.debug("==============================");
//      LOGGER.info("HTTP Server started: Port " + PORT);
//      LOGGER.debug("==============================");
//    } catch (Throwable t) {
//      throw new DfxException("startHttpServer", t);
//    }
//  }

  /**
   * Maybe for future use: HTTP Server instead of RMI - we will see ...
   */
//  private synchronized void stopHttpServer() {
//    LOGGER.debug("stopHttpServer");
//
//    try {
//      if (null != httpServer) {
//        httpServer.stop();
//      }
//
//      LOGGER.debug("===================");
//      LOGGER.info("HTTP Server stopped");
//      LOGGER.debug("===================");
//    } catch (Throwable t) {
//      LOGGER.error("stopHttpServer", t);
//    }
//  }

  /**
   * 
   */
  private synchronized void startRMI() throws DfxException {
    LOGGER.debug("startRMI");

    try {
      int rmiPort = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.RMI_PORT, -1);

      registry = LocateRegistry.createRegistry(rmiPort);

      processInfoService =
          (ProcessInfoService) UnicastRemoteObject.exportObject(new ProcessInfoServiceImpl(), 0);

      registry.rebind(ProcessInfoService.class.getSimpleName(), processInfoService);

      LOGGER.debug("=============================");
      LOGGER.info("RMI Server started: Port " + rmiPort);
      LOGGER.debug("=============================");
    } catch (Throwable t) {
      throw new DfxException("startRMI", t);
    }
  }

  /**
   * 
   */
  private synchronized void stopRMI() {
    LOGGER.debug("stopRMI");

    try {
      if (null != registry) {
        registry.unbind(ProcessInfoService.class.getSimpleName());
      }

      LOGGER.debug("===========");
      LOGGER.info("RMI stopped");
      LOGGER.debug("===========");
    } catch (Throwable t) {
      LOGGER.error("stopRMI", t);
    }
  }

  /**
   * 
   */
  private void shutdown() {
    LOGGER.debug("shutdown()");

    SchedulerProvider.getInstance().shutdown();

//    stopHttpServer();
    stopRMI();

    deleteProcessLockfile();

    // ...
    String shutdownMessage = "[Transaction Check Server Watchdog] Process shutdown";
    LOGGER.info(shutdownMessage);

    sendTelegramMessage(shutdownMessage);

    // ...
    LogManager.shutdown();
  }

  /**
   * 
   */
  private void waitForDefichainInSync() throws DfxException {
    LOGGER.debug("waitForDefichainInSync");

    try {
      DefiStatusHandler statusHandler = new DefiStatusHandler();

      boolean isInSync = false;

      while (!isInSync) {
        isInSync = statusHandler.isInSync();

        String syncMessage = "[Transaction Check Server Watchdog] Defichain inSync? " + isInSync;
        LOGGER.error(syncMessage);

        sendTelegramMessage(syncMessage);

        if (!isInSync) {
          Thread.sleep(1 * 60 * 1000);
        }
      }
    } catch (Exception e) {
      throw new DfxException("waitForDefichainInSync", e);
    }
  }

  /**
   * 
   */
  private void runProcess() {
    LOGGER.debug("runProcess");

    try {
      String watchdogExecutable = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.WATCHDOG_EXECUTABLE);

      if (null != watchdogExecutable) {
        LOGGER.debug("Start process: " + watchdogExecutable);

        // ...
        ProcessBuilder processBuilder = new ProcessBuilder(watchdogExecutable.split("\\s+"));
        processBuilder.inheritIO();
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // ...
        int exitCode = process.waitFor();
        LOGGER.debug("... Process exit code: " + exitCode);

        deleteTransactionServerProcessLockfile();

        String stopMessage = "[Transaction Check Server] Unexpected stop, trying to restart now";
        LOGGER.error(stopMessage);

        sendTelegramMessage(stopMessage);
      }
    } catch (Throwable t) {
      String errorMessage = "[Transaction Check Server] Unexpected exception stop";
      sendTelegramMessage(errorMessage);

      LOGGER.error("Fatal Error", t);
    }
  }

  /**
   * 
   */
  private void deleteTransactionServerProcessLockfile() {
    LOGGER.debug("deleteTransactionServerLockfile()");

    String transactionServerProcessLockFilename = TransactionCheckerUtils.getProcessLockFilename(TransactionServerMain.IDENTIFIER, network);
    File transactionServerProcessLockfile = new File(transactionServerProcessLockFilename);

    if (transactionServerProcessLockfile.exists()) {
      transactionServerProcessLockfile.delete();
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
        LOGGER.error("Another Watchdog still running: " + processLockFile.getAbsolutePath());
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

    File processLockfile = getProcessLockfile();

    if (processLockfile.exists()) {
      processLockfile.delete();
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
  private void sendTelegramMessage(@Nonnull String message) {
    MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));
    messageEventProvider.run();
  }
}
