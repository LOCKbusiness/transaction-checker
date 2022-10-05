package ch.dfx.lockbusiness.stakingbalances.api;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;

/**
 * 
 * https://app-lock-api-dev.azurewebsites.net/swagger/
 * https://app-dfx-api-dev.azurewebsites.net/swagger/
 * 
 */
public class ApiAccessManager {
  private static final Logger LOGGER = LogManager.getLogger(ApiAccessManager.class);

  private final HttpClient httpClient;

  private final Gson gson;

  private ApiLoginData loginData = null;

  /**
   * 
   */
  public ApiAccessManager() {
    this.httpClient = HttpClientBuilder.create().build();

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void signIn() throws DfxException {
    LOGGER.trace("signIn() ...");

    try {
      // ...
      HttpClient httpClient = HttpClientBuilder.create().build();

      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/auth/sign-in";
      LOGGER.trace("URL: " + url);
      HttpPost httpPost = new HttpPost(url);

      loginData = createLoginData();

      String jsonLoginData = gson.toJson(loginData);
      StringEntity stringEntity = new StringEntity(jsonLoginData, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);

      HttpResponse httpResponse = httpClient.execute(httpPost);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_CREATED != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      LOGGER.trace(jsonResponse);

      AccessTokenData accessTokenData = gson.fromJson(jsonResponse, AccessTokenData.class);
      loginData.setAccessToken(accessTokenData.accessToken);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("signIn", e);
    }
  }

  /**
   * 
   */
  public void getCustomerWalletList() throws DfxException {
    LOGGER.trace("getCustomerWalletList() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/analytics/staking";
      LOGGER.trace("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + loginData.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      Map resultMap = gson.fromJson(jsonResponse, Map.class);
      LOGGER.debug(resultMap);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getCustomerWalletList", e);
    }
  }

  /**
   * 
   */

  /**
   * 
   */
  private ApiLoginData createLoginData() {
    ApiLoginData loginData = new ApiLoginData();

    loginData.setAddress(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_ADDRESS));
    loginData.setSignature(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_SIGNATURE));

    return loginData;
  }

  /**
   * 
   */
  private class AccessTokenData {
    private String accessToken;
  }
}
