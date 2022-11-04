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
public class OpenTransactionManagerUtxoTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerUtxoTest.class);

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
      TestUtils.globalSetup("opentransactionmanagerutxo");
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
      setJSONResoureFiles(
          "json/utxo/good/01-utxo.json",
          "json/utxo/good/01-chaindata-utxo.json");

      // ...
      // ...
      OpenTransactionVerifiedDTO verifiedDTO = new OpenTransactionVerifiedDTO();
      verifiedDTO.setSignature("validUtoxTest-signature");

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
  public void utxoAddressNotInWhitelistTest() {
    LOGGER.debug("utxoAddressNotInWhitelistTest()");

    try {
      setJSONResoureFiles(
          "json/utxo/invalid/01-utxo-not-in-whitelist.json",
          "json/utxo/invalid/01-chaindata-utxo-not-in-whitelist.json");

      // ...
      OpenTransactionInvalidatedDTO invalidatedDTO = new OpenTransactionInvalidatedDTO();
      invalidatedDTO.setSignature("utxoAddressNotInWhitelistTest-signature");
      invalidatedDTO.setReason("[UTXO Transaction] ID: 60dd53580576d8c07e37c9511ea37f7c273712cdcbd42a214b542da069cf6a92 - address not in whitelist");

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
      @Nonnull String chaindataFileName) throws Exception {
    // ...
    ClassLoader classLoader = this.getClass().getClassLoader();

    TestUtils.apiTransactionRequestHandler.setJSONFileArray(new File[] {
        new File(classLoader.getResource(transactionFileName).getFile())
    });

    TestUtils.apiWithdrawalRequestHandler.setJSONFileArray(null);

    // ...
    File jsonChainDataFile = new File(classLoader.getResource(chaindataFileName).getFile());

    DefiTransactionData transactionData =
        gson.fromJson(Files.readString(jsonChainDataFile.toPath()), DefiTransactionData.class);

    when(dataProviderMock.decodeRawTransaction(anyString())).thenReturn(transactionData);

    // ...
    DefiCustomData customData = new DefiCustomData();
    when(dataProviderMock.decodeCustomTransaction(anyString())).thenReturn(customData);
  }
}
