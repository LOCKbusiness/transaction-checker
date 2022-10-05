package ch.dfx.defichain.provider;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.DefiAccountResultData;
import ch.dfx.defichain.data.DefiAmountData;
import ch.dfx.defichain.data.DefiListAccountHistoryData;
import ch.dfx.defichain.data.DefiListAccountHistoryResultData;
import ch.dfx.defichain.data.ResultDataA;
import ch.dfx.defichain.data.ResultErrorData;
import ch.dfx.defichain.data.block.DefiBlockCountResultData;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.block.DefiBlockHashResultData;
import ch.dfx.defichain.data.block.DefiBlockHeaderData;
import ch.dfx.defichain.data.block.DefiBlockHeaderResultData;
import ch.dfx.defichain.data.block.DefiBlockResultData;
import ch.dfx.defichain.data.transaction.DefiRawTransactionResultData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionResultData;

/**
 * 
 */
public class DefiDataProviderImpl implements DefiDataProvider {
  private static final Logger LOGGER = LogManager.getLogger(DefiDataProviderImpl.class);

  private final HttpClient httpClient;
  private final HttpPost httpPost;

  private final Gson gson;

  /**
   * 
   */
  public DefiDataProviderImpl(
      @Nonnull HttpClient httpClient,
      @Nonnull HttpPost httpPost) {
    this.httpClient = httpClient;
    this.httpPost = httpPost;

    this.gson = new GsonBuilder()
        .registerTypeAdapter(DefiAmountData.class, new JsonDeserializer<DefiAmountData>() {
          @Override
          public DefiAmountData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new DefiAmountData(json.getAsString());
          }
        }).create();
  }

  /**
   * 
   */
  @Override
  public Long getBlockCount() throws DfxException {
    LOGGER.trace("getBlockCount() ...");
    return getData("getblockcount", DefiBlockCountResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String getBlockHash(@Nonnull Long blockCount) throws DfxException {
    LOGGER.trace("getBlockHash(): blockCount=" + blockCount + " ...");

    List<Object> paramList = Arrays.asList(blockCount);

    return getData("getblockhash", paramList, DefiBlockHashResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiBlockHeaderData getBlockHeader(@Nonnull String blockHash) throws DfxException {
    LOGGER.trace("getBlockHeader(): blockHash=" + blockHash + " ...");

    List<Object> paramList = Arrays.asList(blockHash);

    return getData("getblockheader", paramList, DefiBlockHeaderResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiBlockData getBlock(@Nonnull String blockHash) throws DfxException {
    LOGGER.trace("getBlock(): blockHash=" + blockHash + " ...");

    List<Object> paramList = Arrays.asList(blockHash);

    return getData("getblock", paramList, DefiBlockResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<DefiAmountData> getAccount(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAccount(): address=" + address + " ...");

    List<Object> paramList = Arrays.asList(address);

    return getData("getaccount", paramList, DefiAccountResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<DefiListAccountHistoryData> listAccountHistory(
      @Nonnull String wallet,
      @Nonnull String address,
      @Nonnull Long blockHeight,
      @Nonnull Long limit) throws DfxException {
    LOGGER.trace("getAccount(): address=" + address + " ...");

    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("maxBlockHeight", blockHeight);
    paramMap.put("no_rewards", true);
    paramMap.put("limit", limit);

    List<Object> paramList = Arrays.asList(address, paramMap);

    return getData(wallet, "listaccounthistory", paramList, DefiListAccountHistoryResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiTransactionData getTransaction(
      @Nonnull String transactionId,
      @Nonnull String blockHash) throws DfxException {
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " / blockHash=" + blockHash + " ...");

    List<Object> paramList = Arrays.asList(transactionId, true, blockHash);

    return getData("getrawtransaction", paramList, DefiTransactionResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " ...");

    List<Object> paramList = Arrays.asList(transactionId);

    String hexString =
        getData("getrawtransaction", paramList, DefiRawTransactionResultData.class).getResult();

    return decodeRawTransaction(hexString);
  }

  /**
   * 
   */
  @Override
  public DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException {
    LOGGER.trace("decodeRawTransaction() ...");

    List<Object> paramList = Arrays.asList(hexString);

    return getData("decoderawtransaction", paramList, DefiTransactionResultData.class).getResult();
  }

  /**
   * 
   */
  private <T extends ResultDataA> T getData(
      @Nonnull String methodName,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(methodName, new ArrayList<>(), returnType);
  }

  /**
   * 
   */
  private <T extends ResultDataA> T getData(
      @Nonnull String wallet,
      @Nonnull String methodName,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(wallet, methodName, new ArrayList<>(), returnType);
  }

  /**
   * 
   */
  private <T extends ResultDataA> T getData(
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(null, methodName, paramList, Type.class.cast(returnType));
  }

  /**
   * 
   */
  private <T extends ResultDataA> T getData(
      @Nonnull String wallet,
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(wallet, methodName, paramList, Type.class.cast(returnType));
  }

  /**
   * 
   */
  private <T extends ResultDataA> T getData(
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Type returnType) throws DfxException {
    return getData(null, methodName, paramList, returnType);
  }

  /**
   * 
   */
  private synchronized <T extends ResultDataA> T getData(
      @Nullable String wallet,
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Type returnType) throws DfxException {
    try {
      // ...
      setHttpURI(wallet);

      // ...
      StringBuilder methodBuilder = new StringBuilder("{")
          .append("\"method\":\"").append(methodName).append("\"")
          .append(", \"params\":")
          .append(gson.toJson(paramList));

      if (null != wallet) {
        methodBuilder.append(", \"wallet\":\"").append(wallet).append("\"");
      }

      methodBuilder.append("}");

      String method = methodBuilder.toString();
      LOGGER.trace(method);

      StringEntity stringEntity = new StringEntity(method, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);

      // ...
      HttpResponse response = httpClient.execute(httpPost);
      HttpEntity responseEntity = response.getEntity();

      String jsonResponse = EntityUtils.toString(responseEntity);
      LOGGER.trace(trim(jsonResponse));

      T resultData = gson.fromJson(jsonResponse, returnType);
      handleError(resultData);

      return resultData;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      String typeName = returnType.getTypeName();
      typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
      throw new DfxException("get Data (" + typeName + ")", e);
    }
  }

  /**
   * 
   */
  private void setHttpURI(@Nullable String wallet) {
    // ...
    String url = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_URL);

    if (null != wallet) {
      url += "/wallet/" + wallet;
    }

    LOGGER.trace("DFI URL: " + url);
    httpPost.setURI(URI.create(url));
  }

  /**
   * 
   */
  private <T extends ResultDataA> void handleError(
      @Nonnull T resultData) throws DfxException {
    ResultErrorData resultErrorData = resultData.getError();

    if (null != resultErrorData) {
      throw new DfxException("ERROR " + resultErrorData.getCode() + ":\n" + resultErrorData.getMessage());
    }
  }

  /**
   * 
   */
  private String trim(@Nonnull String text) {
    if (200 < text.length()) {
      text = StringUtils.left(text, 200) + " ...";
    }

    return text.trim();
  }
}
