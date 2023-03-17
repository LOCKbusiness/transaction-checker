package ch.dfx.tools.sync;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.DatabaseTool;
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

    // syncDuplicateCheck(localConnection, remoteConnection);
    // resyncDuplicateCheck(localConnection, remoteConnection);
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

      String localDeleteSql = "DELETE FROM " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check";
      localDeleteStatement.execute(DatabaseUtils.replaceSchema(network, localDeleteSql));

      localDeleteStatement.close();

      // ...
      String localInsertSql = "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)";
      PreparedStatement localInsertStatement = localConnection.prepareStatement(DatabaseUtils.replaceSchema(network, localInsertSql));

      // ...
      Statement remoteStatement = remoteConnection.createStatement();

      String remoteSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check";
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
  private void resyncDuplicateCheck(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection) throws DfxException {
    LOGGER.trace("syncDuplicateCheck()");

    try {
      // ...
      String remoteInsertSql = "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)";
      PreparedStatement remoteInsertStatement = remoteConnection.prepareStatement(DatabaseUtils.replaceSchema(network, remoteInsertSql));

      // ...
      Statement localSelectStatement = localConnection.createStatement();

      String localSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check WHERE withdrawal_id > 7531";
      ResultSet localResultSet = localSelectStatement.executeQuery(DatabaseUtils.replaceSchema(network, localSelectSql));

      while (localResultSet.next()) {
        remoteInsertStatement.setInt(1, localResultSet.getInt("withdrawal_id"));
        remoteInsertStatement.setString(2, localResultSet.getString("transaction_id"));
        remoteInsertStatement.execute();
      }

      localResultSet.close();

      remoteInsertStatement.close();

      remoteConnection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(remoteConnection);
      LOGGER.error("syncDuplicateCheck", e);
    }
  }

}
