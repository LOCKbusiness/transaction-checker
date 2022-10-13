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

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.DefiAmountData;
import ch.dfx.defichain.data.ResultDataA;
import ch.dfx.defichain.data.ResultErrorData;
import ch.dfx.defichain.data.basic.DefiBooleanResultData;
import ch.dfx.defichain.data.basic.DefiLongResultData;
import ch.dfx.defichain.data.basic.DefiStringListResultData;
import ch.dfx.defichain.data.basic.DefiStringResultData;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.block.DefiBlockResultData;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.custom.DefiCustomResultData;
import ch.dfx.defichain.data.custom.DefiCustomResultWrapperData;
import ch.dfx.defichain.data.masternode.DefiMasternodeData;
import ch.dfx.defichain.data.masternode.DefiMasternodeResultData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionResultData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletResultData;
import ch.dfx.defichain.provider.typeadapter.AmountTypeAdapter;
import ch.dfx.defichain.provider.typeadapter.CustomTypeAdapter;

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
        .registerTypeAdapter(DefiAmountData.class, new AmountTypeAdapter())
        .registerTypeAdapter(DefiCustomResultWrapperData.class, new CustomTypeAdapter())
        .create();
  }

  /**
   * 
   */
  @Override
  public DefiLoadWalletData loadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("loadWallet(): wallet=" + wallet + " ...");

    List<Object> paramList = Arrays.asList(wallet);

    return getData("loadwallet", paramList, DefiLoadWalletResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String unloadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("unloadWallet(): wallet=" + wallet + " ...");

    List<Object> paramList = Arrays.asList(wallet);

    return getData("unloadwallet", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<String> listWallets() throws DfxException {
    LOGGER.trace("listWallets() ...");

    List<Object> paramList = new ArrayList<>();

    return getData("listwallets", paramList, DefiStringListResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String walletPassphrase(
      @Nonnull String wallet,
      @Nonnull String passphrase,
      int timeInSeconds) throws DfxException {
    LOGGER.trace("walletPassphrase(): wallet=" + wallet + " ...");

    List<Object> paramList = Arrays.asList(passphrase, timeInSeconds);

    return getData(wallet, "walletpassphrase", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String walletLock(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("walletLock(): wallet=" + wallet + " ...");

    List<Object> paramList = new ArrayList<>();

    return getData(wallet, "walletlock", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String signMessage(
      @Nonnull String wallet,
      @Nonnull String address,
      @Nonnull String message) throws DfxException {
    LOGGER.trace("signMessage(): wallet=" + wallet + " ...");

    List<Object> paramList = Arrays.asList(address, message);

    return getData(wallet, "signmessage", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public Boolean verifyMessage(
      @Nonnull String address,
      @Nonnull String signature,
      @Nonnull String message) throws DfxException {
    LOGGER.trace("verifyMessage() ...");

    List<Object> paramList = Arrays.asList(address, signature, message);

    return getData("verifymessage", paramList, DefiBooleanResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public Long getBlockCount() throws DfxException {
    LOGGER.trace("getBlockCount() ...");

    List<Object> paramList = new ArrayList<>();

    return getData("getblockcount", paramList, DefiLongResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String getBlockHash(@Nonnull Long blockCount) throws DfxException {
    LOGGER.trace("getBlockHash(): blockCount=" + blockCount + " ...");

    List<Object> paramList = Arrays.asList(blockCount);

    return getData("getblockhash", paramList, DefiStringResultData.class).getResult();
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
  public DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " ...");

    List<Object> paramList = Arrays.asList(transactionId, true);

    return getData("getrawtransaction", paramList, DefiTransactionResultData.class).getResult();
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
  public DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException {
    LOGGER.trace("decodeRawTransaction() ...");

    List<Object> paramList = Arrays.asList(hexString);

    return getData("decoderawtransaction", paramList, DefiTransactionResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public Boolean isAppliedCustomTransaction(
      @Nonnull String transactionId,
      @Nonnull Long blockCount) throws DfxException {
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " / blockCount=" + blockCount + " ...");

    List<Object> paramList = Arrays.asList(transactionId, blockCount);

    return getData("isappliedcustomtx", paramList, DefiBooleanResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiCustomData decodeCustomTransaction(@Nonnull String hexString) throws DfxException {
    LOGGER.trace("decodeCustomTransaction() ...");

    DefiCustomData customData = null;

    List<Object> paramList = Arrays.asList(hexString);

    Object wrapperObject = getData("decodecustomtx", paramList, DefiCustomResultWrapperData.class);

    if (wrapperObject instanceof DefiCustomResultData) {
      customData = ((DefiCustomResultData) wrapperObject).getResult();
    } else {
      customData = new DefiCustomData();
      customData.setMessage(((DefiStringResultData) wrapperObject).getResult());
    }

    return customData;
  }

  /**
   * 
   */
  @Override
  public Map<String, DefiMasternodeData> getMasternode(
      @Nonnull String wallet,
      @Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getMasternode(): transactionId=" + transactionId + " ...");

    Map<String, DefiMasternodeData> masternodeMap = null;

    List<Object> paramList = Arrays.asList(transactionId);

    DefiMasternodeResultData masternodeResultData =
        getDataWithErrorExpected(wallet, "getmasternode", paramList, DefiMasternodeResultData.class);

    ResultErrorData errorData = masternodeResultData.getError();

    if (null != errorData) {
      masternodeMap = new HashMap<>();
      DefiMasternodeData masternodeData = new DefiMasternodeData();
      masternodeData.setMessage(errorData.getMessage());

      masternodeMap.put("message", masternodeData);
    } else {
      masternodeMap = masternodeResultData.getResult();
    }

    return masternodeMap;
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
  private <T extends ResultDataA> T getDataWithErrorExpected(
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(null, methodName, paramList, Type.class.cast(returnType), true);
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
  private <T extends ResultDataA> T getDataWithErrorExpected(
      @Nonnull String wallet,
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Class<T> returnType) throws DfxException {
    return getData(wallet, methodName, paramList, Type.class.cast(returnType), true);
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
    return getData(wallet, methodName, paramList, returnType, null);
  }

  /**
   * 
   */
  private synchronized <T extends ResultDataA> T getData(
      @Nullable String wallet,
      @Nonnull String methodName,
      @Nonnull List<Object> paramList,
      @Nonnull Type returnType,
      @Nullable Boolean errorExpected) throws DfxException {
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

      if (null == errorExpected
          || !errorExpected.booleanValue()) {
        handleError(resultData);
      }

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