package ch.dfx.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.TestUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.provider.DefiDataProvider;
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
  private static Gson gson = null;

  private static ApiAccessHandler apiAccessHandler = null;
  private static OpenTransactionManager transactionManager = null;

  /**
   * 
   */
  @BeforeClass
  public static void globalSetup() throws Exception {
    if (!isSuiteContext) {
      TestUtils.globalSetup("opentransactionmanager");
    }

    databaseManagerMock = TestUtils.databaseManagerMock;
    dataProviderMock = TestUtils.dataProviderMock;

    // ...
    gson = new GsonBuilder().setPrettyPrinting().create();

    apiAccessHandler = new ApiAccessHandlerImpl(TestUtils.NETWORK);
    apiAccessHandler.signIn();

    transactionManager =
        new OpenTransactionManager(TestUtils.NETWORK, apiAccessHandler, databaseManagerMock, dataProviderMock);
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
      setJSONResoureFiles(
          "json/withdrawal/empty/01-transaction-empty.json",
          "json/withdrawal/empty/01-withdrawal-empty.json");

      transactionManager.execute();
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void validTransactionTest() {
    LOGGER.debug("validTransactionTest()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/good/01-transaction.json",
          "json/withdrawal/good/01-withdrawal.json");

      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("dataValidTest-signature");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(verifiedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", verifiedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidAmountTest() {
    LOGGER.debug("dataInvalidAmountTest()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/01-transaction-wrong-message.json",
          "json/withdrawal/invalid/01-withdrawal-wrong-message.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("dataInvalidAmountTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 102 - invalid amount");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void signMessageMismatchTest() {
    LOGGER.debug("signMessageMismatchTest()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/02-transaction-wrong-message.json",
          "json/withdrawal/invalid/02-withdrawal-wrong-message.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("signMessageMismatchTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 103 - sign message mismatch");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest1() {
    LOGGER.debug("invalidWithdrawalSignatureTest1()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/03-transaction-wrong-message-signature.json",
          "json/withdrawal/invalid/03-withdrawal-wrong-message-signature.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest1-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 104 - invalid signature");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest2() {
    LOGGER.debug("invalidWithdrawalSignatureTest2()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/04-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/04-withdrawal-wrong-raw-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest2-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 105 - sign message mismatch");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest3() {
    LOGGER.debug("invalidWithdrawalSignatureTest3()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/05-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/05-withdrawal-wrong-raw-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest3-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 106 - sign message mismatch");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidWithdrawalSignatureTest4() {
    LOGGER.debug("invalidWithdrawalSignatureTest4()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/06-transaction-wrong-raw-transaction.json",
          "json/withdrawal/invalid/06-withdrawal-wrong-raw-transaction.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("invalidWithdrawalSignatureTest4-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 110 - sign message mismatch");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void withdrawalIdNotFoundTest() {
    LOGGER.debug("withdrawalIdNotFoundTest()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/07-transaction-wrong-signmessage-format.json",
          "json/withdrawal/invalid/07-withdrawal-wrong-signmessage-format.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("withdrawalIdNotFoundTest-signature");
      invalidatedDTO.setReason("[Withdrawal Transaction] ID: 440a351c87bb030c774c4b822504bc954188dde6321b05bb5b52af357098f688 - withdrawal id not found");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void transactionIdNotMatchTest() {
    LOGGER.debug("transactionIdNotMatchTest()");

    try {
      setJSONResoureFiles(
          "json/withdrawal/invalid/08-transaction-wrong-transactionid.json",
          "json/withdrawal/invalid/08-withdrawal-wrong-transactionid.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("transactionIdNotMatchTest-signature");
      invalidatedDTO.setReason("[Withdrawal] ID: 107 - transaction id not matches");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

      // ...
      transactionManager.execute();

      // ...
      String jsonResponse = TestUtils.apiTransactionRequestHandler.getJSONResponse();

      assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponse);
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   * 
   */
  private void setJSONResoureFiles(
      @Nonnull String transactionFileName,
      @Nonnull String withdrawalFileName) throws Exception {
    // ...
    ClassLoader classLoader = this.getClass().getClassLoader();

    TestUtils.apiTransactionRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(transactionFileName).getFile())
    });

    TestUtils.apiWithdrawalRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(withdrawalFileName).getFile())
    });

    // ...
    File jsonChainDataFile = new File(classLoader.getResource("json/withdrawal/01-chaindata-transaction.json").getFile());

    DefiTransactionData transactionData =
        gson.fromJson(Files.readString(jsonChainDataFile.toPath()), DefiTransactionData.class);

    when(dataProviderMock.decodeRawTransaction(anyString())).thenReturn(transactionData);

    // ...
    DefiCustomData customData = new DefiCustomData();
    when(dataProviderMock.decodeCustomTransaction(anyString())).thenReturn(customData);

  }
}
