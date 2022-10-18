package ch.dfx.api.data.signin;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class SignInAccessTokenHeaderDTO {
  private String alg = null;
  private String typ = null;

  /**
   * 
   */
  public SignInAccessTokenHeaderDTO() {
  }

  public String getAlg() {
    return alg;
  }

  public void setAlg(String alg) {
    this.alg = alg;
  }

  public String getTyp() {
    return typ;
  }

  public void setTyp(String typ) {
    this.typ = typ;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
