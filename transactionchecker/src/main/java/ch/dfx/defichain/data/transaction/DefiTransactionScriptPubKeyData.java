package ch.dfx.defichain.data.transaction;

import java.util.List;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DefiTransactionScriptPubKeyData {
  private String hex = null;
  private String type = null;

  private List<String> addresses = null;

  /**
   * 
   */
  public DefiTransactionScriptPubKeyData() {
  }

  public String getHex() {
    return hex;
  }

  public void setHex(String hex) {
    this.hex = hex;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<String> addresses) {
    this.addresses = addresses;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
