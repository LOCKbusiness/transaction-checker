package ch.dfx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ch.dfx.manager.OpenTransactionManagerMasternodeTest;
import ch.dfx.manager.OpenTransactionManagerTest;
import ch.dfx.manager.OpenTransactionManagerUtxoTest;
import ch.dfx.manager.checker.withdrawal.StakingBalanceCheckerTest;
import ch.dfx.transactionserver.cleaner.StakingWithdrawalReservedCleanerTest;

// ...
@RunWith(Suite.class)

// ...
@Suite.SuiteClasses({
    StakingBalanceCheckerTest.class,
    StakingWithdrawalReservedCleanerTest.class,

    OpenTransactionManagerTest.class,
    OpenTransactionManagerUtxoTest.class,
    OpenTransactionManagerMasternodeTest.class
})

public class TestSuite {

  @BeforeClass
  public static void globalSetup() throws Exception {
    TestUtils.globalSetup("testsuite", true);

    StakingBalanceCheckerTest.isSuiteContext = true;
    StakingWithdrawalReservedCleanerTest.isSuiteContext = true;

    OpenTransactionManagerTest.isSuiteContext = true;
    OpenTransactionManagerUtxoTest.isSuiteContext = true;
    OpenTransactionManagerMasternodeTest.isSuiteContext = true;
  }

  @AfterClass
  public static void globalCleanup() {
    TestUtils.globalCleanup();
  }
}
