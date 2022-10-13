package ch.dfx.api.data;

import ch.dfx.api.enumeration.OpenTransactionStateEnum;
import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionDTO {
  private String fileName = null;

  private String rawTransaction = null;
  private String signature = null;

  private OpenTransactionStateEnum state = null;

  /**
   * 
   */
  public OpenTransactionDTO() {
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getRawTransaction() {
    return rawTransaction;
  }

  public void setRawTransaction(String rawTransaction) {
    this.rawTransaction = rawTransaction;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
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
