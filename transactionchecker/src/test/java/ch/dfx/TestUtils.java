package ch.dfx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
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
  public static final String NETWORK = "testnet";

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

  /**
   * 
   */
  public static void globalSetup(@Nonnull String logPrefix) throws Exception {
    // ...
    String environment = TransactionCheckerUtils.getEnvironment().name().toLowerCase();

    // ...
    System.setProperty("logFilename", logPrefix + "-test-" + NETWORK + "-" + environment);
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

    setupDatabase();
    startHttpServer();
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

    httpServer.stop();
  }
}
