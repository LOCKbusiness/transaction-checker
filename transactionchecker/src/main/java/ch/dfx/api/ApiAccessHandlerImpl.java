package ch.dfx.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;
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
public class ApiAccessHandlerImpl implements ApiAccessHandler {
  private static final Logger LOGGER = LogManager.getLogger(ApiAccessHandlerImpl.class);

  // ...
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
  private int logCounter = 1;
  private final Set<String> uniqueLogSet;

  // ...
  private final Base64.Decoder urlDecoder;

  private final HttpClient httpClient;

  private final Gson gson;

  // ...
  private SignInDTO signInDTO = null;

  /**
   * 
   */
  public ApiAccessHandlerImpl() {
    this.uniqueLogSet = new HashSet<>();

    this.urlDecoder = Base64.getUrlDecoder();

    this.httpClient = HttpClientBuilder.create().build();

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  @Override
  public void resetSignIn() {
    signInDTO = null;
  }

  /**
   * 
   */
  @Override
  public void signIn() {
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

        if (HttpStatus.SC_CREATED == statusCode) {
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
        } else {
          // TODO: Error Handling:
          // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
          LOGGER.error("[SignIn] HTTP Status Code: " + statusCode);
          resetSignIn();
        }
      }
    } catch (Exception e) {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("signIn", e);
      resetSignIn();
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
  @Override
  public OpenTransactionDTOList getOpenTransactionDTOList() {
    LOGGER.trace("getOpenTransactionDTOList() ...");

    OpenTransactionDTOList openTransactionDTOList = new OpenTransactionDTOList();

    if (null != signInDTO) {
      fillOpenTransactionDTOList(openTransactionDTOList);
    } else {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("[Transaction] no SignIn Data found");
    }

    return openTransactionDTOList;
  }

  /**
   * 
   */
  private void fillOpenTransactionDTOList(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("fillOpenTransactionDTOList() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/transaction/open";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK == statusCode) {
        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        logJSON("transaction", jsonResponse);

        openTransactionDTOList.addAll(gson.fromJson(jsonResponse, OpenTransactionDTOList.class));
      } else {
        // TODO: Error Handling:
        // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
        LOGGER.error("[Transaction] HTTP Status Code: " + statusCode);
      }
    } catch (Exception e) {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("fillOpenTransactionDTOList", e);
    }
  }

  /**
   * 
   */
  @Override
  public PendingWithdrawalDTOList getPendingWithdrawalDTOList() {
    LOGGER.trace("getPendingWithdrawalDTOList() ...");

    PendingWithdrawalDTOList pendingWithdrawalDTOList = new PendingWithdrawalDTOList();

    if (null != signInDTO) {
      fillPendingWithdrawalDTOList(pendingWithdrawalDTOList);
    } else {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("[Withdrawal] no SignIn Data found");
    }

    return pendingWithdrawalDTOList;
  }

  /**
   * 
   */
  private void fillPendingWithdrawalDTOList(@Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    LOGGER.trace("fillPendingWithdrawalDTOList() ...");

    try {
      String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.LOCK_API_URL) + "/withdrawal/pending";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());

      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK == statusCode) {
        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        logJSON("withdrawal", jsonResponse);

        pendingWithdrawalDTOList.addAll(gson.fromJson(jsonResponse, PendingWithdrawalDTOList.class));
      } else {
        // TODO: Error Handling:
        // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
        LOGGER.error("[Withdrawal] HTTP Status Code: " + statusCode);
      }
    } catch (Exception e) {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("getPendingWithdrawalDTOList", e);
    }
  }

  /**
   * 
   */
  @Override
  public void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) {
    LOGGER.trace("sendOpenTransactionVerified() ...");

    if (null != signInDTO) {
      doSendOpenTransactionVerified(openTransactionId, openTransactionVerifiedDTO);
    } else {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("[Verified] no SignIn Data found");
    }
  }

  /**
   * 
   */
  private void doSendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) {
    LOGGER.trace("doSendOpenTransactionVerified() ...");

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
        // TODO: Error Handling:
        // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
        LOGGER.error("[Verified] HTTP Status Code: " + statusCode);
      }
    } catch (Exception e) {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("sendOpenTransactionVerified", e);
    }
  }

  /**
   * 
   */
  @Override
  public void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) {
    LOGGER.trace("sendOpenTransactionInvalidated() ...");

    if (null != signInDTO) {
      doSendOpenTransactionInvalidated(openTransactionId, openTransactionInvalidatedDTO);
    } else {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("[Invalidated] no SignIn Data found");
    }
  }

  /**
   * 
   */
  private void doSendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) {
    LOGGER.trace("doSendOpenTransactionInvalidated() ...");

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
        // TODO: Error Handling:
        // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
        LOGGER.error("[Invalidated] HTTP Status Code: " + statusCode);
      }
    } catch (Exception e) {
      // TODO: Error Handling:
      // TODO: Count Error and send via E-Mail, if a defined number of errors occurs ...
      LOGGER.error("sendOpenTransactionInvalidated", e);
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
              .append(logCounter)
              .append(".json")
              .toString();

      Path jsonLogFilePath = Path.of("", "logs", "json", type, fileName);

      // ...
      int jsonStringLengthMB = jsonString.length() / 1024 / 1024;
      String jsonPrettyPrinted = 10 < jsonStringLengthMB ? jsonString : gson.toJson(JsonParser.parseString(jsonString));

      // ...
      String sha256Hex = DigestUtils.sha256Hex(jsonPrettyPrinted);

      if (uniqueLogSet.add(sha256Hex)) {
        if (!"[]".equals(jsonPrettyPrinted)) {
          Files.createDirectories(jsonLogFilePath.getParent());
          Files.writeString(jsonLogFilePath, jsonPrettyPrinted);

          logCounter++;
        }
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
