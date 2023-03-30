package ch.dfx.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
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
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 * https://app-lock-api-dev.azurewebsites.net/swagger/
 * https://app-dfx-api-dev.azurewebsites.net/swagger/
 * 
 */
public class ApiAccessHandlerImpl implements ApiAccessHandler {
  private static final Logger LOGGER = LogManager.getLogger(ApiAccessHandlerImpl.class);

  // ...
  private static final int HTTP_CLIENT_TIMEOUT = 10 * 1000;

  // ...
  private final NetworkEnum network;

  // ...
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
  private int logCounter = 1;
  private final Set<String> uniqueLogSet;

  // ...
  private final Base64.Decoder urlDecoder;

  private final Gson gson;

  // ...
  private HttpClient httpClient = null;
  private SignInDTO signInDTO = null;

  /**
   * 
   */
  public ApiAccessHandlerImpl(@Nonnull NetworkEnum network) {
    Objects.requireNonNull(network, "null 'network' not allowed");
    this.network = network;

    this.uniqueLogSet = new HashSet<>();

    this.urlDecoder = Base64.getUrlDecoder();

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  @Override
  public void resetSignIn() {
    httpClient = null;
    signInDTO = null;
  }

  /**
   * 
   */
  @Override
  public void signIn() {
    LOGGER.trace("signIn()");

    try {
      if (isAccessTokenExpired()) {
        String serverId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.SERVER_ID);
        String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/auth/sign-in";
        LOGGER.debug("URL: " + url);

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Device-Id", serverId);

        signInDTO = createLoginData();

        String jsonLoginData = gson.toJson(signInDTO);
        StringEntity stringEntity = new StringEntity(jsonLoginData, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        HttpResponse httpResponse = getHttpClient().execute(httpPost);

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
          LOGGER.error("[SignIn] HTTP Status Code: " + statusCode);
          resetSignIn();
        }
      }
    } catch (Exception e) {
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
    LOGGER.trace("isAccessTokenExpired()");

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
    LOGGER.trace("getOpenTransactionDTOList()");

    OpenTransactionDTOList openTransactionDTOList = new OpenTransactionDTOList();

    if (null != signInDTO) {
      fillOpenTransactionDTOList(openTransactionDTOList);
    } else {
      LOGGER.error("[Transaction] no SignIn Data found");
    }

    return openTransactionDTOList;
  }

  /**
   * 
   */
  private void fillOpenTransactionDTOList(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("fillOpenTransactionDTOList()");

    try {
      String serverId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.SERVER_ID);
      String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/transaction/open";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());
      httpGet.addHeader("Device-Id", serverId);

      HttpResponse httpResponse = getHttpClient().execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK == statusCode) {
        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        logJSON("transaction", jsonResponse);

        openTransactionDTOList.addAll(gson.fromJson(jsonResponse, OpenTransactionDTOList.class));
      } else {
        LOGGER.error("[Transaction] HTTP Status Code: " + statusCode);
        httpClient = null;
      }
    } catch (Exception e) {
      LOGGER.error("fillOpenTransactionDTOList", e);
      resetSignIn();
    }
  }

  /**
   * 
   */
  @Override
  public PendingWithdrawalDTOList getPendingWithdrawalDTOList() {
    LOGGER.trace("getPendingWithdrawalDTOList()");

    PendingWithdrawalDTOList pendingWithdrawalDTOList = new PendingWithdrawalDTOList();

    if (null != signInDTO) {
      fillPendingWithdrawalDTOList(pendingWithdrawalDTOList);
    } else {
      LOGGER.error("[Withdrawal] no SignIn Data found");
    }

    return pendingWithdrawalDTOList;
  }

  /**
   * 
   */
  private void fillPendingWithdrawalDTOList(@Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    LOGGER.trace("fillPendingWithdrawalDTOList()");

    try {
      String serverId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.SERVER_ID);
      String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/withdrawal/pending";
      LOGGER.debug("URL: " + url);

      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());
      httpGet.addHeader("Device-Id", serverId);

      HttpResponse httpResponse = getHttpClient().execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK == statusCode) {
        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        logJSON("withdrawal", jsonResponse);

        pendingWithdrawalDTOList.addAll(gson.fromJson(jsonResponse, PendingWithdrawalDTOList.class));
      } else {
        LOGGER.error("[Withdrawal] HTTP Status Code: " + statusCode);
        httpClient = null;
      }
    } catch (Exception e) {
      LOGGER.error("getPendingWithdrawalDTOList", e);
      resetSignIn();
    }
  }

  /**
   * 
   */
  @Override
  public void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) {
    LOGGER.trace("sendOpenTransactionVerified()");

    if (null != signInDTO) {
      String jsonOpenTransactionVerified = gson.toJson(openTransactionVerifiedDTO);
      logJSON("verified", jsonOpenTransactionVerified);

      boolean isSimulateSend = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_SIMULATE_SEND, true);

      if (!isSimulateSend) {
        doSendOpenTransactionVerified(openTransactionId, jsonOpenTransactionVerified);
      }
    } else {
      LOGGER.error("[Verified] no SignIn Data found");
    }
  }

  /**
   * 
   */
  private void doSendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull String jsonOpenTransactionVerified) {
    LOGGER.trace("doSendOpenTransactionVerified()");

    try {
      String serverId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.SERVER_ID);
      String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/verified";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());
      httpPut.addHeader("Device-Id", serverId);

      StringEntity entity = new StringEntity(jsonOpenTransactionVerified, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = getHttpClient().execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        LOGGER.error("[Verified] HTTP Status Code: " + statusCode);
        httpClient = null;
      }
    } catch (Exception e) {
      LOGGER.error("sendOpenTransactionVerified", e);
      resetSignIn();
    }
  }

  /**
   * 
   */
  @Override
  public void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) {
    LOGGER.trace("sendOpenTransactionInvalidated()");

    if (null != signInDTO) {
      String jsonOpenTransactionInvalidated = gson.toJson(openTransactionInvalidatedDTO);
      logJSON("invalidated", jsonOpenTransactionInvalidated);

      boolean isSimulateSend = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_SIMULATE_SEND, true);

      if (!isSimulateSend) {
        doSendOpenTransactionInvalidated(openTransactionId, jsonOpenTransactionInvalidated);
      }
    } else {
      LOGGER.error("[Invalidated] no SignIn Data found");
    }
  }

  /**
   * 
   */
  private void doSendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull String jsonOpenTransactionInvalidated) {
    LOGGER.trace("doSendOpenTransactionInvalidated()");

    try {
      String serverId = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.SERVER_ID);
      String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/transaction/" + openTransactionId + "/invalidated";
      LOGGER.debug("URL: " + url);

      HttpPut httpPut = new HttpPut(url);
      httpPut.addHeader("Authorization", "Bearer " + signInDTO.getAccessToken());
      httpPut.addHeader("Device-Id", serverId);

      StringEntity entity = new StringEntity(jsonOpenTransactionInvalidated, ContentType.APPLICATION_JSON);
      httpPut.setEntity(entity);

      HttpResponse httpResponse = getHttpClient().execute(httpPut);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (HttpStatus.SC_OK != statusCode) {
        LOGGER.error("[Invalidated] HTTP Status Code: " + statusCode);
        httpClient = null;
      }
    } catch (Exception e) {
      LOGGER.error("sendOpenTransactionInvalidated", e);
      resetSignIn();
    }
  }

  /**
   * 
   */
  private void logJSON(
      @Nonnull String type,
      @Nonnull String jsonString) {
    LOGGER.trace("logJSON()");

    try {
      String fileName =
          new StringBuilder()
              .append(type)
              .append("-").append(DATE_FORMAT.format(new Date()))
              .append(logCounter)
              .append(".json")
              .toString();

      Path jsonLogFilePath = Path.of("", "logs", "json", network.toString(), type, fileName);

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
  private HttpClient getHttpClient() {
    LOGGER.trace("getHttpClient()");

    if (null == httpClient) {
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
              .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
              .setSocketTimeout(HTTP_CLIENT_TIMEOUT).build();

      httpClient =
          HttpClientBuilder.create()
              .setDefaultRequestConfig(requestConfig)
              .build();
    }

    return httpClient;
  }

  /**
   * 
   */
  private SignInDTO createLoginData() {
    SignInDTO loginData = new SignInDTO();

    loginData.setAddress(ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_ADDRESS));
    loginData.setSignature(ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_SIGNATURE));

    return loginData;
  }

  /**
   * 
   */
  private class AccessTokenDTO {
    private String accessToken;
  }
}
