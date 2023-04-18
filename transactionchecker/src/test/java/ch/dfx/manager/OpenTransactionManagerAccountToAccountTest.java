package ch.dfx.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.dfx.TestUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiAccessHandlerImpl;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManagerAccountToAccountTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerAccountToAccountTest.class);

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
      TestUtils.globalSetup("opentransactionmanagerutxo", true);
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

  @Test
  public void validAccountToAccountTest() {
    LOGGER.debug("validAccountToAccountTest()");

    try {
      doValidTest(
          "json/accounttoaccount/good/01-API-AccountToAccount.json",
          "json/accounttoaccount/good/01-DC-AccountToAccount.json",
          "json/accounttoaccount/good/01-DC-CustomAccountToAccount.json",
          "validAccountToAccountTest-signature-01");

      doValidTest(
          "json/accounttoaccount/good/01-API-AccountToAccount.json",
          "json/accounttoaccount/good/02-DC-AccountToAccount.json",
          "json/accounttoaccount/good/01-DC-CustomAccountToAccount.json",
          "validAccountToAccountTest-signature-02");

      doValidTest(
          "json/accounttoaccount/good/01-API-AccountToAccount.json",
          "json/accounttoaccount/good/01-DC-AccountToAccount.json",
          "json/accounttoaccount/good/03-DC-CustomAccountToAccount.json",
          "validAccountToAccountTest-signature-03");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidAccountToAccountTest() {
    LOGGER.debug("invalidAccountToAccountTest()");

    try {
      doInvalidTest(
          "json/accounttoaccount/good/01-API-AccountToAccount.json",
          "json/accounttoaccount/invalid/01-DC-AccountToAccount-wrong-address.json",
          "json/accounttoaccount/good/01-DC-CustomAccountToAccount.json",
          "invalidAccountToAccountTest-signature-01",
          "[Transaction] ID: 832b121aad86af15bd5844515500e854c2968232458faace96fdff7f9ae245bd - invalid vout address");

      doInvalidTest(
          "json/accounttoaccount/good/01-API-AccountToAccount.json",
          "json/accounttoaccount/good/01-DC-AccountToAccount.json",
          "json/accounttoaccount/invalid/01-DC-CustomAccountToAccount-wrong-address.json",
          "invalidAccountToAccountTest-signature-01",
          "[Transaction] ID: 832b121aad86af15bd5844515500e854c2968232458faace96fdff7f9ae245bd - invalid custom address");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   * 
   */
  private void doValidTest(
      @Nonnull String validAPITransactionFileName,
      @Nonnull String validChainTransactionFileName,
      @Nonnull String validChainCustomTransactionFileName,
      @Nonnull String validSignature) throws Exception {
    TestUtils.setJSONTransactionFile(validAPITransactionFileName);
    TestUtils.setJSONChainTransactionFile(validChainTransactionFileName);
    TestUtils.setJSONChainCustomTransactionFile(validChainCustomTransactionFileName);

    // ...
    OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
    verifiedDTO.setSignature(validSignature);

    when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(verifiedDTO.getSignature());
    when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

    // ...
    transactionManager.execute();

    // ...
    List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

    assertEquals("JSON Response List Size", 1, jsonResponseList.size());
    assertEquals("JSON Response", verifiedDTO.toString(), jsonResponseList.get(0));
  }

  /**
   * 
   */
  private void doInvalidTest(
      @Nonnull String validAPITransactionFileName,
      @Nonnull String chainTransactionFileName,
      @Nonnull String chainCustomTransactionFileName,
      @Nonnull String invalidSignature,
      @Nonnull String invalidReason) throws Exception {
    TestUtils.setJSONTransactionFile(validAPITransactionFileName);
    TestUtils.setJSONChainTransactionFile(chainTransactionFileName);
    TestUtils.setJSONChainCustomTransactionFile(chainCustomTransactionFileName);

    // ...
    OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
    invalidatedDTO.setSignature(invalidSignature);
    invalidatedDTO.setReason(invalidReason);

    when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
    when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

    // ...
    transactionManager.execute();

    // ...
    List<String> jsonResponseList = TestUtils.apiTransactionRequestHandler.getJSONResponseList();

    assertEquals("JSON Response List Size", 1, jsonResponseList.size());
    assertEquals("JSON Response", invalidatedDTO.toString(), jsonResponseList.get(0));
  }
}
