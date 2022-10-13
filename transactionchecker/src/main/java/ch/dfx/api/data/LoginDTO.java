package ch.dfx.api.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class LoginDTO {
  private String address = null;

  private String signature = null;

  private String accessToken = null;

  /**
   * 
   */
  public LoginDTO() {
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
