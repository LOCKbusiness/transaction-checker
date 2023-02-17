package ch.dfx.manager.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class SignedMessageCheckDTO {
  private Integer id = null;

  private String message = null;
  private String address = null;
  private String signature = null;

  private boolean isValid = false;

  /**
   * 
   */
  public SignedMessageCheckDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
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

  public boolean isValid() {
    return isValid;
  }

  public void setValid(boolean isValid) {
    this.isValid = isValid;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
