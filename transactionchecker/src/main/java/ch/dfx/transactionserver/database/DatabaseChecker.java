package ch.dfx.transactionserver.database;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

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
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DatabaseChecker {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseChecker.class);

  private final NetworkEnum network;

  private final H2DBManager databaseManager;
  private final DefiDataProvider dataProvider;

  private PreparedStatement transactionSelectStatement = null;

  /**
   * 
   */
  public DatabaseChecker(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
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

      LOGGER.debug("[DatabaseChecker] Block Check: " + minBlockNumber + " to " + maxBlockNumber);

      for (long blockNumber = minBlockNumber; blockNumber <= maxBlockNumber; blockNumber++) {
        List<String> transactionListFromChain = getTransactionListFromChain(dataProvider, blockNumber);
        List<String> transactionListFromDB = getTransactionListFromDB(blockNumber);

        if (!transactionListFromChain.equals(transactionListFromDB)) {
          LOGGER.debug("[DatabaseChecker] Check Block: " + blockNumber + " / " + transactionListFromChain.size() + " / " + transactionListFromDB.size());
          LOGGER.debug("[DatabaseChecker] Transactions in Chain:    " + transactionListFromChain);
          LOGGER.debug("[DatabaseChecker] Transactions in Database: " + transactionListFromDB);
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

      LOGGER.debug("[DatabaseChecker] runtime: " + (System.currentTimeMillis() - startTime));
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
      String transactionSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction WHERE block_number=?";
      transactionSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionSelectSql));
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
            .append(DatabaseUtils.replaceSchema(network, "SELECT MAX(number) FROM " + TOKEN_PUBLIC_SCHEMA + ".block"))
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

    String customTransactionAccountToAccountInDeleteSql =
        "DELETE FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in WHERE block_number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountInDeleteSql));

    String customTransactionAccountToAccountOutDeleteSql =
        "DELETE FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out WHERE block_number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountOutDeleteSql));

    String addressTransactionInDeleteSql = "DELETE FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in WHERE block_number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, addressTransactionInDeleteSql));

    String addressTransactionOutDeleteSql = "DELETE FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, addressTransactionOutDeleteSql));

    String transactionDeleteSql = "DELETE FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction WHERE block_number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, transactionDeleteSql));

    String blockDeleteSql = "DELETE FROM " + TOKEN_PUBLIC_SCHEMA + ".block WHERE number >= " + blockNumber;
    clean(connection, DatabaseUtils.replaceSchema(network, blockDeleteSql));
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
