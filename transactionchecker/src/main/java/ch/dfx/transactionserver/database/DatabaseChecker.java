package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DatabaseChecker {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseChecker.class);

  private final H2DBManager databaseManager;
  private final DefiDataProvider dataProvider;

  private PreparedStatement transactionSelectStatement = null;

  /**
   * 
   */
  public DatabaseChecker(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public boolean check() throws DfxException {
    LOGGER.debug("check()");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();
      openStatements(connection);

      long maxBlockNumber = getMaxBlockNumber(connection);
      long minBlockNumber = maxBlockNumber - 100;

      LOGGER.debug("Block Check: " + minBlockNumber + " to " + maxBlockNumber);

      for (long blockNumber = minBlockNumber; blockNumber <= maxBlockNumber; blockNumber++) {
        List<String> transactionListFromChain = getTransactionListFromChain(dataProvider, blockNumber);
        List<String> transactionListFromDB = getTransactionListFromDB(blockNumber);

        if (!transactionListFromChain.equals(transactionListFromDB)) {
          LOGGER.debug("Check Block: " + blockNumber + " / " + transactionListFromChain.size() + " / " + transactionListFromDB.size());
          LOGGER.debug("Transactions in Chain:    " + transactionListFromChain);
          LOGGER.debug("Transactions in Database: " + transactionListFromDB);
          cleanAll(connection, blockNumber);

          connection.commit();
          break;
        }
      }

      closeStatements();

      return checkBlockCount(connection);
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("check", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private boolean checkBlockCount(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("checkBlockCount()");

    long maxBlockNumber = getMaxBlockNumber(connection);
    long blockCount = dataProvider.getBlockCount();

    return maxBlockNumber == blockCount;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

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
    LOGGER.trace("closeStatements()");

    try {
      transactionSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private int getMaxBlockNumber(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getMaxBlockNumber()");

    String sqlSelect =
        new StringBuilder()
            .append("SELECT MAX(number) FROM public.block")
            .toString();

    return getMaxNumber(connection, sqlSelect);
  }

  /**
   * 
   */
  private int getMaxNumber(
      @Nonnull Connection connection,
      @Nonnull String sqlSelect) throws DfxException {
    LOGGER.trace("getMaxNumber()");

    try (Statement statement = connection.createStatement()) {
      int maxNumber = -1;

      ResultSet resultSet = statement.executeQuery(sqlSelect);

      if (resultSet.next()) {
        maxNumber = resultSet.getInt(1);
      }

      resultSet.close();

      if (-1 == maxNumber) {
        throw new DfxException("max number not found ...");
      }

      return maxNumber;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getMaxNumber", e);
    }
  }

  /**
   * 
   */
  private void cleanAll(
      @Nonnull Connection connection,
      @Nonnull Long blockNumber) throws DfxException {
    LOGGER.trace("cleanAll()");

    String addressTransactionInDeleteSql = "DELETE FROM public.address_transaction_in WHERE block_number >= " + blockNumber;
    clean(connection, addressTransactionInDeleteSql);

    String addressTransactionOutDeleteSql = "DELETE FROM public.address_transaction_out WHERE block_number >= " + blockNumber;
    clean(connection, addressTransactionOutDeleteSql);

    String transactionDeleteSql = "DELETE FROM public.transaction WHERE block_number >= " + blockNumber;
    clean(connection, transactionDeleteSql);

    String blockDeleteSql = "DELETE FROM public.block WHERE number >= " + blockNumber;
    clean(connection, blockDeleteSql);
  }

  /**
   * 
   */
  private void clean(
      @Nonnull Connection connection,
      @Nonnull String sqlDelete) throws DfxException {
    LOGGER.trace("clean()");

    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sqlDelete);
    } catch (Exception e) {
      throw new DfxException("clean", e);
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
