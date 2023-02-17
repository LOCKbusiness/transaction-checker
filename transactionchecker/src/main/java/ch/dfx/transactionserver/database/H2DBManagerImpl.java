package ch.dfx.transactionserver.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.RunScript;
import org.h2.tools.Script;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.errorhandling.DfxException;

/**
 * JDBC URL: "jdbc:h2:C:/Data/Database/h2/[DATABASE_NAME]"
 * JDPC URL: "jdbc:h2:tcp://localhost:[PORT]/[DATABASE_NAME]"
 */
public class H2DBManagerImpl implements H2DBManager {
  private static final Logger LOGGER = LogManager.getLogger(H2DBManagerImpl.class);

  // ...
  private static String jdbcUrlPrefix = "jdbc:h2:";

  /**
   *
   */
  public H2DBManagerImpl() {
  }

  /**
   * 
   */
  private String getDirectoryUrl() {
    LOGGER.trace("getDirectoryUrl()");

    String h2Dir = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_DIR);
    String h2DBName = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_NAME);

    return jdbcUrlPrefix + h2Dir + "/" + h2DBName;
  }

  /**
   * 
   */
  private String getTcpUrl() {
    LOGGER.trace("getTcpUrl()");

    String tcpHost = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_SERVER_TCP_HOST);
    String tcpPort = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_SERVER_TCP_PORT);
    String h2DBName = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_NAME);

    return jdbcUrlPrefix + tcpHost + ":" + tcpPort + "/" + h2DBName;
  }

  /**
   *
   */
  @Override
  public Connection openConnection() throws DfxException {
    LOGGER.trace("openConnection()");

    try {
      String jdbcUrl = getTcpUrl();
      LOGGER.trace("JDBC URL: " + jdbcUrl);

      String jdbcUsername = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_USERNAME);
      String jdbcPassword = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_PASSWORD);

      Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
      connection.setAutoCommit(false);

      return connection;
    } catch (Exception e) {
      throw new DfxException("openConnection", e);
    }
  }

  /**
   *
   */
  @Override
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

  /**
   * 
   */
  @Override
  public void compact() throws DfxException {
    LOGGER.trace("compact()");

    try {
      String jdbcUrl = getDirectoryUrl();
      LOGGER.debug("JDBC URL: " + jdbcUrl);

      String h2Dir = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_DIR);
      String h2DBName = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_DB_NAME);

      String jdbcUsername = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_USERNAME);
      String jdbcPassword = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.H2_PASSWORD);

      // ...
      File dbFile = new File(h2Dir, h2DBName + ".mv.db");
      File dbBakFile = new File(h2Dir, h2DBName + ".mv.db.bak");
      LOGGER.debug("DB FILE:        " + dbFile.getAbsolutePath());
      LOGGER.debug("DB BACKUP FILE: " + dbBakFile.getAbsolutePath());

      if (!dbFile.exists()) {
        throw new DfxException("DB File not found: " + dbFile.getAbsolutePath());
      }

      FileUtils.copyFile(dbFile, dbBakFile);

      if (!dbBakFile.exists()) {
        throw new DfxException("DB Backup File not found: " + dbBakFile.getAbsolutePath());
      }

      // ...
      File compactFile = new File(h2Dir, h2DBName + "-compact.sql");
      LOGGER.debug("COMPACT FILE: " + compactFile.getAbsolutePath());

      Script.process(jdbcUrl, jdbcUsername, jdbcPassword, compactFile.getAbsolutePath(), "", "");

      if (!compactFile.exists()) {
        throw new DfxException("COMPACT File File not found: " + compactFile.getAbsolutePath());
      }

      // ...
      DeleteDbFiles.execute(h2Dir, h2DBName, true);

      if (dbFile.exists()) {
        throw new DfxException("DB File found after deletion: " + dbFile.getAbsolutePath());
      }

      // ...
      RunScript.execute(jdbcUrl, jdbcUsername, jdbcPassword, compactFile.getAbsolutePath(), null, false);

      if (!dbFile.exists()) {
        throw new DfxException("DB File not found: " + dbFile.getAbsolutePath());
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("compact", e);
    }
  }
}
