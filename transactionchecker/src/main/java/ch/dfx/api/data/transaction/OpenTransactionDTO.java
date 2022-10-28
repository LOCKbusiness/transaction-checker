package ch.dfx.api.data.transaction;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionDTO {
  private OpenTransactionTypeEnum type = null;

  private String id = null;
  private String issuerSignature = null;

  private OpenTransactionRawTxDTO rawTx = null;
  private OpenTransactionPayloadDTO payload = null;

  private String invalidatedReason = null;

  /**
   * 
   */
  public OpenTransactionDTO() {
    this.type = OpenTransactionTypeEnum.UNKNOWN;
  }

  public OpenTransactionTypeEnum getType() {
    return type;
  }

  public void setType(@Nonnull OpenTransactionTypeEnum type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
