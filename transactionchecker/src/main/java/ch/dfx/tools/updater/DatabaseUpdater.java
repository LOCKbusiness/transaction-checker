package ch.dfx.tools.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class DatabaseUpdater {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseUpdater.class);

  // ...
  private PreparedStatement updateBlockStatement = null;
  private PreparedStatement updateTransactionStatement = null;

  private final H2DBManager databaseManager;
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public DatabaseUpdater(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void update() throws DfxException {
    LOGGER.trace("update()");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      openStatements(connection);

      // doUpdateBlock(connection);
      // doUpdateTransaction(connection);

      closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("update", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[DatabaseUpdater] runtime: " + (System.currentTimeMillis() - startTime));
    }

  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // ...
      String updateBlockSql =
          "UPDATE public.block"
              + " SET timestamp=?"
              + " WHERE number=?";
      updateBlockStatement = connection.prepareStatement(updateBlockSql);

      // ...
      String updateTransactionSql =
          "UPDATE public.transaction"
              + " SET custom_type_code =?"
              + " WHERE block_number=? AND number=?";
      updateTransactionStatement = connection.prepareStatement(updateTransactionSql);
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
      updateBlockStatement.close();
      updateTransactionStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void doUpdateBlock(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("doUpdateBlock()");

    try {
      DefiBlockData blockData = getBlockData(connection);

      while (null != blockData) {
        long blockNumber = blockData.getHeight();

        updateBlockStatement.setLong(1, blockData.getMediantime());
        updateBlockStatement.setLong(2, blockNumber);
        updateBlockStatement.addBatch();

        String nextblockhash = blockData.getNextblockhash();

        if (null == nextblockhash) {
          blockData = null;
        } else {
          blockData = dataProvider.getBlock(nextblockhash);
        }

        if (0 == blockNumber % 1000) {
          updateBlockStatement.executeBatch();
          connection.commit();
          LOGGER.debug("Block: " + blockNumber);
        }
      }

      updateBlockStatement.executeBatch();
      connection.commit();
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("doUpdateBlock", e);
    }
  }

  /**
   * 
   */
  private @Nullable DefiBlockData getBlockData(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getBlockData()");

    try {
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT MIN(number) FROM public.block WHERE timestamp IS NULL");

      DefiBlockData blockData = null;

      if (resultSet.next()) {
        long minBlockNumber = resultSet.getLong(1);
        String blockHash = dataProvider.getBlockHash(minBlockNumber);
        blockData = dataProvider.getBlock(blockHash);
      }

      resultSet.close();
      statement.close();

      return blockData;
    } catch (Exception e) {
      throw new DfxException("getBlockData", e);
    }
  }

  /**
   * 
   */
  private void doUpdateTransaction(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("doUpdateTransaction()");

    File inputFile = new File("data", "transaction.csv");

    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
      String line = reader.readLine();

      while (null != (line = reader.readLine())) {
        String[] dataArray = line.replaceAll("\"", "").split("\\,");
        int blockNumber = Integer.parseInt(dataArray[0].trim());

        if (0 <= blockNumber) {
          int transactionNumber = Integer.parseInt(dataArray[1].trim());
          String customTypeCode = dataArray[2];

          updateTransactionStatement.setString(1, customTypeCode);
          updateTransactionStatement.setInt(2, blockNumber);
          updateTransactionStatement.setInt(3, transactionNumber);
          updateTransactionStatement.addBatch();

          if (0 == blockNumber % 1000) {
            updateTransactionStatement.executeBatch();
            connection.commit();
            LOGGER.debug("Block: " + blockNumber);
          }
        }
      }

      updateTransactionStatement.executeBatch();
      connection.commit();
    } catch (Exception e) {
      throw new DfxException("doUpdateTransaction", e);
    }
  }
}
