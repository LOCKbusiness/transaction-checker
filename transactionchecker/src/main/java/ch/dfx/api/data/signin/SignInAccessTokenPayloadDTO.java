package ch.dfx.api.data.signin;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class SignInAccessTokenPayloadDTO {
  private Integer walletId = null;
  private Integer userId = null;

  private String address = null;
  private String blockchain = null;
  private String role = null;

  private Long iat = null;
  private Long exp = null;

  /**
   * 
   */
  public SignInAccessTokenPayloadDTO() {
  }

  public Integer getWalletId() {
    return walletId;
  }

  public void setWalletId(Integer walletId) {
    this.walletId = walletId;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getBlockchain() {
    return blockchain;
  }

  public void setBlockchain(String blockchain) {
    this.blockchain = blockchain;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Long getIat() {
    return iat;
  }

  public void setIat(Long iat) {
    this.iat = iat;
  }

  public Long getExp() {
    return exp;
  }

  public void setExp(Long exp) {
    this.exp = exp;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
