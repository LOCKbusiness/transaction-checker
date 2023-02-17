package ch.dfx.defichain.data.custom;

import java.util.Map;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DefiCustomData {
  private String txid = null;
  private String type = null;
  private Boolean valid = null;

  private Map<String, Object> results = null;

  private String message = null;

  /**
   * 
   */
  public DefiCustomData() {
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getValid() {
    return valid;
  }

  public void setValid(Boolean valid) {
    this.valid = valid;
  }

  public Map<String, Object> getResults() {
    return results;
  }

  public void setResults(Map<String, Object> results) {
    this.results = results;
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
