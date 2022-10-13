package ch.dfx.api;

import java.util.Objects;

import javax.annotation.Nonnull;

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

import ch.dfx.api.data.LoginDTO;
import ch.dfx.api.data.OpenTransactionDTOList;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;

/**
 * 
 * https://app-lock-api-dev.azurewebsites.net/swagger/
 * https://app-dfx-api-dev.azurewebsites.net/swagger/
 * 
 */
public class ApiAccessHandler {
  private static final Logger LOGGER = LogManager.getLogger(ApiAccessHandler.class);

  private final HttpClient httpClient;

  private final Gson gson;

  private LoginDTO loginDTO = null;

  /**
   * 
   */
  public ApiAccessHandler() {
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

      loginDTO = createLoginData();

      String jsonLoginData = gson.toJson(loginDTO);
      StringEntity stringEntity = new StringEntity(jsonLoginData, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);

      HttpResponse httpResponse = httpClient.execute(httpPost);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_CREATED != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      LOGGER.trace(jsonResponse);

      AccessTokenDTO accessTokenDTO = gson.fromJson(jsonResponse, AccessTokenDTO.class);
      loginDTO.setAccessToken(accessTokenDTO.accessToken);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("signIn", e);
    }
  }

  /**
   * 
   */
  public OpenTransactionDTOList getOpenTransactionDTOList() throws DfxException {
    Objects.requireNonNull(loginDTO, "null loginDTO: first call signIn()");
    LOGGER.trace("getOpenTransactionDTOList() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transactions/open";
      LOGGER.trace("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      return gson.fromJson(jsonResponse, OpenTransactionDTOList.class);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getOpenTransactionDTOList", e);
    }
  }

  /**
   * 
   */
  public void postOpenTransactionDTOList(@Nonnull OpenTransactionDTOList openTransactionDTOList) throws DfxException {
    LOGGER.trace("postOpenTransactionDTOList() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transactions/open";
      LOGGER.trace("URL: " + url);

      HttpPost httpPost = new HttpPost(url);
      httpPost.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());

      String jsonOpenTransactionDTOList = gson.toJson(openTransactionDTOList);
      StringEntity entity = new StringEntity(jsonOpenTransactionDTOList, ContentType.APPLICATION_JSON);
      httpPost.setEntity(entity);

//      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
//
//      multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//      multipartEntityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
//      
//      // ...
//      HttpEntity multipartEntity = multipartEntityBuilder.build();
//
//      httpPost.setEntity(multipartEntity);

      // ...
      HttpResponse httpResponse = httpClient.execute(httpPost);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getOpenTransactionDTOList", e);
    }
  }

  /**
   * 
   */

  /**
   * 
   */
  private LoginDTO createLoginData() {
    LoginDTO loginData = new LoginDTO();

    loginData.setAddress(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_ADDRESS));
    loginData.setSignature(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_SIGNATURE));

    return loginData;
  }

  /**
   * 
   */
  private class AccessTokenDTO {
    private String accessToken;
  }
}
