package ch.dfx.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import ch.dfx.api.data.LoginDTO;
import ch.dfx.api.data.OpenTransactionDTOList;
import ch.dfx.api.data.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.OpenTransactionVerifiedDTO;
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

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");

  // ...
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
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/open";
      LOGGER.trace("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      logJSONResponse(jsonResponse);

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
  private void logJSONResponse(@Nonnull String jsonResponse) {
    LOGGER.trace("logJSONResponse() ...");

    try {
      String fileName =
          new StringBuilder()
              .append("transactions-")
              .append(DATE_FORMAT.format(new Date()))
              .append(".json")
              .toString();

      Path jsonLogFilePath = Path.of("", "logs", "json", fileName);
      String jsonResponsePrettyPrinted = gson.toJson(JsonParser.parseString(jsonResponse));

      if (!"[]".equals(jsonResponsePrettyPrinted)) {
        Files.createDirectories(jsonLogFilePath.getParent());
        Files.writeString(jsonLogFilePath, jsonResponsePrettyPrinted);
      }
    } catch (Exception e) {
      LOGGER.error("logJSONResponse", e);
    }
  }

  /**
   * 
   */
  public void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) throws DfxException {
    LOGGER.trace("sendOpenTransactionVerified() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/verified";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());

      String jsonOpenTransactionVerifiedDTO = gson.toJson(openTransactionVerifiedDTO);
      StringEntity entity = new StringEntity(jsonOpenTransactionVerifiedDTO, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = httpClient.execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("sendOpenTransactionVerified", e);
    }
  }

  /**
   * 
   */
  public void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) throws DfxException {
    LOGGER.trace("sendOpenTransactionInvalidated() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/invalidated";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());

      String jsonOpenTransactionVerifiedDTO = gson.toJson(openTransactionInvalidatedDTO);
      StringEntity entity = new StringEntity(jsonOpenTransactionVerifiedDTO, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = httpClient.execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("HTTP Status Code: " + statusCode);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("sendOpenTransactionInvalidated", e);
    }
  }

//  /**
//   * 
//   */
//  public void postOpenTransactionDTOList(@Nonnull OpenTransactionDTOList openTransactionDTOList) throws DfxException {
//    LOGGER.trace("postOpenTransactionDTOList() ...");
//
//    try {
//      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transactions/open";
//      LOGGER.trace("URL: " + url);
//
//      HttpPost httpPost = new HttpPost(url);
//      httpPost.addHeader("Authorization", "Bearer " + loginDTO.getAccessToken());
//
//      String jsonOpenTransactionDTOList = gson.toJson(openTransactionDTOList);
//      StringEntity entity = new StringEntity(jsonOpenTransactionDTOList, ContentType.APPLICATION_JSON);
//      httpPost.setEntity(entity);
//
////      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
////
////      multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
////      multipartEntityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
////      
////      // ...
////      HttpEntity multipartEntity = multipartEntityBuilder.build();
////
////      httpPost.setEntity(multipartEntity);
//
//      // ...
//      HttpResponse httpResponse = httpClient.execute(httpPost);
//
//      int statusCode = httpResponse.getStatusLine().getStatusCode();
//
//      if (HttpStatus.SC_OK != statusCode) {
//        throw new DfxException("HTTP Status Code: " + statusCode);
//      }
//    } catch (DfxException e) {
//      throw e;
//    } catch (Exception e) {
//      throw new DfxException("postOpenTransactionDTOList", e);
//    }
//  }

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
