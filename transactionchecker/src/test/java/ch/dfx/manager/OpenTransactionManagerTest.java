package ch.dfx.manager;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.dfx.TestUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManagerTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerTest.class);

  // ...
  public static boolean isSuiteContext = false;

  // ...
  private static H2DBManager databaseManagerMock = null;
  private static DefiDataProvider dataProviderMock = null;

  // ...
  private static ApiAccessHandler apiAccessHandler = null;
  private static OpenTransactionManager transactionManager = null;

  /**
   * 
   */
  @BeforeClass
  public static void globalSetup() {
    if (!isSuiteContext) {
      TestUtils.globalSetup("opentransactionmanager", true);
    }

    databaseManagerMock = TestUtils.databaseManagerMock;
    dataProviderMock = TestUtils.dataProviderMock;

    // ...
    apiAccessHandler = new ApiAccessHandlerImpl(NetworkEnum.TESTNET);
    apiAccessHandler.signIn();

    transactionManager =
        new OpenTransactionManager(NetworkEnum.TESTNET, apiAccessHandler, databaseManagerMock, dataProviderMock);
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

  /**
   * 
   */
  @Before
  public void before() {
    String deleteSql = "api_duplicate_check";
    deleteSql = DatabaseUtils.replaceSchema(NetworkEnum.TESTNET, deleteSql);

    TestUtils.sqlDelete(TOKEN_NETWORK_SCHEMA, deleteSql);
  }

  @Test
  public void missingDataTest() {
    LOGGER.debug("missingDataTest()");

    try {
      TestUtils.apiTransactionRequestHandler.setJSONFileArray(new File[] {});
      TestUtils.apiWithdrawalRequestHandler.setJSONFileArray(new File[] {});

      transactionManager.execute();
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void emptyDataTest() {
    LOGGER.debug("emptyDataTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/empty/01-transaction-empty.json",
          "json/withdrawal/empty/01-withdrawal-empty.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      transactionManager.execute();
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void validTransactionTest() {
    LOGGER.debug("validTransactionTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/good/01-transaction.json",
          "json/withdrawal/good/01-withdrawal.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("dataValidTest-signature");

      TestUtils.setDataProviderMock(verifiedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", verifiedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidAmountTest() {
    LOGGER.debug("dataInvalidAmountTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/01-transaction-wrong-message.json",
          "json/withdrawal/invalid/01-withdrawal-wrong-message.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("dataInvalidAmountTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 102 - invalid amount");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void signMessageMismatchTest() {
    LOGGER.debug("signMessageMismatchTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/02-transaction-wrong-message.json",
          "json/withdrawal/invalid/02-withdrawal-wrong-message.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("signMessageMismatchTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 103 - sign message mismatch");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest1() {
    LOGGER.debug("invalidWithdrawalSignatureTest1()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/03-transaction-wrong-message-signature.json",
          "json/withdrawal/invalid/03-withdrawal-wrong-message-signature.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest1-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 104 - invalid signature");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest2() {
    LOGGER.debug("invalidWithdrawalSignatureTest2()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/04-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/04-withdrawal-wrong-raw-transaction.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest2-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 105 - sign message mismatch");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest3() {
    LOGGER.debug("invalidWithdrawalSignatureTest3()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/05-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/05-withdrawal-wrong-raw-transaction.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest3-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 106 - sign message mismatch");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest4() {
    LOGGER.debug("invalidWithdrawalSignatureTest4()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/06-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/06-withdrawal-wrong-raw-transaction.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest4-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 110 - sign message mismatch");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void withdrawalIdNotFoundTest() {
    LOGGER.debug("withdrawalIdNotFoundTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/07-transaction-wrong-signmessage-format.json",
          "json/withdrawal/invalid/07-withdrawal-wrong-signmessage-format.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("withdrawalIdNotFoundTest-signature");
      invalidatedDTO.setReason("[Withdrawal Transaction] ID: 440a351c87bb030c774c4b822504bc954188dde6321b05bb5b52af357098f688 - withdrawal id not found");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void transactionIdNotMatchTest() {
    LOGGER.debug("transactionIdNotMatchTest()");

    try {
      TestUtils.setJSONTransactionAndWithdrawalFile(
          "json/withdrawal/invalid/08-transaction-wrong-transactionid.json",
          "json/withdrawal/invalid/08-withdrawal-wrong-transactionid.json");
      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("transactionIdNotMatchTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 107 - transaction id not matches");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void validDuplicateWithdrawalTest() {
    LOGGER.debug("validDuplicateWithdrawalTest()");

    try {
      // ...
      ClassLoader classLoader = this.getClass().getClassLoader();

      TestUtils.apiTransactionRequestHandler.setJSONFileArray(new File[] {
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile())
      });

      TestUtils.apiWithdrawalRequestHandler.setJSONFileArray(new File[] {
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile())
      });

      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("dataValidTest-signature");

      TestUtils.setDataProviderMock(verifiedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 1, jsonResponseList.size());
      assertEquals("JSON Response", verifiedDTO.toString(), jsonResponseList.get(0));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidDuplicateWithdrawalTest() {
    LOGGER.debug("invalidDuplicateWithdrawalTest()");

    try {
      // ...
      ClassLoader classLoader = this.getClass().getClassLoader();

      TestUtils.apiTransactionRequestHandler.setJSONFileArray(new File[] {
          new File(classLoader.getResource("json/withdrawal/good/01-transaction.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/invalid/09-transaction-duplicated.json").getFile())
      });

      TestUtils.apiWithdrawalRequestHandler.setJSONFileArray(new File[] {
          new File(classLoader.getResource("json/withdrawal/good/01-withdrawal.json").getFile()),
          new File(classLoader.getResource("json/withdrawal/invalid/09-withdrawal-duplicated.json").getFile())
      });

      TestUtils.setJSONChainTransactionFile("json/withdrawal/01-chaindata-transaction.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("invalidDuplicateWithdrawalTest-signature");

      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidDuplicateWithdrawalTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 2 - duplicated");

      TestUtils.setDataProviderMock(invalidatedDTO.getSignature());

      // ...
      transactionManager.execute();

      // ...
      List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

      assertEquals("JSON Response List Size", 2, jsonResponseList.size());
      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
      assertEquals("JSON Response", verifiedDTO.toString(), jsonResponseList.get(1));
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }
}
