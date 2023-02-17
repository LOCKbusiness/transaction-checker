package ch.dfx;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.TokenProvider;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.httpserver.handler.APISignInHandler;
import ch.dfx.httpserver.handler.APITransactionRequestHandler;
import ch.dfx.httpserver.handler.APIWithdrawalRequestHandler;
import ch.dfx.manager.OpenTransactionManagerTest;
import ch.dfx.transactionserver.database.DatabaseUtils;
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
      TokenProvider.setup(network);

      setupConfigProvider(network, environment);

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
  private static void setupConfigProvider(
      @Nonnull NetworkEnum network,
      @Nonnull EnvironmentEnum environment) throws DfxException {
    LOGGER.debug("setupConfigProvider()");

    ConfigProvider.setup(network, environment, new String[] {});

    // ...
    File configFile = Path.of("config", "global", "config-junittest.json").toFile();

    try (FileReader reader = new FileReader(configFile)) {
      Field hostIdField = ConfigProvider.class.getDeclaredField("hostId");
      hostIdField.setAccessible(true);
      hostIdField.set(ConfigProvider.getInstance(), "");

      // ...
      JsonObject configObject = new Gson().fromJson(reader, JsonObject.class);

      Method setupConfigMapMethod = ConfigProvider.class.getDeclaredMethod("setupConfigMap", JsonObject.class, String.class);
      setupConfigMapMethod.setAccessible(true);
      setupConfigMapMethod.invoke(ConfigProvider.getInstance(), configObject, null);
    } catch (Exception e) {
      throw new DfxException("setupConfigProvider", e);
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
      @Nonnull String tokenSchema,
      @Nonnull String tableName,
      @Nullable String whereSql) {
    LOGGER.debug("sqlSelect()");

    List<Map<String, Object>> dataList = new ArrayList<>();

    try {
      StringBuilder sqlSelectBuilder = new StringBuilder();

      sqlSelectBuilder.append("SELECT * FROM ").append(tokenSchema).append(".").append(tableName);

      if (null != whereSql) {
        sqlSelectBuilder.append(" WHERE ").append(whereSql);
      }

      String sqlSelect = DatabaseUtils.replaceSchema(NetworkEnum.TESTNET, sqlSelectBuilder.toString());
      LOGGER.debug(sqlSelect);

      Statement statement = connection.createStatement();

      ResultSet resultSet = statement.executeQuery(sqlSelect);

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
      @Nonnull String tokenSchema,
      @Nonnull String tableName,
      @Nonnull String fieldNames,
      @Nonnull String values) {
    LOGGER.debug("sqlInsert()");

    try {
      StringBuilder sqlInsertBuilder = new StringBuilder();

      sqlInsertBuilder.append("INSERT INTO ").append(tokenSchema).append(".").append(tableName);
      sqlInsertBuilder.append(" (").append(fieldNames).append(")");
      sqlInsertBuilder.append(" VALUES (").append(values).append(")");

      String sqlInsert = DatabaseUtils.replaceSchema(NetworkEnum.TESTNET, sqlInsertBuilder.toString());
      LOGGER.debug(sqlInsert);

      Statement statement = connection.createStatement();
      statement.execute(sqlInsert);
      statement.close();

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("sqlInsert", e);
    }
  }

  /**
   * 
   */
  public static void sqlDelete(
      @Nonnull String tokenSchema,
      @Nonnull String tableName) {
    sqlDelete(tokenSchema, tableName, null);
  }

  /**
   * 
   */
  public static void sqlDelete(
      @Nonnull String tokenSchema,
      @Nonnull String tableName,
      @Nullable String whereSql) {
    LOGGER.debug("sqlDelete()");

    try {
      StringBuilder sqlDeleteBuilder = new StringBuilder();

      sqlDeleteBuilder.append("DELETE FROM ").append(tokenSchema).append(".").append(tableName);

      if (null != whereSql) {
        sqlDeleteBuilder.append(" WHERE ").append(whereSql);
      }

      String sqlDelete = DatabaseUtils.replaceSchema(NetworkEnum.TESTNET, sqlDeleteBuilder.toString());
      LOGGER.debug(sqlDelete);

      Statement statement = connection.createStatement();
      statement.execute(sqlDelete);
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

    LOGGER.debug("=========================================");
    LOGGER.info("HTTP Server started: Port " + PORT);
    LOGGER.debug("=========================================");
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
