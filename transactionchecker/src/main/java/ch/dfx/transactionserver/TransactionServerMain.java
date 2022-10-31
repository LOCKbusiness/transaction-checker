package ch.dfx.transactionserver;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.rmi.RemoteException;
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
import org.h2.tools.Server;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.handler.DefiWalletHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.ManagerRunnable;
import ch.dfx.process.data.ProcessInfoDTO;
import ch.dfx.process.stub.ProcessInfoService;
import ch.dfx.transactionserver.builder.DatabaseBuilder;
import ch.dfx.transactionserver.database.DatabaseRunnable;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class TransactionServerMain implements ProcessInfoService {
  private static final Logger LOGGER = LogManager.getLogger(TransactionServerMain.class);

  public static final String IDENTIFIER = "transactionserver";

  // ...
  private final String network;

  private final H2DBManager databaseManager;

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
      boolean isCompact = Stream.of(args).anyMatch(a -> "--compact".equals(a));
      boolean isInitialSetup = Stream.of(args).anyMatch(a -> "--initialsetup".equals(a));
      boolean isServerOnly = Stream.of(args).anyMatch(a -> "--serveronly".equals(a));

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

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
        transactionServer.execute(network, isMainnet, isServerOnly);
      }
    } catch (Throwable t) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
      LOGGER.error("Fatal Error", t);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public TransactionServerMain(@Nonnull String network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();
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
      startServer();

      DatabaseBuilder databaseBuilder = new DatabaseBuilder(databaseManager);
      databaseBuilder.build();

      shutdown();
    }
  }

  /**
   * 
   */
  private void execute(
      @Nonnull String network,
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
      startRMI();
      startServer();

      // ...
      loadWallet();

      // ...
      int runPeriodDatabase = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_DATABASE, 30);
      int runPeriodAPI = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_API, 60);

      LOGGER.debug("run period database: " + runPeriodDatabase);
      LOGGER.debug("run period api:      " + runPeriodAPI);

      if (30 <= runPeriodDatabase) {
        DatabaseRunnable databaseRunnable = new DatabaseRunnable(databaseManager, getProcessLockfile(), isServerOnly);
        SchedulerProvider.getInstance().add(databaseRunnable, 5, runPeriodDatabase, TimeUnit.SECONDS);
      }

      if (10 <= runPeriodAPI) {
        ManagerRunnable managerRunnable = new ManagerRunnable(databaseManager, network, isServerOnly);
        SchedulerProvider.getInstance().add(managerRunnable, 15, runPeriodAPI, TimeUnit.SECONDS);
      }
    }
  }

  /**
   * 
   */
  private void shutdown() {
    LOGGER.debug("shutdown()");

    unloadWallet();

    SchedulerProvider.getInstance().shutdown();

    stopServer();
    stopRMI();

    deleteProcessLockfile();

    LOGGER.debug("finish");

    LogManager.shutdown();
  }

  /**
   * 
   */
  private void loadWallet() throws DfxException {
    LOGGER.debug("loadWallet()");

    String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
    DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    DefiWalletHandler walletHandler = new DefiWalletHandler(dataProvider);
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

      DefiWalletHandler walletHandler = new DefiWalletHandler(dataProvider);
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
  private void startRMI() throws DfxException {
    LOGGER.debug("startRMI()");

    try {
      ProcessInfoService processInfoServiceStub = (ProcessInfoService) UnicastRemoteObject.exportObject(this, 0);
      int rmiPort = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RMI_PORT, -1);

      Registry registry = LocateRegistry.createRegistry(rmiPort);
      registry.bind(ProcessInfoService.class.getSimpleName(), processInfoServiceStub);

      LOGGER.info("================================");
      LOGGER.info("RMI started: RMI Port " + rmiPort);
      LOGGER.info("================================");
    } catch (Exception e) {
      throw new DfxException("startRMI", e);
    }
  }

  /**
   * 
   */
  private void stopRMI() {
    LOGGER.debug("stopRMI()");

    try {
      int rmiPort = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RMI_PORT, -1);

      Registry registry = LocateRegistry.getRegistry(rmiPort);
      registry.unbind(ProcessInfoService.class.getSimpleName());

      LOGGER.info("===========");
      LOGGER.info("RMI stopped");
      LOGGER.info("===========");
    } catch (Exception e) {
      LOGGER.error("stopRMI", e);
    }
  }

  /**
   * 
   */
  private void startServer() throws DfxException {
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
  private void stopServer() {
    LOGGER.debug("stopServer()");

    tcpServer.stop();

    LOGGER.info("==========================");
    LOGGER.info("Transaction Server stopped");
    LOGGER.info("==========================");
  }

  /**
   * 
   */
  @Override
  public ProcessInfoDTO getProcessInfoDTO() throws RemoteException {
    ProcessInfoDTO processInfoDTO = new ProcessInfoDTO();

    // Memory ...
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

    processInfoDTO.setHeapInitSize(heapMemoryUsage.getInit());
    processInfoDTO.setHeapMaxSize(heapMemoryUsage.getMax());
    processInfoDTO.setHeapUsedSize(heapMemoryUsage.getUsed());
    processInfoDTO.setHeapCommittedSize(heapMemoryUsage.getCommitted());

    // Disk ...
    File file = new File(".");
    processInfoDTO.setDiskTotalSpace(file.getTotalSpace());
    processInfoDTO.setDiskUsableSpace(file.getUsableSpace());
    processInfoDTO.setDiskFreeSpace(file.getFreeSpace());

    return processInfoDTO;
  }
}
