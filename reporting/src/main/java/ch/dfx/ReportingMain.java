package ch.dfx;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventCollector;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.supervision.DefiManagerRunnable;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class ReportingMain {
  private static final Logger LOGGER = LogManager.getLogger(ReportingMain.class);

  private static final String IDENTIFIER = "reporting";

  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private final MessageEventCollector messageEventCollector;
  private final MessageEventProvider messageEventProvider;

  /**
   * 
   */
  public static void main(String[] args) throws Exception {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
      boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
      boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      ReportingMain reporting = new ReportingMain(network);
      reporting.execute();

    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public ReportingMain(@Nonnull NetworkEnum network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();

    this.messageEventCollector = new MessageEventCollector();
    this.messageEventProvider = new MessageEventProvider(messageEventCollector);
  }

  /**
   * 
   */
  private void execute() {
    LOGGER.debug("execute");

    // ...
    MessageEventBus.getInstance().register(messageEventCollector);

    // ...
    int runPeriodReport = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_REPORT, 600);
    int runPeriodDefiManager = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_DEFIMANAGER, 60);

    LOGGER.debug("run period report: " + runPeriodReport);
    LOGGER.debug("run period defi manager: " + runPeriodDefiManager);

    if (60 <= runPeriodReport) {
      ReportingRunnable reporting = new ReportingRunnable(network, databaseManager);
      SchedulerProvider.getInstance().add(reporting, 5, runPeriodReport, TimeUnit.SECONDS);
    }

    if (30 <= runPeriodDefiManager) {
      DefiManagerRunnable defiManagerRunnable = new DefiManagerRunnable(messageEventProvider);
      SchedulerProvider.getInstance().add(defiManagerRunnable, 30, runPeriodDefiManager, TimeUnit.SECONDS);
    }
  }
}
