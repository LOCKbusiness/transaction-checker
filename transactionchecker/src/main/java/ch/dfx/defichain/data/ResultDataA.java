package ch.dfx.defichain.data;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public abstract class ResultDataA {

  private ResultErrorData error = null;

  /**
   * 
   */
  public ResultDataA() {
  }

  public ResultErrorData getError() {
    return error;
  }

  public void setError(ResultErrorData error) {
    this.error = error;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
