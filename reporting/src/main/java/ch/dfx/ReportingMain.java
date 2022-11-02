package ch.dfx;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.transactionserver.scheduler.SchedulerProvider;

/**
 * 
 */
public class ReportingMain {
  private static final Logger LOGGER = LogManager.getLogger(ReportingMain.class);

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
      System.setProperty("logFilename", "reporting-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      int runPeriodReport = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RUN_PERIOD_REPORT, 600);
      LOGGER.debug("run period report: " + runPeriodReport);

      if (60 <= runPeriodReport) {
        Reporting reporting = new Reporting();
        SchedulerProvider.getInstance().add(reporting, 5, runPeriodReport, TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
