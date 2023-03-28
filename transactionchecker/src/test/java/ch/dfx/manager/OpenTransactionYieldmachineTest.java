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
public class OpenTransactionYieldmachineTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionYieldmachineTest.class);

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
      TestUtils.globalSetup("opentransactionyieldmachine", true);
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
  @Test
  public void validCreateVaultTest() {
    LOGGER.debug("validCreateVaultTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/02-API-CreateVault.json",
          "json/yieldmaschine/good/02-DC-Transaction.json",
          "json/yieldmaschine/good/02-DC-CustomTransaction.json",
          "validCreateVaultTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validDepositToVaultTest() {
    LOGGER.debug("validDepositToVaultTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/03-API-DepositToVault.json",
          "json/yieldmaschine/good/03-DC-Transaction.json",
          "json/yieldmaschine/good/03-DC-CustomTransaction.json",
          "validDepositToVaultTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validWithdrawFromVaultTest() {
    LOGGER.debug("validWithdrawFromVaultTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/04-API-WithdrawFromVault.json",
          "json/yieldmaschine/good/04-DC-Transaction.json",
          "json/yieldmaschine/good/04-DC-CustomTransaction.json",
          "validWithdrawFromVaultTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validTakeLoanTest() {
    LOGGER.debug("validTakeLoanTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/05-API-TakeLoan.json",
          "json/yieldmaschine/good/05-DC-Transaction.json",
          "json/yieldmaschine/good/05-DC-CustomTransaction.json",
          "validTakeLoanTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validPaybackLoanTest() {
    LOGGER.debug("validPaybackLoanTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/06-API-PaybackLoan.json",
          "json/yieldmaschine/good/06-DC-Transaction.json",
          "json/yieldmaschine/good/06-DC-CustomTransaction.json",
          "validPaybackLoanTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validPoolAddLiquidityTest() {
    LOGGER.debug("validPoolAddLiquidityTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/07-API-PoolAddLiquidity.json",
          "json/yieldmaschine/good/07-DC-Transaction.json",
          "json/yieldmaschine/good/07-DC-CustomTransaction.json",
          "validPoolAddLiquidityTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void validPoolRemoveLiquidityTest() {
    LOGGER.debug("validPoolRemoveLiquidityTest()");

    try {
      doValidTest(
          "json/yieldmaschine/good/08-API-PoolRemoveLiquidity.json",
          "json/yieldmaschine/good/08-DC-Transaction.json",
          "json/yieldmaschine/good/08-DC-CustomTransaction.json",
          "validPoolRemoveLiquidityTest-signature");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
  *
  */
  @Test
  public void invalidCreateVaultTest() {
    LOGGER.debug("invalidCreateVaultTest1()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/02-API-CreateVault.json",
          "json/yieldmaschine/invalid/02-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/02-DC-CustomTransaction.json",
          "invalidCreateVaultTest-signature-1",
          "[Transaction] ID: 7604d78bdb4bc72125ec5b8a7faf19bc434fc9ac31c0ba5f9dd7e00e3061c200 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/02-API-CreateVault.json",
          "json/yieldmaschine/good/02-DC-Transaction.json",
          "json/yieldmaschine/invalid/02-DC-CustomTransaction-wrong-address.json",
          "invalidCreateVaultTest-signature-2",
          "[Transaction] ID: 7604d78bdb4bc72125ec5b8a7faf19bc434fc9ac31c0ba5f9dd7e00e3061c200 - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidDepositToVaultTest() {
    LOGGER.debug("invalidDepositToVaultTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/03-API-DepositToVault.json",
          "json/yieldmaschine/invalid/03-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/03-DC-CustomTransaction.json",
          "invalidDepositToVaultTest-signature-1",
          "[Transaction] ID: 7a348073524fb60f87a07e626da8960eeeca15c01237f327dc44ed6fa4b3b7b1 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/03-API-DepositToVault.json",
          "json/yieldmaschine/good/03-DC-Transaction.json",
          "json/yieldmaschine/invalid/03-DC-CustomTransaction-wrong-address.json",
          "invalidDepositToVaultTest-signature-2",
          "[Transaction] ID: 7a348073524fb60f87a07e626da8960eeeca15c01237f327dc44ed6fa4b3b7b1 - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidWithdrawFromVaultTest() {
    LOGGER.debug("invalidWithdrawFromVaultTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/04-API-WithdrawFromVault.json",
          "json/yieldmaschine/invalid/04-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/04-DC-CustomTransaction.json",
          "invalidWithdrawFromVaultTest-signature-1",
          "[Transaction] ID: 2622c830c9005b8de22282ab2801e5ce7ee7773e87e2a8e339704234e455b106 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/04-API-WithdrawFromVault.json",
          "json/yieldmaschine/good/04-DC-Transaction.json",
          "json/yieldmaschine/invalid/04-DC-CustomTransaction-wrong-address.json",
          "invalidWithdrawFromVaultTest-signature-2",
          "[Transaction] ID: 2622c830c9005b8de22282ab2801e5ce7ee7773e87e2a8e339704234e455b106 - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidTakeLoanTest() {
    LOGGER.debug("invalidTakeLoanTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/05-API-TakeLoan.json",
          "json/yieldmaschine/invalid/05-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/05-DC-CustomTransaction.json",
          "invalidTakeLoanTest-signature-1",
          "[Transaction] ID: 2a4386b8ea235c9e76d4f1b907a12686297475143684f37cbede4c2c0864ee04 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/05-API-TakeLoan.json",
          "json/yieldmaschine/good/05-DC-Transaction.json",
          "json/yieldmaschine/invalid/05-DC-CustomTransaction-wrong-address.json",
          "invalidTakeLoanTest-signature-2",
          "[Transaction] ID: 2a4386b8ea235c9e76d4f1b907a12686297475143684f37cbede4c2c0864ee04 - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidPaybackLoanTest() {
    LOGGER.debug("invalidPaybackLoanTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/06-API-PaybackLoan.json",
          "json/yieldmaschine/invalid/06-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/06-DC-CustomTransaction.json",
          "invalidPaybackLoanTest-signature-1",
          "[Transaction] ID: 0a8761b329082913c5ed359f40c7d440cbc8d367a588c488a871d60e303715a6 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/06-API-PaybackLoan.json",
          "json/yieldmaschine/good/06-DC-Transaction.json",
          "json/yieldmaschine/invalid/06-DC-CustomTransaction-wrong-address.json",
          "invalidPaybackLoanTest-signature-2",
          "[Transaction] ID: 0a8761b329082913c5ed359f40c7d440cbc8d367a588c488a871d60e303715a6 - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidPoolAddLiquidityTest() {
    LOGGER.debug("invalidPoolAddLiquidityTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/07-API-PoolAddLiquidity.json",
          "json/yieldmaschine/invalid/07-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/07-DC-CustomTransaction.json",
          "invalidPoolAddLiquidityTest-signature-1",
          "[Transaction] ID: 431e43dde6fa1534dc8299ad898556b5b149f90a0ecaccbc1a06e093a5f3933c - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/07-API-PoolAddLiquidity.json",
          "json/yieldmaschine/good/07-DC-Transaction.json",
          "json/yieldmaschine/invalid/07-DC-CustomTransaction-wrong-address.json",
          "invalidPoolAddLiquidityTest-signature-2",
          "[Transaction] ID: 431e43dde6fa1534dc8299ad898556b5b149f90a0ecaccbc1a06e093a5f3933c - custom address not in whitelist");
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   *
   */
  @Test
  public void invalidPoolRemoveLiquidityTest() {
    LOGGER.debug("invalidPoolRemoveLiquidityTest()");

    try {
      doInvalidTest(
          "json/yieldmaschine/good/08-API-PoolRemoveLiquidity.json",
          "json/yieldmaschine/invalid/08-DC-Transaction-wrong-address.json",
          "json/yieldmaschine/good/08-DC-CustomTransaction.json",
          "invalidPoolRemoveLiquidityTest-signature-1",
          "[Transaction] ID: a7641e494c07b2543a78348dae81aa9eacf1a4be45173dbc5a4d353ab5fc1a18 - vout address not in whitelist");

      doInvalidTest(
          "json/yieldmaschine/good/08-API-PoolRemoveLiquidity.json",
          "json/yieldmaschine/good/08-DC-Transaction.json",
          "json/yieldmaschine/invalid/08-DC-CustomTransaction-wrong-address.json",
          "invalidPoolRemoveLiquidityTest-signature-2",
          "[Transaction] ID: a7641e494c07b2543a78348dae81aa9eacf1a4be45173dbc5a4d353ab5fc1a18 - custom address not in whitelist");
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
