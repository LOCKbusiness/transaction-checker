package ch.dfx.defichain.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class DefiDataProviderTest {

  private static final byte CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS = 0x61;
  private static final byte CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT = 0x42;

  private static DefiDataProvider dataProvider = null;

  @BeforeClass
  public static void beforeClass() {
    HttpClient httpClient = mock(HttpClient.class);
    HttpPost httpPost = mock(HttpPost.class);

    dataProvider = new DefiDataProviderImpl(httpClient, httpPost);
  }

  @Test
  public void noCustomData() throws DfxException {
    String textData =
        "604c7644665478610217a9142c";

    assertFalse("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS);
    assertFalse("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT);
  }

  @Test
  public void noCustomDataType() throws DfxException {
    String textData =
        "6a4b44665478330217a9142c";

    assertFalse("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS);
    assertFalse("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT);
  }

  @Test
  public void customDataTest() throws DfxException {
    String textData =
        "6a254466547842160014bf9ff04fa04e90";

    assertTrue("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT);
  }

  @Test
  public void customDataOpPushData1Test() throws DfxException {
    String textData =
        "6a4c7644665478610217a9142c";

    assertTrue("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS);
  }

  @Test
  public void customDataOpPushdata2Test() throws DfxException {
    String textData =
        "6a4d85014466547842160014bf9ff04fa04e90";

    assertTrue("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT);
  }

  @Test
  public void customDataOpPushdata4Test() throws DfxException {
    String textData =
        "6a4e001122334466547861160014bf9ff04fa04e90";

    assertTrue("Matcher matches", dataProvider.getCustomType(textData) == CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS);
  }
}
