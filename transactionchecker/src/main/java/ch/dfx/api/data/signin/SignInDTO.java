package ch.dfx.api.data.signin;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class SignInDTO {
  private String address = null;
  private String signature = null;

  private String accessToken = null;
  private SignInAccessTokenHeaderDTO accessTokenHeader = null;
  private SignInAccessTokenPayloadDTO accessTokenPayload = null;

  /**
   * 
   */
  public SignInDTO() {
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

  public SignInAccessTokenHeaderDTO getAccessTokenHeader() {
    return accessTokenHeader;
  }

  public void setAccessTokenHeader(SignInAccessTokenHeaderDTO accessTokenHeader) {
    this.accessTokenHeader = accessTokenHeader;
  }

  public SignInAccessTokenPayloadDTO getAccessTokenPayload() {
    return accessTokenPayload;
  }

  public void setAccessTokenPayload(SignInAccessTokenPayloadDTO accessTokenPayload) {
    this.accessTokenPayload = accessTokenPayload;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
