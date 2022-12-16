package ch.dfx.transactionserver.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class DatabaseUpdater {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseUpdater.class);

  // ...
  private PreparedStatement updateTransactionStatement = null;

  private final H2DBManager databaseManager;

  /**
   * 
   */
  public DatabaseUpdater(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
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

      doUpdateTransaction(connection);

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
      String updateTransactionSql = "UPDATE public.transaction"
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
      updateTransactionStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
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
    } catch (Exception e) {
      throw new DfxException("doUpdateTransaction", e);
    }
  }
}
