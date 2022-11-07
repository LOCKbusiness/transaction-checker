package ch.dfx;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class ReportingMain {
  private static final Logger LOGGER = LogManager.getLogger(ReportingMain.class);

  private static final String IDENTIFIER = "reporting";

  private final H2DBManager databaseManager;

  /**
   * 
   */
  public static void main(String[] args) throws Exception {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));

      // ...
      String network = (isMainnet ? "mainnet" : "testnet");
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      ReportingMain reporting = new ReportingMain();
      reporting.execute();

    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public ReportingMain() {
    this.databaseManager = new H2DBManagerImpl();
  }

  /**
   * 
   */
  private void execute() {
    LOGGER.debug("execute");

    int runPeriodReport = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_REPORT, 600);
    LOGGER.debug("run period report: " + runPeriodReport);

    if (60 <= runPeriodReport) {
      ReportingRunnable reporting = new ReportingRunnable(databaseManager);
      SchedulerProvider.getInstance().add(reporting, 5, runPeriodReport, TimeUnit.SECONDS);
    }
  }
}
