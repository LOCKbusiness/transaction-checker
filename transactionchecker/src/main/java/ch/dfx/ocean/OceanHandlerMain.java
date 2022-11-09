package ch.dfx.ocean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.ocean.data.ListTransactionsDTO;
import ch.dfx.ocean.data.TransactionsDTO;
import ch.dfx.ocean.data.TransactionsDetailDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;

/**
 * The application is used to check the stored deposit balances.
 * 
 * The application fetches all deposit addresses and determines the balance via the Ocean API.
 * The results can be compared with the stored values.
 * 
 * Example:
 * --testnet
 * --from-ocean
 * --password=[PASSWORD]
 * 
 * or
 * 
 * --testnet
 * --from-file
 * --password=[PASSWORD]
 */
public class OceanHandlerMain {
  private static final Logger LOGGER = LogManager.getLogger(OceanHandlerMain.class);

  private static final String IDENTIFIER = "oceanhandler";

  // ...
  private final NetworkEnum network;

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public static void main(String[] args) throws DfxException {

    // ...
    boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
    boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
    boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));
    boolean isFromOcean = Stream.of(args).anyMatch(a -> "--from-ocean".equals(a));
    boolean isFromFile = Stream.of(args).anyMatch(a -> "--from-file".equals(a));

    if (!isFromOcean
        && !isFromFile) {
      System.out.println(createUsageInfo());
      System.exit(-1);
    }

    // ...
    NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
    EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

    // ...
    System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
    TransactionCheckerUtils.initLog4j("log4j2.xml");

    // ...
    TransactionCheckerUtils.loadConfigProperties(network, environment);

    // ...
    OceanHandlerMain oceanHandler = new OceanHandlerMain(network);

    // ...
    List<DepositDTO> depositDTOList = oceanHandler.getDepositDTOList();

    for (DepositDTO depositDTO : depositDTOList) {
      String depositAddress = depositDTO.getDepositAddress();

      if (null != depositAddress) {
        File jsonFile = new File("logs", "ocean-" + depositAddress + ".json");

        if (isFromOcean) {
          oceanHandler.readFromOcean(depositAddress, jsonFile);
        } else if (isFromFile) {
          oceanHandler.readFromFile(depositAddress, jsonFile);
        }
      }
    }
  }

  /**
   * 
   */
  public OceanHandlerMain(NetworkEnum network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  private static String createUsageInfo() {
    StringBuilder usageBuilder = new StringBuilder();

    usageBuilder.append("usage: options").append("\n");
    usageBuilder.append("\n");
    usageBuilder.append("Options:").append("\n");
    usageBuilder.append("--mainnet").append("\n");
    usageBuilder.append("\tRun program with the mainnet.").append("\n");
    usageBuilder.append("\n");
    usageBuilder.append("--testnet").append("\n");
    usageBuilder.append("\tRun program with the testnet.").append("\n");
    usageBuilder.append("\tDefault, if neither --mainnet nor --testnet are specified.").append("\n");
    usageBuilder.append("\n");
    usageBuilder.append("--from-ocean").append("\n");
    usageBuilder.append("\tRead the data from the ocean API.").append("\n");
    usageBuilder.append("\n");
    usageBuilder.append("--from-file").append("\n");
    usageBuilder.append("\tRead the data from the previously stored JSON file.").append("\n");

    return usageBuilder.toString();
  }

  /**
   * 
   */
  private List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.debug("getDepositDTOList() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();
      databaseHelper.openStatements(connection);

      List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOList();

      databaseHelper.closeStatements();

      return depositDTOList;
    } catch (Exception e) {
      throw new DfxException("getDepositDTOList", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void readFromOcean(
      @Nonnull String testAddress,
      @Nonnull File jsonFile) throws DfxException {
    LOGGER.debug("readFromOcean() ...");

    // ...
    OceanHandler oceanHandler = new OceanHandler();
    oceanHandler.setup(network, testAddress);

    oceanHandler.openWriter(jsonFile);

    String next = oceanHandler.webcall(null);

    while (null != next) {
      next = oceanHandler.webcall(next);
    }

    oceanHandler.closeWriter();

    BigDecimal vinBalance = oceanHandler.getVinBalance();
    BigDecimal voutBalance = oceanHandler.getVoutBalance();
    BigDecimal balance = voutBalance.subtract(vinBalance);

    LOGGER.debug("vin Balance: " + vinBalance);
    LOGGER.debug("vout Balance: " + voutBalance);
    LOGGER.debug("Balance: " + balance);
  }

  /**
   * 
   */
  private void readFromFile(
      @Nonnull String testAddress,
      @Nonnull File jsonFile) throws DfxException {
    LOGGER.debug("readFromFile() ...");

    OceanHandler oceanHandler = new OceanHandler();
    oceanHandler.setup(network, testAddress);

    ListTransactionsDTO transactionsDTOList = oceanHandler.readFromFile(jsonFile);

    BigDecimal vinBalance = oceanHandler.getVinBalance();
    BigDecimal voutBalance = oceanHandler.getVoutBalance();
    BigDecimal balance = voutBalance.subtract(vinBalance);

    LOGGER.debug("vin Balance: " + vinBalance);
    LOGGER.debug("vout Balance: " + voutBalance);
    LOGGER.debug("Balance: " + balance);

    analyze(transactionsDTOList);
  }

  /**
   * 
   */
  private void analyze(ListTransactionsDTO transactionsDTOList) throws DfxException {
    LOGGER.debug("analyze() ...");

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("logs", "ocean-transactions.txt")))) {
      List<String> txidList = new ArrayList<>();

      // ...
      for (TransactionsDTO transactionsDTO : transactionsDTOList.getDatalist()) {
        for (TransactionsDetailDTO transactionsDetailDTO : transactionsDTO.getData()) {
          String txid = transactionsDetailDTO.getTxid();
          BigDecimal value = transactionsDetailDTO.getValue();

          if (!txidList.contains(txid)) {
            txidList.add(txid + ";" + value);
          }
        }
      }

      // ...
      for (String txid : txidList) {
        writer.append(txid).append("\n");
      }
    } catch (Exception e) {
      throw new DfxException("analyze", e);
    }
  }
}
