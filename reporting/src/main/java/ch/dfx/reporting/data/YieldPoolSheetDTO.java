package ch.dfx.reporting.data;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class YieldPoolSheetDTO {
  private final String type;
  private final Timestamp timestamp;

  private BigDecimal tokenAAmount = BigDecimal.ZERO;
  private BigDecimal tokenAPrice = BigDecimal.ZERO;

  private BigDecimal tokenBAmount = BigDecimal.ZERO;
  private BigDecimal tokenBPrice = BigDecimal.ZERO;

  private BigDecimal balanceAmount = BigDecimal.ZERO;
  private BigDecimal balancePrice = BigDecimal.ZERO;

  private BigDecimal difference = BigDecimal.ZERO;
  private BigDecimal interval = BigDecimal.ZERO;
  private BigDecimal yield = BigDecimal.ZERO;

  /**
   * 
   */
  public YieldPoolSheetDTO(
      @Nonnull String type,
      @Nonnull Timestamp timestamp) {
    this.type = type;
    this.timestamp = timestamp;
  }

  public String getType() {
    return type;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public BigDecimal getTokenAAmount() {
    return tokenAAmount;
  }

  public void setTokenAAmount(BigDecimal tokenAAmount) {
    this.tokenAAmount = tokenAAmount;
  }

  public BigDecimal getTokenAPrice() {
    return tokenAPrice;
  }

  public void setTokenAPrice(BigDecimal tokenAPrice) {
    this.tokenAPrice = tokenAPrice;
  }

  public BigDecimal getTokenBAmount() {
    return tokenBAmount;
  }

  public void setTokenBAmount(BigDecimal tokenBAmount) {
    this.tokenBAmount = tokenBAmount;
  }

  public BigDecimal getTokenBPrice() {
    return tokenBPrice;
  }

  public void setTokenBPrice(BigDecimal tokenBPrice) {
    this.tokenBPrice = tokenBPrice;
  }

  public BigDecimal getBalanceAmount() {
    return balanceAmount;
  }

  public void setBalanceAmount(BigDecimal balanceAmount) {
    this.balanceAmount = balanceAmount;
  }

  public BigDecimal getBalancePrice() {
    return balancePrice;
  }

  public void setBalancePrice(BigDecimal balancePrice) {
    this.balancePrice = balancePrice;
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public void setDifference(BigDecimal difference) {
    this.difference = difference;
  }

  public BigDecimal getInterval() {
    return interval;
  }

  public void setInterval(BigDecimal interval) {
    this.interval = interval;
  }

  public BigDecimal getYield() {
    return yield;
  }

  public void setYield(BigDecimal yield) {
    this.yield = yield;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
