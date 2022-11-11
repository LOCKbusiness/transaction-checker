package ch.dfx;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.httpserver.handler.APISignInHandler;
import ch.dfx.httpserver.handler.APITransactionRequestHandler;
import ch.dfx.httpserver.handler.APIWithdrawalRequestHandler;
import ch.dfx.manager.OpenTransactionManagerTest;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class TestUtils {
  private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);

  // ...
  public static final DateTimeFormatter SQL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // ...
  public static H2DBManager databaseManagerMock = null;
  public static DefiDataProvider dataProviderMock = null;

  public static Connection connection;

  // ...
  public static APISignInHandler apiSignInHandler = null;
  public static APIWithdrawalRequestHandler apiWithdrawalRequestHandler = null;
  public static APITransactionRequestHandler apiTransactionRequestHandler = null;

  // ...
  private static final int PORT = 8080;
  private static final String API_URL = "http://localhost:8080/v1";

  private static HttpServer httpServer = null;

  // ...
  private static Gson gson = null;

  /**
   * 
   */
  public static void globalSetup(
      @Nonnull String logPrefix,
      boolean withHttpServer) {
    try {
      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(false, false, true);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", logPrefix + "-test-" + network + "-" + environment);
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      Properties properties = new Properties();
      properties.put(PropertyEnum.LOCK_ADDRESS.name().toLowerCase(), "");
      properties.put(PropertyEnum.LOCK_SIGNATURE.name().toLowerCase(), "");
      properties.put(
          PropertyEnum.LOCK_API_TEST_TOKEN.name().toLowerCase(),
          "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ3YWxsZXRJZCI6NSwidXNlcklkIjozLCJhZGRyZXNzIjoidGYxcTJ0azN1bjJwZGVkZmQ1aHpjc21meGp5bTMyZGVsZ3Z0ZzlxaHp6IiwiYmxvY2tjaGFpbiI6IkRlRmlDaGFpbiIsInJvbGUiOiJUcmFuc2FjdGlvbkNoZWNrZXIiLCJpYXQiOjE2NjU4MjAxNzksImV4cCI6MTY2NTk5Mjk3OX0.lfyMpb49XtWV91dcsilj3gFWo7cvYZ6iVA4k2YDTXOE");
      properties.put(PropertyEnum.LOCK_API_URL.name().toLowerCase(), API_URL);

      ConfigPropertyProvider.setup(properties);

      // ...
      LOGGER.debug("globalSetup()");

      // ...
      databaseManagerMock = mock(H2DBManager.class);
      dataProviderMock = mock(DefiDataProvider.class);

      // ...
      gson = new GsonBuilder().setPrettyPrinting().create();

      // ...
      setupDatabase();

      if (withHttpServer) {
        startHttpServer();
      }
    } catch (Exception e) {
      LOGGER.error("globalSetup", e);
    }
  }

  /**
   * 
   */
  public static void globalCleanup() {
    LOGGER.debug("globalCleanup()");

    stopHttpServer();
  }

  /**
   * 
   */
  public static void setDataProviderMock(@Nonnull String signature) throws DfxException {
    when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(signature);
    when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);
  }

  /**
   * 
   */
  public static void setJSONTransactionFile(@Nonnull String transactionFileName) throws Exception {
    // ...
    ClassLoader classLoader = TestUtils.class.getClassLoader();

    apiTransactionRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(transactionFileName).getFile())
    });

    apiWithdrawalRequestHandler.setJSONFileArray(null);
  }

  /**
   * 
   */
  public static void setJSONTransactionAndWithdrawalFile(
      @Nonnull String transactionFileName,
      @Nonnull String withdrawalFileName) throws Exception {
    // ...
    ClassLoader classLoader = TestUtils.class.getClassLoader();

    apiTransactionRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(transactionFileName).getFile())
    });

    apiWithdrawalRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(withdrawalFileName).getFile())
    });
  }

  /**
   * 
   */
  public static DefiTransactionData setJSONChainTransactionFile(@Nonnull String chainTransactionFileName) throws Exception {
    // ...
    ClassLoader classLoader = TestUtils.class.getClassLoader();

    File jsonChainDataFile = new File(classLoader.getResource(chainTransactionFileName).getFile());

    DefiTransactionData transactionData =
        gson.fromJson(Files.readString(jsonChainDataFile.toPath()), DefiTransactionData.class);

    when(dataProviderMock.decodeRawTransaction(anyString())).thenReturn(transactionData);

    // ...
    DefiCustomData customData = new DefiCustomData();
    when(dataProviderMock.decodeCustomTransaction(anyString())).thenReturn(customData);

    return transactionData;
  }

  /**
   * 
   */
  public static DefiCustomData setJSONChainCustomTransactionFile(@Nonnull String chainCustomeTransactionFileName) throws Exception {
    // ...
    ClassLoader classLoader = TestUtils.class.getClassLoader();

    File jsonChainCustomDataFile = new File(classLoader.getResource(chainCustomeTransactionFileName).getFile());

    DefiCustomData customData =
        gson.fromJson(Files.readString(jsonChainCustomDataFile.toPath()), DefiCustomData.class);

    when(dataProviderMock.decodeCustomTransaction(anyString())).thenReturn(customData);

    return customData;
  }

  /**
   * 
   */
  public static List<Map<String, Object>> sqlSelect(
      @Nonnull String tableName,
      @Nullable String whereSql) {
    LOGGER.debug("sqlSelect()");

    List<Map<String, Object>> dataList = new ArrayList<>();

    try {
      StringBuilder sqlSelectBuilder = new StringBuilder();

      sqlSelectBuilder.append("SELECT * FROM ").append(tableName);

      if (null != whereSql) {
        sqlSelectBuilder.append(" WHERE ").append(whereSql);
      }

      LOGGER.debug(sqlSelectBuilder);

      Statement statement = connection.createStatement();

      ResultSet resultSet = statement.executeQuery(sqlSelectBuilder.toString());

      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();

      while (resultSet.next()) {
        Map<String, Object> dataMap = new LinkedHashMap<>();

        for (int column = 1; column <= columnCount; column++) {
          String columnName = metaData.getColumnName(column);
          Object data = resultSet.getObject(column);

          dataMap.put(columnName, data);
        }

        dataList.add(dataMap);
      }

      resultSet.close();
      statement.close();
    } catch (Exception e) {
      LOGGER.error("sqlSelect", e);
    }

    return dataList;
  }

  /**
   * 
   */
  public static void sqlInsert(
      @Nonnull String tableName,
      @Nonnull String fieldNames,
      @Nonnull String values) {
    LOGGER.debug("sqlInsert()");

    try {
      StringBuilder sqlInsertBuilder = new StringBuilder();

      sqlInsertBuilder.append("INSERT INTO ").append(tableName);
      sqlInsertBuilder.append(" (").append(fieldNames).append(")");
      sqlInsertBuilder.append(" VALUES (").append(values).append(")");

      LOGGER.debug(sqlInsertBuilder);

      Statement statement = connection.createStatement();
      statement.execute(sqlInsertBuilder.toString());
      statement.close();

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("sqlInsert", e);
    }
  }

  /**
   * 
   */
  public static void sqlDelete(@Nonnull String tableName) {
    sqlDelete(tableName, null);
  }

  /**
   * 
   */
  public static void sqlDelete(
      @Nonnull String tableName,
      @Nullable String whereSql) {
    LOGGER.debug("sqlDelete()");

    try {
      StringBuilder sqlDeleteBuilder = new StringBuilder();

      sqlDeleteBuilder.append("DELETE FROM ").append(tableName);

      if (null != whereSql) {
        sqlDeleteBuilder.append(" WHERE ").append(whereSql);
      }

      LOGGER.debug(sqlDeleteBuilder);

      Statement statement = connection.createStatement();
      statement.execute(sqlDeleteBuilder.toString());
      statement.close();

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("sqlDelete", e);
    }
  }

  /**
   * 
   */
  private static void setupDatabase() throws Exception {
    LOGGER.debug("setupDatabase()");

    connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
    connection.setAutoCommit(false);

    when(databaseManagerMock.openConnection()).thenReturn(connection);

    ClassLoader classLoader = OpenTransactionManagerTest.class.getClassLoader();

    File initialSetupSqlFile = new File(classLoader.getResource("sql/initialSetup.sql").getFile());
    RunScript.execute(connection, new FileReader(initialSetupSqlFile));

    File openTransactionManagerTestSqlFile = new File(classLoader.getResource("sql/openTransactionManagerTest.sql").getFile());
    RunScript.execute(connection, new FileReader(openTransactionManagerTestSqlFile));
  }

  /**
   * 
   */
  private static void startHttpServer() throws Exception {
    LOGGER.debug("startHttpServer()");

    apiSignInHandler = new APISignInHandler();
    apiWithdrawalRequestHandler = new APIWithdrawalRequestHandler();
    apiTransactionRequestHandler = new APITransactionRequestHandler();

    httpServer =
        ServerBootstrap.bootstrap()
            .setListenerPort(PORT)
            .setServerInfo("My HTTP Testserver")
            .registerHandler("/v1/auth/sign-in", apiSignInHandler)
            .registerHandler("/v1/withdrawal/*", apiWithdrawalRequestHandler)
            .registerHandler("/v1/transaction/*", apiTransactionRequestHandler)
            .create();

    httpServer.start();

    LOGGER.info("=========================================");
    LOGGER.info("HTTP Server started: Port " + PORT);
    LOGGER.info("=========================================");
  }

  /**
   * 
   */
  private static void stopHttpServer() {
    LOGGER.debug("stopHttpServer()");

    if (null != httpServer) {
      httpServer.stop();
    }
  }
}
