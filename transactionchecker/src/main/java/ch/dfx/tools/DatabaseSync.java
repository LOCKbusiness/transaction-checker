package ch.dfx.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  /**
   * 
   */
  public DatabaseSync() {
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
      localDeleteStatement.execute("DELETE FROM public.api_duplicate_check");
      localDeleteStatement.close();

      // ...
      Statement remoteStatement = remoteConnection.createStatement();

      PreparedStatement localInsertStatement =
          localConnection.prepareStatement(
              "INSERT INTO public.api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)");

      // ...
      ResultSet resultSet = remoteStatement.executeQuery("SELECT * FROM public.api_duplicate_check");

      while (resultSet.next()) {
        localInsertStatement.setInt(1, resultSet.getInt("withdrawal_id"));
        localInsertStatement.setString(2, resultSet.getString("transaction_id"));
        localInsertStatement.execute();
      }

      resultSet.close();

      remoteStatement.close();
      localInsertStatement.close();

      localConnection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(localConnection);
      LOGGER.error("syncDuplicateCheck", e);
    }
  }
}
