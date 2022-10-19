package ch.dfx.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
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

import ch.dfx.api.data.signin.SignInAccessTokenHeaderDTO;
import ch.dfx.api.data.signin.SignInAccessTokenPayloadDTO;
import ch.dfx.api.data.signin.SignInDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
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
  private final Base64.Decoder urlDecoder;

  private final HttpClient httpClient;

  private final Gson gson;

  // ...
  private SignInDTO signInDTO = null;

  /**
   * 
   */
  public ApiAccessHandler() {
    this.urlDecoder = Base64.getUrlDecoder();

    this.httpClient = HttpClientBuilder.create().build();

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void fakeForTest() {
    String testToken = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_TEST_TOKEN);

    signInDTO = createLoginData();
    signInDTO.setAccessToken(testToken);
  }

  /**
   * 
   */
  public void resetSignIn() {
    signInDTO = null;
  }

  /**
   * 
   */
  public void signIn() throws DfxException {
    LOGGER.trace("signIn() ...");

    try {
      if (isAccessTokenExpired()) {
        // ...
        String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/auth/sign-in";
        LOGGER.debug("URL: " + url);
        HttpPost httpPost = new HttpPost(url);

        signInDTO = createLoginData();

        String jsonLoginData = gson.toJson(signInDTO);
        StringEntity stringEntity = new StringEntity(jsonLoginData, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        HttpResponse httpResponse = httpClient.execute(httpPost);

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        if (HttpStatus.SC_CREATED != statusCode) {
          throw new DfxException("HTTP Status Code: " + statusCode);
        }

        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        LOGGER.trace(jsonResponse);

        // ...
        AccessTokenDTO accessTokenDTO = gson.fromJson(jsonResponse, AccessTokenDTO.class);
        signInDTO.setAccessToken(accessTokenDTO.accessToken);

        // ...
        String[] accessTokenSplit = accessTokenDTO.accessToken.split("\\.");

        if (2 > accessTokenSplit.length) {
          throw new DfxException("unknown access token format");
        }

        String header = new String(urlDecoder.decode(accessTokenSplit[0]));
        String payload = new String(urlDecoder.decode(accessTokenSplit[1]));

        signInDTO.setAccessTokenHeader(gson.fromJson(header, SignInAccessTokenHeaderDTO.class));
        signInDTO.setAccessTokenPayload(gson.fromJson(payload, SignInAccessTokenPayloadDTO.class));
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("signIn", e);
    }
  }

  /**
   * Get the expired time from the access token payload.
   * Subtract a time tolerance (e.g. 10 minutes) from the expired time.
   * Get the current system time (in seconds)
   * Check, if the current time is before the expired time.
   */
  private boolean isAccessTokenExpired() throws DfxException {
    LOGGER.trace("isAccessTokenExpired() ...");

    try {
      boolean isExpired = true;

      if (null != signInDTO) {
        SignInAccessTokenPayloadDTO accessTokenPayloadDTO = signInDTO.getAccessTokenPayload();

        if (null != accessTokenPayloadDTO) {
          Long payloadExpired = accessTokenPayloadDTO.getExp();

          if (null != payloadExpired) {
            long expiredTime = payloadExpired.longValue() - (10 * 60); // minus 10 minutes ...;
            long currentTime = System.currentTimeMillis() / 1000;

            isExpired = currentTime > expiredTime;
          }
        }
      }

      return isExpired;
    } catch (Exception e) {
      throw new DfxException("isAccessTokenExpired", e);
    }
  }

  /**
   *
   */
  public OpenTransactionDTOList getOpenTransactionDTOList() throws DfxException {
    LOGGER.trace("getOpenTransactionDTOList() ...");
    Objects.requireNonNull(signInDTO, "null signInDTO: first call signIn()");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/open";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("[Transaction] HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      logJSON("transaction", jsonResponse);

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
  public PendingWithdrawalDTOList getPendingWithdrawalDTOList() throws DfxException {
    LOGGER.trace("getPendingWithdrawalDTOList() ...");
    Objects.requireNonNull(signInDTO, "null signInDTO: first call signIn()");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/withdrawal/pending";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("[Withdrawal] HTTP Status Code: " + statusCode);
      }

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      logJSON("withdrawal", jsonResponse);

      return gson.fromJson(jsonResponse, PendingWithdrawalDTOList.class);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getPendingWithdrawalDTOList", e);
    }
  }

  /**
   * 
   */
  public void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) throws DfxException {
    LOGGER.trace("sendOpenTransactionVerified() ...");
    Objects.requireNonNull(signInDTO, "null signInDTO: first call signIn()");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/verified";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      String jsonOpenTransactionVerified = gson.toJson(openTransactionVerifiedDTO);
      logJSON("verified", jsonOpenTransactionVerified);

      StringEntity entity = new StringEntity(jsonOpenTransactionVerified, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = httpClient.execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("[Verified] HTTP Status Code: " + statusCode);
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
    Objects.requireNonNull(signInDTO, "null signInDTO: first call signIn()");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/invalidated";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      String jsonOpenTransactionInvalidated = gson.toJson(openTransactionInvalidatedDTO);
      logJSON("invalidated", jsonOpenTransactionInvalidated);

      StringEntity entity = new StringEntity(jsonOpenTransactionInvalidated, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = httpClient.execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        throw new DfxException("[Invalidated] HTTP Status Code: " + statusCode);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("sendOpenTransactionInvalidated", e);
    }
  }

  /**
   * 
   */
  private void logJSON(
      @Nonnull String type,
      @Nonnull String jsonString) {
    LOGGER.trace("logJSON() ...");

    try {
      String fileName =
          new StringBuilder()
              .append(type)
              .append("-").append(DATE_FORMAT.format(new Date()))
              .append(".json")
              .toString();

      Path jsonLogFilePath = Path.of("", "logs", "json", type, fileName);
      String jsonPrettyPrinted = gson.toJson(JsonParser.parseString(jsonString));

      if (!"[]".equals(jsonPrettyPrinted)) {
        Files.createDirectories(jsonLogFilePath.getParent());
        Files.writeString(jsonLogFilePath, jsonPrettyPrinted);
      }
    } catch (Exception e) {
      LOGGER.error("logJSON", e);
    }
  }

  /**
   * 
   */
  private SignInDTO createLoginData() {
    SignInDTO loginData = new SignInDTO();

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
