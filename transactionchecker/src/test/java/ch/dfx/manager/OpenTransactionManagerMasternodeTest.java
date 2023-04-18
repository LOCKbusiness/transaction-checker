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
public class OpenTransactionManagerMasternodeTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerMasternodeTest.class);

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
      TestUtils.globalSetup("opentransactionmanagermasternode", true);
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

  @Test
  public void validCreateMasternodeTest() {
    LOGGER.debug("validCreateMasternodeTest()");

    try {
      TestUtils.setJSONTransactionFile("json/masternode/good/01-API-CreateMasternode.json");
      TestUtils.setJSONChainTransactionFile("json/masternode/good/01-DC-Transaction.json");
      TestUtils.setJSONChainCustomTransactionFile("json/masternode/good/01-DC-CustomTransaction.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("validCreateMasternodeTest-signature");

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
  public void validResignMasternodeTest() {
    LOGGER.debug("validResignMasternodeTest()");

    try {
      TestUtils.setJSONTransactionFile("json/masternode/good/02-API-ResignMasternode.json");
      TestUtils.setJSONChainTransactionFile("json/masternode/good/02-DC-Transaction.json");
      TestUtils.setJSONChainCustomTransactionFile("json/masternode/good/02-DC-CustomTransaction.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("validResignMasternodeTest-signature");

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
  public void validVoteMasternodeTest() {
    LOGGER.debug("validVoteMasternodeTest()");

    try {
      TestUtils.setJSONTransactionFile("json/masternode/good/03-API-VoteMasternode.json");
      TestUtils.setJSONChainTransactionFile("json/masternode/good/03-DC-Transaction.json");
      TestUtils.setJSONChainCustomTransactionFile("json/masternode/good/03-DC-CustomTransaction.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("validVoteMasternodeTest-signature");

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
  public void masternodeAddressNotInWhitelistTest() {
    LOGGER.debug("masternodeAddressNotInWhitelistTest()");

    try {
      TestUtils.setJSONTransactionFile("json/masternode/invalid/01-API-Masternode-not-in-whitelist.json");
      TestUtils.setJSONChainTransactionFile("json/masternode/invalid/01-DC-Transaction-not-in-whitelist.json");
      TestUtils.setJSONChainCustomTransactionFile("json/masternode/invalid/01-DC-CustomTransaction-not-in-whitelist.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("masternodeAddressNotInWhitelistTest-signature");
      invalidatedDTO.setReason("[Transaction] ID: 0dc885313b263a4d207046524e8c4a422b2490b56a87ef177e4b94bed8c77140 - invalid vout address");

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
