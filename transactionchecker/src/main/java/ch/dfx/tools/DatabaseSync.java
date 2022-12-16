package ch.dfx.tools;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.data.DatabaseConnectionData;
import ch.dfx.tools.data.DatabaseData;
import ch.dfx.transactionserver.database.DatabaseUtils;

/**
 * Copy table content from remote to local:
 * 
 * - API_DUPLICATE_CHECK
 */
public class DatabaseSync extends DatabaseTool {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseSync.class);

  private final NetworkEnum network;

  /**
   * 
   */
  public DatabaseSync(@Nonnull NetworkEnum network) {
    this.network = network;
  }

  /**
   * 
   */
  public void sync() throws DfxException {
    LOGGER.trace("sync()");

    Connection localConnection = null;
    Connection remoteConnection = null;

    try {
      DatabaseConnectionData databaseConnectionData = getDatabaseConnectionData();

      DatabaseData localDatabaseData = databaseConnectionData.getLocalDatabaseData();
      localConnection = openConnection(localDatabaseData);

      DatabaseData remoteDatabaseData = databaseConnectionData.getRemoteDatabaseData();
      remoteConnection = openConnection(remoteDatabaseData);

      doSync(localConnection, remoteConnection);
    } finally {
      closeConnection(localConnection);
      closeConnection(remoteConnection);
    }
  }

  /**
   * 
   */
  private void doSync(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection) throws DfxException {
    LOGGER.trace("doSync()");

    syncDuplicateCheck(localConnection, remoteConnection);
  }

  /**
   * 
   */
  private void syncDuplicateCheck(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection) throws DfxException {
    LOGGER.trace("syncDuplicateCheck()");

    try {
      // ...
      Statement localDeleteStatement = localConnection.createStatement();

      String localDeleteSql = "DELETE FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".api_duplicate_check";
      localDeleteStatement.execute(DatabaseUtils.replaceSchema(network, localDeleteSql));

      localDeleteStatement.close();

      // ...
      Statement remoteStatement = remoteConnection.createStatement();

      String localInsertSql = "INSERT INTO " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)";
      PreparedStatement localInsertStatement = localConnection.prepareStatement(DatabaseUtils.replaceSchema(network, localInsertSql));

      // ...
      String remoteSelectSql = "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".api_duplicate_check";
      ResultSet remoteResultSet = remoteStatement.executeQuery(DatabaseUtils.replaceSchema(network, remoteSelectSql));

      while (remoteResultSet.next()) {
        localInsertStatement.setInt(1, remoteResultSet.getInt("withdrawal_id"));
        localInsertStatement.setString(2, remoteResultSet.getString("transaction_id"));
        localInsertStatement.execute();
      }

      remoteResultSet.close();

      remoteStatement.close();
      localInsertStatement.close();

      localConnection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(localConnection);
      LOGGER.error("syncDuplicateCheck", e);
    }
  }

  /**
   * 
   */
  private void syncTransaction(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection) {
    LOGGER.trace("syncTransaction()");

    try {
      String localSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction"
              + " WHERE block_number>=? AND number>=?"
              + " ORDER BY block_number, number"
              + " LIMIT ?";
      PreparedStatement localSelectStatement = localConnection.prepareStatement(DatabaseUtils.replaceSchema(network, localSelectSql));

      String remoteSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction"
              + " WHERE custom_type_code IS NULL"
              + " ORDER BY block_number, number"
              + " LIMIT 1";
      PreparedStatement remoteSelectStatement = remoteConnection.prepareStatement(DatabaseUtils.replaceSchema(network, remoteSelectSql));

      String remoteUpdateSql =
          "UPDATE " + TOKEN_PUBLIC_SCHEMA + ".transaction"
              + " SET custom_type_code=?"
              + " WHERE block_number=? AND number=? AND txid=?";
      PreparedStatement remoteUpdateStatement = remoteConnection.prepareStatement(DatabaseUtils.replaceSchema(network, remoteUpdateSql));

      // ...
      int limit = 1000;

      for (int i = 0; i < 20; i++) {
        doSyncTransaction(limit, localSelectStatement, remoteSelectStatement, remoteUpdateStatement);
        remoteConnection.commit();
      }

      // ...
      localSelectStatement.close();
      remoteSelectStatement.close();
      remoteUpdateStatement.close();
    } catch (Exception e) {
      DatabaseUtils.rollback(remoteConnection);
      LOGGER.error("syncTransaction", e);
    }
  }

  /**
   * 
   */
  private void doSyncTransaction(
      int limit,
      @Nonnull PreparedStatement localSelectStatement,
      @Nonnull PreparedStatement remoteSelectStatement,
      @Nonnull PreparedStatement remoteUpdateStatement) throws DfxException {
    try {
      // ...
      int remoteBlockNumber = -1;
      int remoteTransactionNumber = -1;

      ResultSet remoteResultSet = remoteSelectStatement.executeQuery();

      if (remoteResultSet.next()) {
        remoteBlockNumber = remoteResultSet.getInt("block_number");
        remoteTransactionNumber = remoteResultSet.getInt("number");
      }

      remoteResultSet.close();

      // ...
      if (-1 != remoteBlockNumber) {
        localSelectStatement.setInt(1, remoteBlockNumber);
        localSelectStatement.setInt(2, remoteTransactionNumber);
        localSelectStatement.setInt(3, limit);

        ResultSet localResultSet = localSelectStatement.executeQuery();

        while (localResultSet.next()) {
          int localBlockNumber = localResultSet.getInt("block_number");
          int localTransactionNumber = localResultSet.getInt("number");

          LOGGER.debug("Block: " + localBlockNumber + " / " + localTransactionNumber);

          remoteUpdateStatement.setString(1, localResultSet.getString("custom_type_code"));

          remoteUpdateStatement.setInt(2, localBlockNumber);
          remoteUpdateStatement.setInt(3, localTransactionNumber);
          remoteUpdateStatement.setString(4, localResultSet.getString("txid"));

          remoteUpdateStatement.execute();
        }

        localResultSet.close();
      }
    } catch (Exception e) {
      throw new DfxException("doSyncTransaction", e);
    }
  }
}
