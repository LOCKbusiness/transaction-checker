package ch.dfx.reporting.compare.data;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class DBTransactionDTO {
  private final TokenEnum token;

  private Timestamp inTimestamp = null;
  private String inTxId = null;

  private Timestamp outTimestamp = null;
  private String outTxId = null;

  private BigDecimal amount = null;

  /**
   * 
   */
  public DBTransactionDTO(@Nonnull TokenEnum token) {
    this.token = token;
  }

  public TokenEnum getToken() {
    return token;
  }

  public Timestamp getInTimestamp() {
    return inTimestamp;
  }

  public void setInTimestamp(Timestamp inTimestamp) {
    this.inTimestamp = inTimestamp;
  }

  public String getInTxId() {
    return inTxId;
  }

  public void setInTxId(String inTxId) {
    this.inTxId = inTxId;
  }

  public Timestamp getOutTimestamp() {
    return outTimestamp;
  }

  public void setOutTimestamp(Timestamp outTimestamp) {
    this.outTimestamp = outTimestamp;
  }

  public String getOutTxId() {
    return outTxId;
  }

  public void setOutTxId(String outTxId) {
    this.outTxId = outTxId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
