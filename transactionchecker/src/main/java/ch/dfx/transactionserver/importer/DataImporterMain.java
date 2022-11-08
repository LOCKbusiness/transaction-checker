package ch.dfx.transactionserver.importer;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DataImporterMain {
  private static final Logger LOGGER = LogManager.getLogger(DataImporterMain.class);

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

      // ...
      String network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "dataimporter-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      DataImporter dataImporter = new DataImporter(network);
      dataImporter.execute();
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }
}
