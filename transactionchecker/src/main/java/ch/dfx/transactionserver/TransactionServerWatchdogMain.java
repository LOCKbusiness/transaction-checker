package ch.dfx.transactionserver;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.process.data.ProcessInfoDTO;
import ch.dfx.process.stub.ProcessInfoService;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class TransactionServerWatchdogMain implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(TransactionServerWatchdogMain.class);

  public static final String IDENTIFIER = "transactionserver-watchdog";

  // ...
  private static final long KB = 1024;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;

  private static final long MEMORY_WATERMARK_WARNING = 80;
  private static final long MEMORY_WATERMARK_CRITICAL = 90;

  private static final long DISK_WATERMARK_WARNING = 80;
  private static final long DISK_WATERMARK_CRITICAL = 90;

  // ...
  private final String network;

  private Registry registry = null;

  private boolean processIsRunning = false;

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
        transactionServerWatchdogMain.runProcess(network);
        Thread.sleep(30 * 1000);
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
  public TransactionServerWatchdogMain(@Nonnull String network) {
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
        String rmiHost = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.RMI_HOST);
        int rmiPort = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RMI_PORT, -1);

        registry = LocateRegistry.getRegistry(rmiHost, rmiPort);

        LOGGER.info("================================");
        LOGGER.info("RMI started: RMI Port " + rmiPort);
        LOGGER.info("================================");

        // ...
        int runPeriodWatchdog = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_WATCHDOG, 300);

        LOGGER.debug("run period watchdog: " + runPeriodWatchdog);

        if (30 <= runPeriodWatchdog) {
          SchedulerProvider.getInstance().add(this, 30, runPeriodWatchdog, TimeUnit.SECONDS);
        }
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
  private void shutdown() {
    LOGGER.debug("shutdown()");

    SchedulerProvider.getInstance().shutdown();

    deleteProcessLockfile();

    LOGGER.debug("finish");

    LogManager.shutdown();
  }

  /**
   * 
   */
  private void runProcess(String network) {
    LOGGER.debug("runProcess");

    try {
      String javaExecutable = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.WATCHDOG_JAVA);
      String executableJar = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.WATCHDOG_EXECUTABLE_JAR);
      String executableParams = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.WATCHDOG_EXECUTABLE_PARAMS);

      List<String> processBuilderParameterList = new ArrayList<>();

      processBuilderParameterList.add(javaExecutable);
      processBuilderParameterList.addAll(Arrays.asList(executableParams.split("\\|")));
      processBuilderParameterList.add("-jar");
      processBuilderParameterList.add(executableJar);
      processBuilderParameterList.add("--" + network);

      ProcessBuilder processBuilder = new ProcessBuilder(processBuilderParameterList);

      // ...
      LOGGER.debug("START PROCESS: " + executableJar);
      Process process = processBuilder.start();

      processIsRunning = true;
      LOGGER.debug("process is running: " + processIsRunning);

      int exitCode = process.waitFor();
      LOGGER.debug("PROCESS EXIT CODE: " + exitCode);

      deleteTransactionServerProcessLockfile();

      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
    } catch (Throwable t) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
      LOGGER.error("Fatal Error", t);
    }

    processIsRunning = false;
    LOGGER.debug("process is running: " + processIsRunning);
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

  @Override
  public void run() {
    LOGGER.debug("run");

    try {
      if (processIsRunning) {
        ProcessInfoService processInfoServiceStub = (ProcessInfoService) registry.lookup(ProcessInfoService.class.getSimpleName());
        ProcessInfoDTO processInfoDTO = processInfoServiceStub.getProcessInfoDTO();

        LOGGER.debug("Process Info: " + processInfoDTO);
        LOGGER.debug("");

        // ...
        long heapMaxSize = processInfoDTO.getHeapMaxSize();
        long heapUsedSize = processInfoDTO.getHeapUsedSize();
        long heapCapacity = (heapUsedSize * 100 / heapMaxSize);

        LOGGER.debug("Memory Max Size:  " + (heapMaxSize / MB));
        LOGGER.debug("Memory Used Size: " + (heapUsedSize / MB));
        LOGGER.debug("Memory Capacity:  " + heapCapacity + "%");

        if (heapCapacity >= MEMORY_WATERMARK_CRITICAL) {
          // SEND MESSAGE: ...
          LOGGER.error("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
        } else if (heapCapacity >= MEMORY_WATERMARK_WARNING) {
          // SEND MESSAGE: ...
          LOGGER.warn("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
        }

        // ...
        long diskTotalSpace = processInfoDTO.getDiskTotalSpace();
        long diskFreeSpace = processInfoDTO.getDiskFreeSpace();
        long diskUsedSpace = diskTotalSpace - diskFreeSpace;
        long diskCapacity = (diskUsedSpace * 100 / diskTotalSpace);

        LOGGER.debug("Disk Max Space:  " + (diskTotalSpace / GB));
        LOGGER.debug("Disk Used Space: " + (diskUsedSpace / GB));
        LOGGER.debug("Disk Capacity:   " + diskCapacity + "%");

        if (diskCapacity >= DISK_WATERMARK_CRITICAL) {
          // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
          LOGGER.error("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
        } else if (diskCapacity >= DISK_WATERMARK_WARNING) {
          // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
          LOGGER.warn("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
        }
      }
    } catch (Throwable t) {
      LOGGER.error("run", t);
    }
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean isProcessing() {
    return true;
  }
}
