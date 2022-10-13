package ch.dfx.httpserver.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class HttpTransactionDTO {
  private String fileName = null;

  private String rawTransaction = null;
  private String signature = null;

  private String state = null;

  /**
   * 
   */
  public HttpTransactionDTO() {
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

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
