package ch.dfx.tools;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.data.DatabaseConnectionData;
import ch.dfx.tools.data.DatabaseData;

/**
 * 
 */
public abstract class DatabaseTool {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseTool.class);

  // ...
  private final Gson gson;

  /**
   * 
   */
  public DatabaseTool() {
    this.gson = new Gson();
  }

  /**
   * 
   */
  public DatabaseConnectionData getDatabaseConnectionData() throws DfxException {
    File jsonFile = new File("config/json", "databaseConnectionData.json");

    try (Reader reader = new FileReader(jsonFile)) {
      return gson.fromJson(reader, DatabaseConnectionData.class);
    } catch (Exception e) {
      throw new DfxException("getDatabaseConnectionData", e);
    }
  }

  /**
   *
   */
  public Connection openConnection(@Nonnull DatabaseData databaseData) throws DfxException {
    LOGGER.trace("openConnection()");

    try {
      String tcpUrl = databaseData.getTcpUrl();
      LOGGER.trace("JDBC URL: " + tcpUrl);

      Connection connection = DriverManager.getConnection(tcpUrl, databaseData.getUsername(), databaseData.getPassword());
      connection.setAutoCommit(false);
      connection.setReadOnly(true);

      return connection;
    } catch (Exception e) {
      throw new DfxException("openConnection", e);
    }
  }

  /**
   *
   */
  public void closeConnection(Connection connection) {
    LOGGER.trace("closeConnection()");

    try {
      if (null != connection) {
        connection.close();
      }
    } catch (Exception e) {
      LOGGER.error("closeConnection", e);
    }
  }
}
