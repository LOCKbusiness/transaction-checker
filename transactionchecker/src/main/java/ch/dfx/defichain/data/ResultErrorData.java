package ch.dfx.defichain.data;

import ch.dfx.common.PayoutManagerUtils;

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
    return PayoutManagerUtils.toJson(this);
  }
}
