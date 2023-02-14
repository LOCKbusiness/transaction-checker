package ch.dfx.transactionserver.cleaner;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.dfx.TestUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventCollector;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StakingWithdrawalReservedCleanerTest {
  private static final Logger LOGGER = LogManager.getLogger(StakingWithdrawalReservedCleanerTest.class);

  // ...
  public static boolean isSuiteContext = false;

  // ...
  private static H2DBManager databaseManagerMock = null;
  private static MessageEventCollector messageEventCollector = null;

  // ...
  private static final String CUSTOMER_ADDRESS = "df1qgz2xyzqwnsn5979syu6ng9wxlc4c2ac98m377f";

  /**
   * 
   */
  @BeforeClass
  public static void globalSetup() {
    if (!isSuiteContext) {
      TestUtils.globalSetup("opentransactionmanager", false);
    }

    databaseManagerMock = TestUtils.databaseManagerMock;

    // ...
    messageEventCollector = new MessageEventCollector();
    MessageEventBus.getInstance().register(messageEventCollector);
  }

  /**
   * 
   */
  @AfterClass
  public static void globalCleanup() {
    if (!isSuiteContext) {
      TestUtils.globalCleanup();
    }
  }

  @Before
  public void before() {
    TestUtils.sqlDelete(TOKEN_PUBLIC_SCHEMA, "transaction", "txid='abcde'");

    TestUtils.sqlDelete(TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");
    TestUtils.sqlDelete(TOKEN_STAKING_SCHEMA, "staking", "liquidity_address_number=1 AND deposit_address_number=2 AND customer_address_number=4");

    messageEventCollector.getMessageEventList();
  }

  @Test
  public void durationInTimeTest() {
    LOGGER.debug("durationInTimeTest()");

    try {
      String testTime = LocalDateTime.now().minusHours(5).format(TestUtils.SQL_DATETIME_FORMATTER);

      TestUtils.sqlInsert(
          TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved",
          "token_number, withdrawal_id, transaction_id, customer_address, vout, create_time",
          "0, 1, 'abcde', '" + CUSTOMER_ADDRESS + "', 1.50000000, '" + testTime + "'");

      executeStakingWithdrawalReservedCleaner();

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved", "token_number=0 AND withdrawal_id=1");

      assertEquals("staking_withdrawal_reserved Size", 1, reservedAfterDataList.size());

      Map<String, Object> dataMap = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(1), dataMap.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", "abcde", dataMap.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "1.50000000", dataMap.get("VOUT").toString());

      // ...
      List<MessageEvent> messageEventList = messageEventCollector.getMessageEventList();
      assertEquals("Message Event List Size", 0, messageEventList.size());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void durationOvertimeTest() {
    LOGGER.debug("durationOvertimeTest()");

    try {
      String testTime = LocalDateTime.now().minusHours(25).format(TestUtils.SQL_DATETIME_FORMATTER);

      TestUtils.sqlInsert(
          TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved",
          "token_number, withdrawal_id, transaction_id, customer_address, vout, create_time",
          "0, 1, 'abcde', '" + CUSTOMER_ADDRESS + "', 0.12345678, '" + testTime + "'");

      executeStakingWithdrawalReservedCleaner();

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved", "token_number=0 AND withdrawal_id=1");

      assertEquals("staking_withdrawal_reserved Size", 1, reservedAfterDataList.size());

      Map<String, Object> dataMap = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(1), dataMap.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", "abcde", dataMap.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "0.12345678", dataMap.get("VOUT").toString());

      // ...
      List<MessageEvent> messageEventList = messageEventCollector.getMessageEventList();
      assertEquals("Message Event List Size", 1, messageEventList.size());

      assertEquals(
          "Message",
          "Staking Withdrawal Reserved: 25 hours overtime:"
              + " tokenNumber=0 / withdrawalId=1 / transactionId=abcde / vout=0.12345678",
          messageEventList.get(0).getMessage());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void transactionInChainTest() {
    LOGGER.debug("transactionInChainTest()");

    try {
      String testTime = LocalDateTime.now().minusHours(100).format(TestUtils.SQL_DATETIME_FORMATTER);

      TestUtils.sqlInsert(
          TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved",
          "token_number, withdrawal_id, transaction_id, customer_address, vout, create_time",
          "0, 1, 'abcde', '" + CUSTOMER_ADDRESS + "', 0.12345678, '" + testTime + "'");

      TestUtils.sqlInsert(
          TOKEN_PUBLIC_SCHEMA,
          "transaction",
          "block_number, number, txid, custom_type_code",
          "1, 1, 'abcde', 0");

      executeStakingWithdrawalReservedCleaner();

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(TOKEN_STAKING_SCHEMA, "staking_withdrawal_reserved", "token_number=0 AND withdrawal_id=1");

      assertEquals("staking_withdrawal_reserved Size", 0, reservedAfterDataList.size());

      // ...
      List<MessageEvent> messageEventList = messageEventCollector.getMessageEventList();
      assertEquals("Message Event List Size", 0, messageEventList.size());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   * 
   */
  private void executeStakingWithdrawalReservedCleaner() throws DfxException {
    Connection connection = databaseManagerMock.openConnection();

    DatabaseBlockHelper databaseBlockHelper = new DatabaseBlockHelper(NetworkEnum.TESTNET);
    DatabaseBalanceHelper databaseBalanceHelper = new DatabaseBalanceHelper(NetworkEnum.TESTNET);

    databaseBlockHelper.openStatements(connection);
    databaseBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);

    StakingWithdrawalReservedCleaner stakingWithdrawalReservedCleaner =
        new StakingWithdrawalReservedCleaner(NetworkEnum.TESTNET, databaseBlockHelper, databaseBalanceHelper);
    stakingWithdrawalReservedCleaner.clean(connection, TOKEN_STAKING_SCHEMA);

    databaseBlockHelper.closeStatements();
    databaseBalanceHelper.closeStatements();
    databaseManagerMock.closeConnection(connection);
  }
}
