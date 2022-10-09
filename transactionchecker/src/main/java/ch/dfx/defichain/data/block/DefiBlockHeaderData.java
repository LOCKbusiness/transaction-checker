package ch.dfx.defichain.data.block;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DefiBlockHeaderData {
  private String hash = null;
  private Long confirmations = null;
  private Long height = null;
  private String previousblockhash = null;
  private String nextblockhash = null;

  /**
   * 
   */
  public DefiBlockHeaderData() {
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Long getConfirmations() {
    return confirmations;
  }

  public void setConfirmations(Long confirmations) {
    this.confirmations = confirmations;
  }

  public Long getHeight() {
    return height;
  }

  public void setHeight(Long height) {
    this.height = height;
  }

  public String getPreviousblockhash() {
    return previousblockhash;
  }

  public void setPreviousblockhash(String previousblockhash) {
    this.previousblockhash = previousblockhash;
  }

  public String getNextblockhash() {
    return nextblockhash;
  }

  public void setNextblockhash(String nextblockhash) {
    this.nextblockhash = nextblockhash;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
