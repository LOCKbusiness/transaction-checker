package ch.dfx.defichain;

import java.util.concurrent.TimeUnit;
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
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventCollector;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class DefichainWatchdogMain {
  private static final Logger LOGGER = LogManager.getLogger(DefichainWatchdogMain.class);

  public static final String IDENTIFIER = "defichain";

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
      TransactionCheckerUtils.setupGlobalProvider(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      MessageEventBus.getInstance().register(new MessageEventCollector());

      // ...
      DefichainWatchdogMain defichainWatchdogMain = new DefichainWatchdogMain();
      defichainWatchdogMain.watchdog();

      // ...
      String startMessage = "[Defichain Watchdog] Process is running";
      LOGGER.info(startMessage);

      defichainWatchdogMain.sendTelegramMessage(startMessage);

      // ...
      while (true) {
        defichainWatchdogMain.runProcess();
        Thread.sleep(5 * 60 * 1000);
      }
    } catch (Throwable t) {
      LOGGER.error("Fatal Error", t);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public DefichainWatchdogMain() {
    this.messageEventCollector = new MessageEventCollector();
    this.messageEventProvider = new MessageEventProvider(messageEventCollector);
  }

  /**
   * 
   */
  private void watchdog() throws DfxException {
    LOGGER.debug("watchdog");

    try {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

      // ...
      setupMessageEventHandling();
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

    int runPeriodMessageEvent = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_MESSAGE_EVENT, 60);

    if (60 <= runPeriodMessageEvent) {
      SchedulerProvider.getInstance().add(messageEventProvider, 60, runPeriodMessageEvent, TimeUnit.SECONDS);
    }
  }

  /**
   * 
   */
  private void shutdown() {
    LOGGER.debug("shutdown()");

    // ...
    String shutdownMessage = "[Defichain Watchdog] Process shutdown";
    LOGGER.info(shutdownMessage);

    sendTelegramMessage(shutdownMessage);

    // ...
    LogManager.shutdown();
  }

  /**
   * 
   */
  private void runProcess() {
    LOGGER.debug("runProcess");

    try {
      String watchdogExecutable = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DEFICHAIN_WATCHDOG_EXECUTABLE);

      if (null != watchdogExecutable) {
        LOGGER.debug("Start process: " + watchdogExecutable);

        ProcessBuilder processBuilder = new ProcessBuilder(watchdogExecutable.split("\\s+"));
        processBuilder.inheritIO();
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // ...
        int exitCode = process.waitFor();
        LOGGER.debug("... Process exit code: " + exitCode);

        String stopMessage = "[Defichain] Unexpected stop, trying to restart now";
        LOGGER.error(stopMessage);

        sendTelegramMessage(stopMessage);
      }
    } catch (Throwable t) {
      String errorMessage = "[Defichain] Unexpected exception stop";
      sendTelegramMessage(errorMessage);

      LOGGER.error("Fatal Error", t);
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
