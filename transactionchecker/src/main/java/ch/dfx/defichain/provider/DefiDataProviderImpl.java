package ch.dfx.defichain.provider;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Hex;
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

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.ResultDataA;
import ch.dfx.defichain.data.ResultErrorData;
import ch.dfx.defichain.data.account.DefiAccountResultData;
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
import ch.dfx.defichain.data.network.PeerInfoData;
import ch.dfx.defichain.data.network.PeerInfoResultData;
import ch.dfx.defichain.data.pool.DefiPoolPairData;
import ch.dfx.defichain.data.pool.DefiPoolPairResultData;
import ch.dfx.defichain.data.price.DefiFixedIntervalPriceData;
import ch.dfx.defichain.data.price.DefiFixedIntervalPriceResultData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionResultData;
import ch.dfx.defichain.data.vault.DefiListVaultData;
import ch.dfx.defichain.data.vault.DefiListVaultResultData;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.data.vault.DefiVaultResultData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletResultData;
import ch.dfx.defichain.provider.typeadapter.CustomTypeAdapter;

/**
 * 
 */
public class DefiDataProviderImpl implements DefiDataProvider {
  private static final Logger LOGGER = LogManager.getLogger(DefiDataProviderImpl.class);

  // OP_RETURN ...
  private static final byte OP_RETURN = 0x6a;
  private static final byte OP_PUSHDATA1 = 0x4c;
  private static final byte OP_PUSHDATA2 = 0x4d;
  private static final byte OP_PUSHDATA4 = 0x4e;

  // DfTx: 44665478 ...
  private static final byte[] DFTX_BYTES = {
      0x44, 0x66, 0x54, 0x78
  };

  // ...
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
        .registerTypeAdapter(DefiCustomResultWrapperData.class, new CustomTypeAdapter())
        .create();
  }

  /**
   * 
   */
  @Override
  public Long getConnectionCount() throws DfxException {
    LOGGER.trace("getConnectionCount()");

    List<Object> paramList = new ArrayList<>();

    return getDataWithErrorExpected("getconnectioncount", paramList, DefiLongResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<PeerInfoData> getPeerInfo() throws DfxException {
    LOGGER.trace("getPeerInfo()");

    List<Object> paramList = new ArrayList<>();

    return getDataWithErrorExpected("getpeerinfo", paramList, PeerInfoResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiLoadWalletData loadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("loadWallet(): wallet=" + wallet);

    List<Object> paramList = Arrays.asList(wallet);

    return getData("loadwallet", paramList, DefiLoadWalletResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String unloadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("unloadWallet(): wallet=" + wallet);

    List<Object> paramList = Arrays.asList(wallet);

    return getData("unloadwallet", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<String> listWallets() throws DfxException {
    LOGGER.trace("listWallets()");

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
    LOGGER.trace("walletPassphrase(): wallet=" + wallet);

    List<Object> paramList = Arrays.asList(passphrase, timeInSeconds);

    return getData(wallet, "walletpassphrase", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String walletLock(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("walletLock(): wallet=" + wallet);

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
    LOGGER.trace("signMessage(): wallet=" + wallet);

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
    LOGGER.trace("verifyMessage()");

    List<Object> paramList = Arrays.asList(address, signature, message);

    return getData("verifymessage", paramList, DefiBooleanResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public Long getBlockCount() throws DfxException {
    LOGGER.trace("getBlockCount()");

    List<Object> paramList = new ArrayList<>();

    return getData("getblockcount", paramList, DefiLongResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public String getBlockHash(@Nonnull Long blockCount) throws DfxException {
    LOGGER.trace("getBlockHash(): blockCount=" + blockCount);

    List<Object> paramList = Arrays.asList(blockCount);

    return getData("getblockhash", paramList, DefiStringResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiBlockData getBlock(@Nonnull String blockHash) throws DfxException {
    LOGGER.trace("getBlock(): blockHash=" + blockHash);

    List<Object> paramList = Arrays.asList(blockHash);

    return getData("getblock", paramList, DefiBlockResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getTransaction(): transactionId=" + transactionId);

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
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " / blockHash=" + blockHash);

    List<Object> paramList = Arrays.asList(transactionId, true, blockHash);

    return getData("getrawtransaction", paramList, DefiTransactionResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException {
    LOGGER.trace("decodeRawTransaction()");

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
    LOGGER.trace("getTransaction(): transactionId=" + transactionId + " / blockCount=" + blockCount);

    List<Object> paramList = Arrays.asList(transactionId, blockCount);

    return getData("isappliedcustomtx", paramList, DefiBooleanResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public DefiCustomData decodeCustomTransaction(
      @Nonnull String hexString) throws DfxException {
    LOGGER.trace("decodeCustomTransaction()");

    DefiCustomData customData = null;

    List<Object> paramList = Arrays.asList(hexString, true);
    Object wrapperObject = getData("decodecustomtx", paramList, DefiCustomResultWrapperData.class);

    if (wrapperObject instanceof DefiStringResultData) {
      paramList = Arrays.asList(hexString);
      wrapperObject = getData("decodecustomtx", paramList, DefiCustomResultWrapperData.class);
    }

    if (wrapperObject instanceof DefiCustomResultData) {
      customData = ((DefiCustomResultData) wrapperObject).getResult();
    } else {
      customData = new DefiCustomData();
      customData.setMessage(((DefiStringResultData) wrapperObject).getResult());
    }

    return customData;
  }

  /**
   * OP_CODE | HEX | DESCRIPTION
   * -------------|-------------|----------------------------------------------------------------------
   * N/A | 0x01-0x4b | The next opcode bytes is data to be pushed onto the stack
   * OP_PUSHDATA1 | 0x4c | The next byte contains the number of bytes to be pushed onto the stack.
   * OP_PUSHDATA2 | 0x4d | The next 2 bytes contain the number of bytes to be pushed onto the stack in LE order.
   * OP_PUSHDATA4 | 0x4e | The next 4 bytes contain the number of bytes to be pushed onto the stack in LE order.
   */
  @Override
  public byte getCustomType(@Nonnull String scriptPubKeyHexString) throws DfxException {
    LOGGER.trace("getCustomType()");

    try {
      byte customType = 0x00;

      byte[] byteArray = Hex.decodeHex(scriptPubKeyHexString);
      int offset = 0;

      if (byteArray[offset++] == OP_RETURN) {
        byte opPushData = byteArray[offset++];

        if (opPushData == OP_PUSHDATA1) {
          offset += 1;
        } else if (opPushData == OP_PUSHDATA2) {
          offset += 2;
        } else if (opPushData == OP_PUSHDATA4) {
          offset += 4;
        }

        // DfTx ...
        if (byteArray[offset++] == DFTX_BYTES[0]
            && byteArray[offset++] == DFTX_BYTES[1]
            && byteArray[offset++] == DFTX_BYTES[2]
            && byteArray[offset++] == DFTX_BYTES[3]) {
          customType = byteArray[offset];
        }
      }

      return customType;
    } catch (Exception e) {
      throw new DfxException("getCustomType", e);
    }
  }

  /**
   * 
   */
  @Override
  public Map<String, DefiMasternodeData> getMasternode(
      @Nonnull String wallet,
      @Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getMasternode(): transactionId=" + transactionId);

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
  @Override
  public DefiPoolPairData getPoolPair(@Nonnull String poolId) throws DfxException {
    LOGGER.trace("getPoolPair(): poolId=" + poolId);

    List<Object> paramList = Arrays.asList(poolId);

    Map<String, DefiPoolPairData> poolIdToPoolPairDataMap =
        getData("getpoolpair", paramList, DefiPoolPairResultData.class).getResult();

    Entry<String, DefiPoolPairData> poolIdToPoolPairDataMapEntry =
        poolIdToPoolPairDataMap.entrySet().stream().findFirst().get();

    return poolIdToPoolPairDataMapEntry.getValue();
  }

  /**
   * 
   */
  @Override
  public DefiFixedIntervalPriceData getFixedIntervalPrice(@Nonnull String fixedIntervalPriceId) throws DfxException {
    LOGGER.trace("getFixedIntervalPrice(): fixedIntervalPriceId=" + fixedIntervalPriceId);

    List<Object> paramList = Arrays.asList(fixedIntervalPriceId);

    return getData("getfixedintervalprice", paramList, DefiFixedIntervalPriceResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public Map<String, BigDecimal> getActivePriceMap(@Nonnull Set<String> tokenSet) throws DfxException {
    Map<String, BigDecimal> activePriceMap = new HashMap<>();

    for (String token : tokenSet) {
      DefiFixedIntervalPriceData fixedIntervalPriceData = getFixedIntervalPrice(token + "/USD");
      activePriceMap.put(token, fixedIntervalPriceData.getActivePrice());
    }

    return activePriceMap;
  }

  /**
   * 
   */
  @Override
  public List<String> getAccount(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAccount(): address=" + address);

    List<Object> paramList = Arrays.asList(address);

    return getData("getaccount", paramList, DefiAccountResultData.class).getResult();
  }

  /**
   * 
   */
  @Override
  public List<DefiListVaultData> listVaults(@Nonnull String ownerAddress) throws DfxException {
    LOGGER.trace("listVaults(): ownerAddress=" + ownerAddress);

    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("ownerAddress", ownerAddress);
    paramMap.put("verbose", true);

    List<Object> paramList = Arrays.asList(paramMap);

    return getData("listvaults", paramList, DefiListVaultResultData.class).getResult();
  }

  /**
   *
   */
  @Override
  public DefiVaultData getVault(@Nonnull String vaultId) throws DfxException {
    LOGGER.trace("getVault(): vaultId=" + vaultId);

    List<Object> paramList = Arrays.asList(vaultId);

    return getData("getvault", paramList, DefiVaultResultData.class).getResult();
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
    String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_URL);

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
      text = StringUtils.left(text, 200);
    }

    return text.trim();
  }
}
