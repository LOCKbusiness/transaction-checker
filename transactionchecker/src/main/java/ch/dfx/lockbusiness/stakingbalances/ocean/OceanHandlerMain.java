package ch.dfx.lockbusiness.stakingbalances.ocean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.ListTransactionsData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionsData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionsDetailData;

/**
 * ============================================================================
 * There are two different functions available:
 * 
 * readFromOcean:
 * Get all transactions via the ocean API. Store as JSON data.
 * 
 * readFromFile:
 * Get all transactions from the JSON data.
 * 
 * ----------------------------------------------------------------------------
 * Example: https://defiscan.live/address/df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9
 * 
 * OceanHandlerMain: vin Balance: 123128.69506586
 * OceanHandlerMain: vout Balance: 123128.79506162
 * OceanHandlerMain: Balance: 0.09999576
 * ============================================================================
 */
public class OceanHandlerMain {
  private static final Logger LOGGER = LogManager.getLogger(OceanHandlerMain.class);

  /**
   * 
   */
  public static void main(String[] args) throws DfxException {
    String environment = PayoutManagerUtils.getEnvironment().name().toLowerCase();

    // ...
    System.setProperty("logFilename", "oceanhandler-" + environment);
    PayoutManagerUtils.initLog4j("log4j2-payoutmanager.xml");

    // ...
    String testAddress = "df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9";
    File jsonFile = new File("logs", "ocean-" + testAddress + ".json");

    // readFromOcean(testAddress, jsonFile);
    // readFromFile(testAddress, jsonFile);
  }

  /**
   * 
   */
  private static void readFromOcean(
      String testAddress,
      File jsonFile) throws DfxException {
    // ...
    OceanHandler oceanHandler = new OceanHandler();
    oceanHandler.setup(testAddress);

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
  private static void readFromFile(
      String testAddress,
      File jsonFile) throws DfxException {
    OceanHandler oceanHandler = new OceanHandler();
    oceanHandler.setup(testAddress);

    ListTransactionsData transactionsDataList = oceanHandler.readFromFile(jsonFile);

    BigDecimal vinBalance = oceanHandler.getVinBalance();
    BigDecimal voutBalance = oceanHandler.getVoutBalance();
    BigDecimal balance = voutBalance.subtract(vinBalance);

    LOGGER.debug("vin Balance: " + vinBalance);
    LOGGER.debug("vout Balance: " + voutBalance);
    LOGGER.debug("Balance: " + balance);

    analyze(transactionsDataList);
  }

  /**
   * 
   */
  private static void analyze(ListTransactionsData transactionsDataList) throws DfxException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("logs", "ocean-transactions.txt")))) {
      List<String> txidList = new ArrayList<>();

      // ...
      for (TransactionsData transactionsData : transactionsDataList.getDatalist()) {
        for (TransactionsDetailData transactionsDetailData : transactionsData.getData()) {
          String txid = transactionsDetailData.getTxid();
          BigDecimal value = transactionsDetailData.getValue();

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
