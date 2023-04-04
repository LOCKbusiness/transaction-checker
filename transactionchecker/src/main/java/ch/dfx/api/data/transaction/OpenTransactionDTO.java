package ch.dfx.api.data.transaction;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;

/**
 * 
 */
public class OpenTransactionDTO {
  private ApiTransactionTypeEnum type = null;

  private String id = null;
  private String assetType = null;
  private String issuerSignature = null;

  private OpenTransactionRawTxDTO rawTx = null;
  private OpenTransactionPayloadDTO payload = null;

  private DefiTransactionData transactionData = null;
  private DefiCustomData transactionCustomData = null;

  private String invalidatedReason = null;

  /**
   * 
   */
  public OpenTransactionDTO() {
    this.type = ApiTransactionTypeEnum.UNKNOWN;
  }

  public @Nonnull ApiTransactionTypeEnum getType() {
    return type;
  }

  public void setType(@Nonnull ApiTransactionTypeEnum type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAssetType() {
    return assetType;
  }

  public void setAssetType(String assetType) {
    this.assetType = assetType;
  }

  public String getIssuerSignature() {
    return issuerSignature;
  }

  public void setIssuerSignature(String issuerSignature) {
    this.issuerSignature = issuerSignature;
  }

  public OpenTransactionRawTxDTO getRawTx() {
    return rawTx;
  }

  public void setRawTx(OpenTransactionRawTxDTO rawTx) {
    this.rawTx = rawTx;
  }

  public OpenTransactionPayloadDTO getPayload() {
    return payload;
  }

  public void setPayload(OpenTransactionPayloadDTO payload) {
    this.payload = payload;
  }

  public DefiTransactionData getTransactionData() {
    return transactionData;
  }

  public void setTransactionData(DefiTransactionData transactionData) {
    this.transactionData = transactionData;
  }

  public DefiCustomData getTransactionCustomData() {
    return transactionCustomData;
  }

  public void setTransactionCustomData(DefiCustomData transactionCustomData) {
    this.transactionCustomData = transactionCustomData;
  }

  public String getInvalidatedReason() {
    return invalidatedReason;
  }

  public void setInvalidatedReason(String invalidatedReason) {
    this.invalidatedReason = invalidatedReason;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
