package ch.dfx.transactionserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;

/**
 * 
 */
public class TransactionSupervisonMain {
  private static final Logger LOGGER = LogManager.getLogger(TransactionSupervisonMain.class);

  private PreparedStatement transactionSelectStatement = null;

  // ...
  private final H2DBManager databaseManager;

  private int diffCounter = 0;

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
      Optional<String> optionalStartblockArgument = Stream.of(args).filter(a -> a.startsWith("--startblock=")).findFirst();
      Optional<String> optionalEndblockArgument = Stream.of(args).filter(a -> a.startsWith("--endblock=")).findFirst();

      // ...
      String network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

      // ...
      System.setProperty("logFilename", "transactionsupervision-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2-transactionsupervision.xml");

      // ...
      TransactionCheckerUtils.loadConfigProperties(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      long startblock = -1;
      long endblock = -1;

      if (optionalStartblockArgument.isPresent()) {
        startblock = Long.parseLong(optionalStartblockArgument.get().split("=")[1]);
      }

      if (optionalEndblockArgument.isPresent()) {
        endblock = Long.parseLong(optionalEndblockArgument.get().split("=")[1]);
      }

      if (-1 != startblock
          && -1 == endblock) {
        DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();
        endblock = dataProvider.getBlockCount();
      }

      LOGGER.debug("Start Block: " + startblock);
      LOGGER.debug("End Block:   " + endblock);

      if (-1 != startblock
          && -1 != endblock
          && startblock < endblock) {
        TransactionSupervisonMain transactionSupervison = new TransactionSupervisonMain();
        transactionSupervison.checkTransaction(startblock, endblock);
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error" + e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public TransactionSupervisonMain() {
    this.databaseManager = new H2DBManagerImpl();
  }

  /**
   * 
   */
  private void checkTransaction(
      long startblock,
      long endblock) throws DfxException {
    LOGGER.trace("checkTransaction() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();
      openStatements(connection);

      DefiDataProvider dataProvider = TransactionCheckerUtils.createDefiDataProvider();

      long blockNumber = startblock;

      while (blockNumber++ < endblock) {
        List<String> transactionListFromChain = getTransactionListFromChain(dataProvider, blockNumber);
        List<String> transactionListFromDB = getTransactionListFromDB(blockNumber);

        LOGGER.debug("BLOCK: " + blockNumber + " / " + transactionListFromChain.size() + " / " + transactionListFromDB.size());

        if (!transactionListFromChain.equals(transactionListFromDB)) {
          diffCounter++;

          LOGGER.error("DIFF: " + transactionListFromChain);
          LOGGER.error("DIFF: " + transactionListFromDB);
        }
      }

      closeStatements();

      // ...
      String diffText = diffCounter + " differences found ...";

      if (0 == diffCounter) {
        LOGGER.debug(diffText);
      } else {
        LOGGER.error(diffText);
      }
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      String transactionSelectSql = "SELECT * FROM public.transaction WHERE block_number=?";
      transactionSelectStatement = connection.prepareStatement(transactionSelectSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      transactionSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private List<String> getTransactionListFromChain(
      @Nonnull DefiDataProvider dataProvider,
      @Nonnull Long blockNumber) throws DfxException {
    String blockHash = dataProvider.getBlockHash(blockNumber);
    DefiBlockData block = dataProvider.getBlock(blockHash);
    return block.getTx();
  }

  /**
   * 
   */
  private List<String> getTransactionListFromDB(@Nonnull Long blockNumber) throws DfxException {
    try {
      List<String> transactionIdList = new ArrayList<>();

      transactionSelectStatement.setInt(1, blockNumber.intValue());

      ResultSet resultSet = transactionSelectStatement.executeQuery();

      while (resultSet.next()) {
        transactionIdList.add(resultSet.getString(3));
      }

      resultSet.close();

      return transactionIdList;
    } catch (Exception e) {
      throw new DfxException("getTransactionListFromDB", e);
    }
  }
}
