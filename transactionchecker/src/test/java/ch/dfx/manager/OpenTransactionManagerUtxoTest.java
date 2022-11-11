package ch.dfx.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

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
public class OpenTransactionManagerUtxoTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerUtxoTest.class);

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

  // @Test
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

  // @Test
  public void validUtoxTest() {
    LOGGER.debug("validUtoxTest()");

    try {
      TestUtils.setJSONTransactionFile("json/utxo/good/01-utxo.json");
      TestUtils.setJSONChainTransactionFile("json/utxo/good/01-chaindata-utxo.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("validUtoxTest-signature");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(verifiedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

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
  public void utxoAddressNotInWhitelistTest() {
    LOGGER.debug("utxoAddressNotInWhitelistTest()");

    try {
      TestUtils.setJSONTransactionFile("json/utxo/invalid/01-utxo-not-in-whitelist.json");
      TestUtils.setJSONChainTransactionFile("json/utxo/invalid/01-chaindata-utxo-not-in-whitelist.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("utxoAddressNotInWhitelistTest-signature");
      invalidatedDTO.setReason("[UTXO Transaction] ID: 60dd53580576d8c07e37c9511ea37f7c273712cdcbd42a214b542da069cf6a92 - address not in whitelist");

      when(dataProviderMock.signMessage(anyString(), anyString(), anyString())).thenReturn(invalidatedDTO.getSignature());
      when(dataProviderMock.verifyMessage(anyString(), anyString(), anyString())).thenReturn(true);

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
}
