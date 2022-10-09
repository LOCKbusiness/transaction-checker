package ch.dfx.lockbusiness.stakingbalances.defichain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.DefiListAccountHistoryData;
import ch.dfx.defichain.data.transaction.DefiTransactionVinData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * Test Address: df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9
 * Test TxId: 65c924f92946b3ef1088ad507c658f638bb0425f6a469c615c942f0d93c31836
 */
public class DefichainBalanceHandlerMain {
  private static final Logger LOGGER = LogManager.getLogger(DefichainBalanceHandlerMain.class);

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
      System.setProperty("logFilename", "defichainbalancehandler-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-transactionchecker.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment, args);

      // ...
      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      Long blockCount = dataProvider.getBlockCount();
      LOGGER.debug("Block Count: " + blockCount);

      // ...
      // TODO: Only for testing purposes ...
      String testAddress = "df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9";
      LOGGER.debug("Test Address: " + testAddress);

      // ...
      String walletName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
      LOGGER.debug("Wallet Name: " + walletName);

      // ...
      List<DefiListAccountHistoryData> accountHistoryDataList =
          dataProvider.listAccountHistory(walletName, testAddress, blockCount, 1l);

      DefiListAccountHistoryData accountHistoryData = accountHistoryDataList.get(0);

      // ...
      String txid = accountHistoryData.getTxid();

      // TODO: Only for testing purposes ...
      txid = "65c924f92946b3ef1088ad507c658f638bb0425f6a469c615c942f0d93c31836";
      LOGGER.debug("Test TxId: " + txid);

      DefichainBalanceHandler balanceHandler = new DefichainBalanceHandler(dataProvider);
      balanceHandler.setup(testAddress);

      // ...
      List<String> txidList = Arrays.asList(txid);

      while (0 < txidList.size()) {
        List<DefiTransactionVinData> transactionVinDataList = balanceHandler.calculateBalance(txidList);
        txidList = new ArrayList<>(transactionVinDataList.stream().map(data -> data.getTxid()).collect(Collectors.toSet()));
      }

      writeCheckList(balanceHandler);

      // ...
      BigDecimal completeVinBalance = balanceHandler.getCompleteVinBalance();
      BigDecimal completeVoutBalance = balanceHandler.getCompleteVoutBalance();
      BigDecimal completeBalance = completeVinBalance.subtract(completeVoutBalance);

      LOGGER.debug("Complete vin Balance:  " + completeVinBalance);
      LOGGER.debug("Complete vout Balance: " + completeVoutBalance);
      LOGGER.debug("Complete Balance:      " + completeBalance);

    } catch (Exception e) {
      LOGGER.error("Fatal Error ...", e);
    }
  }

  /**
   * 
   */
  private static void writeCheckList(DefichainBalanceHandler balanceHandler) throws DfxException {
    LOGGER.debug("writeCheckList() ...");

    File checkFile = new File("logs", "defichain-transactions.txt");
    LOGGER.debug("Check File: " + checkFile.getAbsolutePath());

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(checkFile))) {
      List<String> checkList = balanceHandler.getCheckList();
      LOGGER.debug("Check List Size: " + checkList.size());

      for (String check : checkList) {
        writer.append(check).append("\n");
      }
    } catch (Exception e) {
      throw new DfxException("writeCheckList", e);
    }
  }
}
