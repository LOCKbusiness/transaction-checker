package ch.dfx.manager.checker.withdrawal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.dfx.TestUtils;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionTypeEnum;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.manager.OpenTransactionManagerTest;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingBalanceCheckerTest {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerTest.class);

  // ...
  public static boolean isSuiteContext = false;

  // ...
  private static H2DBManager databaseManagerMock = null;

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
    TestUtils.sqlDelete("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");
    TestUtils.sqlDelete("public.staking", "liquidity_address_number=1 AND deposit_address_number=2 AND customer_address_number=4");
  }

  @Test
  public void validBalanceTest() {
    LOGGER.debug("validBalanceTest()");

    try {
      TestUtils.sqlInsert(
          "public.staking",
          "liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout",
          "1, 2, 4, 0, 175.00000000, 0, 25.00000000");

      // ...
      int withdrawalId = 111;
      String transactionId = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId, transactionId, new BigDecimal("150.00000000")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 1, reservedAfterDataList.size());

      Map<String, Object> dataMap = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId), dataMap.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId, dataMap.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "150.00000000", dataMap.get("VOUT").toString());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 1, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 1, resultTransactionWithdrawalDTOList.size());

      assertEquals("Lists", transactionWithdrawalDTOList.toString(), resultTransactionWithdrawalDTOList.toString());

      TransactionWithdrawalDTO transactionWithdrawalDTO = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO.getState());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void validBalanceEqualWithdrawalTest() {
    LOGGER.debug("validBalanceEqualWithdrawalTest()");

    try {
      TestUtils.sqlInsert(
          "public.staking",
          "liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout",
          "1, 2, 4, 0, 175.00000000, 0, 25.00000000");

      // ...
      int withdrawalId = 112;
      String transactionId = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId, transactionId, new BigDecimal("150.00000000")));
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId, transactionId, new BigDecimal("150.00000000")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 1, reservedAfterDataList.size());

      Map<String, Object> dataMap = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId), dataMap.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId, dataMap.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "150.00000000", dataMap.get("VOUT").toString());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 2, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 2, resultTransactionWithdrawalDTOList.size());

      assertEquals("Lists", transactionWithdrawalDTOList.toString(), resultTransactionWithdrawalDTOList.toString());

      TransactionWithdrawalDTO transactionWithdrawalDTO1 = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO1.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO1.getState());

      TransactionWithdrawalDTO transactionWithdrawalDTO2 = transactionWithdrawalDTOList.get(1);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO2.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO2.getState());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void validBalanceDifferentWithdrawalTest() {
    LOGGER.debug("validBalanceDifferentWithdrawalTest()");

    try {
      TestUtils.sqlInsert(
          "public.staking",
          "liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout",
          "1, 2, 4, 0, 175.00000000, 0, 25.00000000");

      // ...
      int withdrawalId1 = 113;
      String transactionId1 = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      int withdrawalId2 = 114;
      String transactionId2 = "c6984ef476ab5c62996624a34e565fb30c62cfc10f7206d6a118cf94ec640649";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId1, transactionId1, new BigDecimal("100.00000000")));
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId2, transactionId2, new BigDecimal("50.00000000")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 2, reservedAfterDataList.size());

      Map<String, Object> dataMap1 = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId1), dataMap1.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId1, dataMap1.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap1.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "100.00000000", dataMap1.get("VOUT").toString());

      Map<String, Object> dataMap2 = reservedAfterDataList.get(1);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId2), dataMap2.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId2, dataMap2.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap2.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "50.00000000", dataMap2.get("VOUT").toString());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 2, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 2, resultTransactionWithdrawalDTOList.size());

      assertEquals("Lists", transactionWithdrawalDTOList.toString(), resultTransactionWithdrawalDTOList.toString());

      TransactionWithdrawalDTO transactionWithdrawalDTO1 = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO1.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO1.getState());

      TransactionWithdrawalDTO transactionWithdrawalDTO2 = transactionWithdrawalDTOList.get(1);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO2.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO2.getState());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidBalanceNoStakingTest() {
    LOGGER.debug("invalidBalanceNoStakingTest()");

    try {
      // ...
      int withdrawalId = 115;
      String transactionId = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId, transactionId, new BigDecimal("150.00000000")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 0, reservedAfterDataList.size());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 1, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 0, resultTransactionWithdrawalDTOList.size());

      TransactionWithdrawalDTO transactionWithdrawalDTO = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", "[Withdrawal] ID: " + withdrawalId + " - invalid balance", transactionWithdrawalDTO.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.INVALID, transactionWithdrawalDTO.getState());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidBalanceLessStakingTest() {
    LOGGER.debug("invalidBalanceLessStakingTest()");

    try {
      TestUtils.sqlInsert(
          "public.staking",
          "liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout",
          "1, 2, 4, 0, 175.00000000, 0, 25.00000001");

      // ...
      int withdrawalId = 116;
      String transactionId = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId, transactionId, new BigDecimal("150.00000000")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 0, reservedAfterDataList.size());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 1, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 0, resultTransactionWithdrawalDTOList.size());

      TransactionWithdrawalDTO transactionWithdrawalDTO = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", "[Withdrawal] ID: " + withdrawalId + " - invalid balance", transactionWithdrawalDTO.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.INVALID, transactionWithdrawalDTO.getState());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  @Test
  public void invalidBalanceDifferentWithdrawalTest() {
    LOGGER.debug("invalidBalanceDifferentWithdrawalTest()");

    try {
      TestUtils.sqlInsert(
          "public.staking",
          "liquidity_address_number, deposit_address_number, customer_address_number, last_in_block_number, vin, last_out_block_number, vout",
          "1, 2, 4, 0, 175.00000000, 0, 25.00000000");

      // ...
      int withdrawalId1 = 117;
      String transactionId1 = "ab75298a8be37341da68e341d3257b03abc33431280d24673d6cbe93aacb423c";

      int withdrawalId2 = 118;
      String transactionId2 = "c6984ef476ab5c62996624a34e565fb30c62cfc10f7206d6a118cf94ec640649";

      int withdrawalId3 = 119;
      String transactionId3 = "68a10d0e466cdc9cba1d0cb39f355b268ce8072c710c064a058883857ed459ab";

      TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId1, transactionId1, new BigDecimal("100.00000000")));
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId2, transactionId2, new BigDecimal("50.00000000")));
      transactionWithdrawalDTOList.add(createTransactionWithdrawalDTO(withdrawalId3, transactionId3, new BigDecimal("0.00000001")));

      // ...
      List<Map<String, Object>> reservedBeforeDataList =
          TestUtils.sqlSelect("public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      StakingBalanceChecker stakingBalanceChecker = new StakingBalanceChecker(databaseManagerMock);

      TransactionWithdrawalDTOList resultTransactionWithdrawalDTOList =
          stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);

      List<Map<String, Object>> reservedAfterDataList =
          TestUtils.sqlSelect(
              "public.staking_withdrawal_reserved", "customer_address='" + CUSTOMER_ADDRESS + "'");

      // ...
      assertEquals("staking_withdrawal_reserved Size", 0, reservedBeforeDataList.size());
      assertEquals("staking_withdrawal_reserved Size", 2, reservedAfterDataList.size());

      Map<String, Object> dataMap1 = reservedAfterDataList.get(0);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId1), dataMap1.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId1, dataMap1.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap1.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "100.00000000", dataMap1.get("VOUT").toString());

      Map<String, Object> dataMap2 = reservedAfterDataList.get(1);
      assertEquals("WITHDRAWAL_ID", Integer.toString(withdrawalId2), dataMap2.get("WITHDRAWAL_ID").toString());
      assertEquals("TRANSACTION_ID", transactionId2, dataMap2.get("TRANSACTION_ID"));
      assertEquals("CUSTOMER_ADDRESS", CUSTOMER_ADDRESS, dataMap2.get("CUSTOMER_ADDRESS"));
      assertEquals("VOUT", "50.00000000", dataMap2.get("VOUT").toString());

      // ...
      assertEquals("TransactionWithdrawalDTO List Size", 3, transactionWithdrawalDTOList.size());
      assertEquals("Result TransactionWithdrawalDTO List Size", 2, resultTransactionWithdrawalDTOList.size());

      assertEquals("Lists", transactionWithdrawalDTOList.get(0).toString(), resultTransactionWithdrawalDTOList.get(0).toString());
      assertEquals("Lists", transactionWithdrawalDTOList.get(1).toString(), resultTransactionWithdrawalDTOList.get(1).toString());

      TransactionWithdrawalDTO transactionWithdrawalDTO1 = transactionWithdrawalDTOList.get(0);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO1.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO1.getState());

      TransactionWithdrawalDTO transactionWithdrawalDTO2 = transactionWithdrawalDTOList.get(1);
      assertEquals("TransactionWithdrawalDTO Reason", null, transactionWithdrawalDTO2.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.BALANCE_CHECKED, transactionWithdrawalDTO2.getState());

      TransactionWithdrawalDTO transactionWithdrawalDTO3 = transactionWithdrawalDTOList.get(2);
      assertEquals("TransactionWithdrawalDTO Reason", "[Withdrawal] ID: " + withdrawalId3 + " - invalid balance", transactionWithdrawalDTO3.getStateReason());
      assertEquals("TransactionWithdrawalDTO State", TransactionWithdrawalStateEnum.INVALID, transactionWithdrawalDTO3.getState());

    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }

  /**
   * 
   */
  private TransactionWithdrawalDTO createTransactionWithdrawalDTO(
      int withdrawalId,
      @Nonnull String transactionId,
      @Nonnull BigDecimal amount) {
    TransactionWithdrawalDTO transactionWithdrawalDTO = new TransactionWithdrawalDTO();

    transactionWithdrawalDTO.setId(withdrawalId);
    transactionWithdrawalDTO.setCustomerAddress(CUSTOMER_ADDRESS);
    transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.SIGNATURE_CHECKED);

    OpenTransactionDTO openTransactionDTO = new OpenTransactionDTO();
    openTransactionDTO.setType(OpenTransactionTypeEnum.WITHDRAWAL);
    openTransactionDTO.setId(transactionId);

    transactionWithdrawalDTO.setOpenTransactionDTO(openTransactionDTO);

    PendingWithdrawalDTO pendingWithdrawalDTO = new PendingWithdrawalDTO();
    pendingWithdrawalDTO.setId(withdrawalId);
    pendingWithdrawalDTO.setAmount(amount);

    transactionWithdrawalDTO.setPendingWithdrawalDTO(pendingWithdrawalDTO);

    return transactionWithdrawalDTO;
  }
}
