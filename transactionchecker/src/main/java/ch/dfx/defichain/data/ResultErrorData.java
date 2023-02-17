package ch.dfx.defichain.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class ResultErrorData {
  private Integer code = null;

  private String message = null;

  /**
   * 
   */
  public ResultErrorData() {
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
