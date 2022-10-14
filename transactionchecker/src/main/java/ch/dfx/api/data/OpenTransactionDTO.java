package ch.dfx.api.data;

import ch.dfx.api.enumeration.OpenTransactionStateEnum;
import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionDTO {
  private String id = null;

  private OpenTransactionRawTxDTO rawTx = null;
  private String issuerSignature = null;

  // ...
  private String fileName = null;
  private OpenTransactionStateEnum state = null;

  /**
   * 
   */
  public OpenTransactionDTO() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OpenTransactionRawTxDTO getRawTx() {
    return rawTx;
  }

  public void setRawTx(OpenTransactionRawTxDTO rawTx) {
    this.rawTx = rawTx;
  }

  public String getIssuerSignature() {
    return issuerSignature;
  }

  public void setIssuerSignature(String issuerSignature) {
    this.issuerSignature = issuerSignature;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public OpenTransactionStateEnum getState() {
    return state;
  }

  public void setState(OpenTransactionStateEnum state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
