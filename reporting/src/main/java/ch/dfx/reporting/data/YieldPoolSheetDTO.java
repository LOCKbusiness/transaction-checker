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

  private BigDecimal hourDifference = BigDecimal.ZERO;
  private BigDecimal hourInterval = BigDecimal.ZERO;
  private BigDecimal hourYield = BigDecimal.ZERO;

  private BigDecimal dayDifference = BigDecimal.ZERO;
  private BigDecimal dayInterval = BigDecimal.ZERO;
  private BigDecimal dayYield = BigDecimal.ZERO;

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

  public BigDecimal getHourDifference() {
    return hourDifference;
  }

  public void setHourDifference(BigDecimal hourDifference) {
    this.hourDifference = hourDifference;
  }

  public BigDecimal getHourInterval() {
    return hourInterval;
  }

  public void setHourInterval(BigDecimal hourInterval) {
    this.hourInterval = hourInterval;
  }

  public BigDecimal getHourYield() {
    return hourYield;
  }

  public void setHourYield(BigDecimal hourYield) {
    this.hourYield = hourYield;
  }

  public BigDecimal getDayDifference() {
    return dayDifference;
  }

  public void setDayDifference(BigDecimal dayDifference) {
    this.dayDifference = dayDifference;
  }

  public BigDecimal getDayInterval() {
    return dayInterval;
  }

  public void setDayInterval(BigDecimal dayInterval) {
    this.dayInterval = dayInterval;
  }

  public BigDecimal getDayYield() {
    return dayYield;
  }

  public void setDayYield(BigDecimal dayYield) {
    this.dayYield = dayYield;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
