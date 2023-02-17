package ch.dfx.defichain.data.block;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBlockData extends ResultDataA {
  private String hash = null;
  private Long confirmations = null;
  private Long height = null;
  private Long nTx = null;
  private Long mediantime = null;
  private String previousblockhash = null;
  private String nextblockhash = null;

  private List<String> tx = null;

  /**
   * 
   */
  public DefiBlockData() {
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

  public Long getnTx() {
    return nTx;
  }

  public void setnTx(Long nTx) {
    this.nTx = nTx;
  }

  public Long getMediantime() {
    return mediantime;
  }

  public void setMediantime(Long mediantime) {
    this.mediantime = mediantime;
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

  public List<String> getTx() {
    return tx;
  }

  public void setTx(List<String> tx) {
    this.tx = tx;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
