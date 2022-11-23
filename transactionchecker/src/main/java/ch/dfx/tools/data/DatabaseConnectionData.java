package ch.dfx.tools.data;

import ch.dfx.transactionserver.data.DatabaseDTO;

/**
 * 
 */
public class DatabaseConnectionData extends DatabaseDTO {

  private DatabaseData localDatabaseData = null;
  private DatabaseData remoteDatabaseData = null;

  /**
   * 
   */
  public DatabaseConnectionData() {
  }

  public DatabaseData getLocalDatabaseData() {
    return localDatabaseData;
  }

  public void setLocalDatabaseData(DatabaseData localDatabaseData) {
    this.localDatabaseData = localDatabaseData;
  }

  public DatabaseData getRemoteDatabaseData() {
    return remoteDatabaseData;
  }

  public void setRemoteDatabaseData(DatabaseData remoteDatabaseData) {
    this.remoteDatabaseData = remoteDatabaseData;
  }
}
