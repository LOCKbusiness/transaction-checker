package ch.dfx.ocean.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryPoolDTO {
  private String timestamp = "";
  private String txId = "";
  private String type = "";

  private BigDecimal poolTokenA = BigDecimal.ZERO;
  private BigDecimal poolTokenB = BigDecimal.ZERO;
  private BigDecimal poolToken = BigDecimal.ZERO;

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getTxId() {
    return txId;
  }

  public void setTxId(String txId) {
    this.txId = txId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getPoolTokenA() {
    return poolTokenA;
  }

  public void setPoolTokenA(BigDecimal poolTokenA) {
    this.poolTokenA = poolTokenA;
  }

  public void addPoolTokenA(BigDecimal poolTokenA) {
    this.poolTokenA = this.poolTokenA.add(poolTokenA);
  }

  public BigDecimal getPoolTokenB() {
    return poolTokenB;
  }

  public void setPoolTokenB(BigDecimal poolTokenB) {
    this.poolTokenB = poolTokenB;
  }

  public void addPoolTokenB(BigDecimal poolTokenB) {
    this.poolTokenB = this.poolTokenB.add(poolTokenB);
  }

  public BigDecimal getPoolToken() {
    return poolToken;
  }

  public void setPoolToken(BigDecimal poolToken) {
    this.poolToken = poolToken;
  }

  public void addPoolToken(BigDecimal poolToken) {
    this.poolToken = this.poolToken.add(poolToken);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
