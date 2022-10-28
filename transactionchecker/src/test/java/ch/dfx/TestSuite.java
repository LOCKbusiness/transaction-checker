package ch.dfx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ch.dfx.manager.OpenTransactionManagerMasternodeTest;
import ch.dfx.manager.OpenTransactionManagerTest;
import ch.dfx.manager.OpenTransactionManagerUtxoTest;

// ...
@RunWith(Suite.class)

// ...
@Suite.SuiteClasses({
    OpenTransactionManagerTest.class,
    OpenTransactionManagerUtxoTest.class,
    OpenTransactionManagerMasternodeTest.class
})

public class TestSuite {

  @BeforeClass
  public static void globalSetup() throws Exception {
    TestUtils.globalSetup("testsuite");

    OpenTransactionManagerTest.isSuiteContext = true;
    OpenTransactionManagerUtxoTest.isSuiteContext = true;
    OpenTransactionManagerMasternodeTest.isSuiteContext = true;
  }

  @AfterClass
  public static void globalCleanup() {
    TestUtils.globalCleanup();
  }
}
