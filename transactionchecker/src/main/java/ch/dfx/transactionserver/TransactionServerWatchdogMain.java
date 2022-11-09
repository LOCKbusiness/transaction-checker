package ch.dfx.transactionserver;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
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
  private Registry registry = null;

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
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("Start");

      TransactionServerWatchdogMain transactionServerWatchdogMain = new TransactionServerWatchdogMain(network);
      transactionServerWatchdogMain.watchdog();

      while (true) {
        transactionServerWatchdogMain.runProcess();
        Thread.sleep(30 * 1000);
      }
    } catch (

    Throwable t) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
      LOGGER.error("Fatal Error", t);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public TransactionServerWatchdogMain(@Nonnull NetworkEnum network) {
    this.network = network;
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
//      LOGGER.info("==============================");
//      LOGGER.info("HTTP Server started: Port " + PORT);
//      LOGGER.info("==============================");
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
//      LOGGER.info("===================");
//      LOGGER.info("HTTP Server stopped");
//      LOGGER.info("===================");
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
      int rmiPort = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RMI_PORT, -1);

      registry = LocateRegistry.createRegistry(rmiPort);

      ProcessInfoService processInfoService =
          (ProcessInfoService) UnicastRemoteObject.exportObject(new ProcessInfoServiceImpl(), 0);

      registry.rebind(ProcessInfoService.class.getSimpleName(), processInfoService);

      LOGGER.info("=============================");
      LOGGER.info("RMI Server started: Port " + rmiPort);
      LOGGER.info("=============================");
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

      LOGGER.info("===========");
      LOGGER.info("RMI stopped");
      LOGGER.info("===========");
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

    LOGGER.debug("finish");

    LogManager.shutdown();
  }

  /**
   * 
   */
  private void runProcess() {
    LOGGER.debug("runProcess");

    try {
      String watchdogExecutable = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.WATCHDOG_EXECUTABLE);
      LOGGER.debug("Start process: " + watchdogExecutable);

      // ...
      ProcessBuilder processBuilder = new ProcessBuilder(watchdogExecutable.split("\\s+"));
      processBuilder.inheritIO();
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      LOGGER.debug("Process is running ...");

      int exitCode = process.waitFor();
      LOGGER.debug("... Process exit code: " + exitCode);

      deleteTransactionServerProcessLockfile();

      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
    } catch (Throwable t) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
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
}
