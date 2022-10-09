package ch.dfx.lockbusiness.stakingbalances.api;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class ApiLoginData {
  private String address = null;

  private String signature = null;

  private String accessToken = null;

  /**
   * 
   */
  public ApiLoginData() {
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
