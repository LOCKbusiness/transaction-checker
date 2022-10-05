package ch.dfx.lockbusiness.stakingbalances.defichain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVinData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DefichainBalanceHandler {
  private static final Logger LOGGER = LogManager.getLogger(DefichainBalanceHandler.class);

  // ...
  private final List<String> checkList;

  // ...
  private final DefiDataProvider dataProvider;

  // ...
  private String address = null;
  private BigDecimal completeVinBalance = BigDecimal.ZERO;
  private BigDecimal completeVoutBalance = BigDecimal.ZERO;

  /**
   * 
   */
  public DefichainBalanceHandler(@Nonnull DefiDataProvider dataProvider) {
    Objects.requireNonNull(dataProvider, "dataProvider must not be null");

    this.dataProvider = dataProvider;

    this.checkList = new ArrayList<>();
  }

  public BigDecimal getCompleteVinBalance() {
    return completeVinBalance;
  }

  public BigDecimal getCompleteVoutBalance() {
    return completeVoutBalance;
  }

  /**
   * 
   */
  public void setup(String address) {
    this.address = address;
    checkList.clear();
  }

  public List<String> getCheckList() {
    return checkList;
  }

  /**
   * 
   */
  public List<DefiTransactionVinData> calculateBalance(@Nonnull List<String> txidList) throws DfxException {
    LOGGER.trace("calculateBalance() ...");

    BigDecimal vinBalance = BigDecimal.ZERO;
    BigDecimal voutBalance = BigDecimal.ZERO;

    List<DefiTransactionVinData> transactionVinDataList = new ArrayList<>();

    for (String txid : txidList) {
      LOGGER.trace("txid: " + txid);

      DefiTransactionData transactionData = dataProvider.getTransaction(txid);

      // vin ...
      for (DefiTransactionVinData transactionVinData : transactionData.getVin()) {
        BigDecimal vinValue = getVin1(transactionVinData, transactionVinDataList);

        if (null != vinValue) {
          vinBalance = vinBalance.add(vinValue);
        }
      }

      // vout ...
      for (DefiTransactionVoutData transactionVoutData : transactionData.getVout()) {
        if (isMyAddress(transactionVoutData)) {
          BigDecimal voutValue = transactionVoutData.getValue();
          checkList.add(txid + ";" + voutValue);
          voutBalance = voutBalance.add(voutValue);
        }
      }
    }

    completeVinBalance = completeVinBalance.add(vinBalance);
    completeVoutBalance = completeVoutBalance.add(voutBalance);

    return transactionVinDataList;
  }

  /**
   * 
   */
  private @Nullable BigDecimal getVin1(
      @Nonnull DefiTransactionVinData transactionVinData,
      @Nonnull List<DefiTransactionVinData> transactionVinDataList) throws DfxException {
    BigDecimal vinValue = null;

    String vinTxid = transactionVinData.getTxid();
    Long vinVout = transactionVinData.getVout();

    DefiTransactionData transactionData = dataProvider.getTransaction(vinTxid);

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();
    DefiTransactionVoutData transactionVoutData = transactionVoutDataList.get(vinVout.intValue());

    if (isMyAddress(transactionVoutData)) {
      vinValue = transactionVoutData.getValue();
    }

    if (null != vinValue) {
      transactionVinDataList.add(transactionVinData);
      checkList.add(vinTxid + ";" + vinValue);
    } else {
      vinValue = getVin2(transactionData, transactionVinDataList);
    }

    return vinValue;
  }

  /**
   * 
   */
  private @Nullable BigDecimal getVin2(
      @Nonnull DefiTransactionData transactionData,
      @Nonnull List<DefiTransactionVinData> transactionVinDataList) throws DfxException {
    BigDecimal vinValue = null;

    for (DefiTransactionVinData transactionVinData : transactionData.getVin()) {
      vinValue = getVin2(transactionVinData, transactionVinDataList);

      if (null != vinValue) {
        break;
      }
    }

    return vinValue;
  }

  /**
   * 
   */
  private @Nullable BigDecimal getVin2(
      @Nonnull DefiTransactionVinData transactionVinData,
      @Nonnull List<DefiTransactionVinData> transactionVinDataList) throws DfxException {
    BigDecimal vinValue = null;

    String vinTxid = transactionVinData.getTxid();
    Long vinVout = transactionVinData.getVout();

    DefiTransactionData transactionData = dataProvider.getTransaction(vinTxid);

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();
    DefiTransactionVoutData transactionVoutData = transactionVoutDataList.get(vinVout.intValue());

    if (isMyAddress(transactionVoutData)) {
      vinValue = transactionVoutData.getValue();
    }

    if (null != vinValue) {
      transactionVinDataList.add(transactionVinData);
      checkList.add(vinTxid + ";" + vinValue);
    }

    return vinValue;
  }

  /**
   * 
   */
  private boolean isMyAddress(@Nonnull DefiTransactionVoutData transactionVoutData) {
    boolean isMyAddress = false;

    DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

    if (null != transactionScriptPubKeyData) {
      List<String> addressesList = transactionScriptPubKeyData.getAddresses();

      isMyAddress =
          (null != addressesList
              && addressesList.contains(address));
    }

    return isMyAddress;
  }
}
