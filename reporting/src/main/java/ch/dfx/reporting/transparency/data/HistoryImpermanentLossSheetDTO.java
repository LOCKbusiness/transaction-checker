package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;

/**
 * 
 */
public class HistoryImpermanentLossSheetDTO extends HistorySheetDTO {
  private BigDecimal poolTokenAAmount = BigDecimal.ZERO;
  private BigDecimal poolTokenBAmount = BigDecimal.ZERO;
  private BigDecimal poolTokenAmount = BigDecimal.ZERO;

  private BigDecimal poolTotalLiquidity = BigDecimal.ZERO;
  private BigDecimal poolTokenAReserve = BigDecimal.ZERO;
  private BigDecimal poolTokenBReserve = BigDecimal.ZERO;

  private BigDecimal poolTokenRatio = BigDecimal.ZERO;

  private BigDecimal calcTokenAAmount = BigDecimal.ZERO;
  private BigDecimal calcTokenBAmount = BigDecimal.ZERO;

  private BigDecimal tokenABalance = BigDecimal.ZERO;
  private BigDecimal tokenBBalance = BigDecimal.ZERO;

  private BigDecimal tokenBInA = BigDecimal.ZERO;
  private BigDecimal total = BigDecimal.ZERO;
  private BigDecimal tokenAPrice = BigDecimal.ZERO;

  private BigDecimal impermanentLoss = BigDecimal.ZERO;

  /**
   * 
   */
  public HistoryImpermanentLossSheetDTO() {
  }

  public BigDecimal getPoolTokenAAmount() {
    return poolTokenAAmount;
  }

  public void setPoolTokenAAmount(BigDecimal poolTokenAAmount) {
    this.poolTokenAAmount = poolTokenAAmount;
  }

  public BigDecimal getPoolTokenBAmount() {
    return poolTokenBAmount;
  }

  public void setPoolTokenBAmount(BigDecimal poolTokenBAmount) {
    this.poolTokenBAmount = poolTokenBAmount;
  }

  public BigDecimal getPoolTokenAmount() {
    return poolTokenAmount;
  }

  public void setPoolTokenAmount(BigDecimal poolTokenAmount) {
    this.poolTokenAmount = poolTokenAmount;
  }

  public BigDecimal getPoolTotalLiquidity() {
    return poolTotalLiquidity;
  }

  public void setPoolTotalLiquidity(BigDecimal poolTotalLiquidity) {
    this.poolTotalLiquidity = poolTotalLiquidity;
  }

  public BigDecimal getPoolTokenAReserve() {
    return poolTokenAReserve;
  }

  public void setPoolTokenAReserve(BigDecimal poolTokenAReserve) {
    this.poolTokenAReserve = poolTokenAReserve;
  }

  public BigDecimal getPoolTokenBReserve() {
    return poolTokenBReserve;
  }

  public void setPoolTokenBReserve(BigDecimal poolTokenBReserve) {
    this.poolTokenBReserve = poolTokenBReserve;
  }

  public BigDecimal getPoolTokenRatio() {
    return poolTokenRatio;
  }

  public void setPoolTokenRatio(BigDecimal poolTokenRatio) {
    this.poolTokenRatio = poolTokenRatio;
  }

  public BigDecimal getCalcTokenAAmount() {
    return calcTokenAAmount;
  }

  public void setCalcTokenAAmount(BigDecimal calcTokenAAmount) {
    this.calcTokenAAmount = calcTokenAAmount;
  }

  public BigDecimal getCalcTokenBAmount() {
    return calcTokenBAmount;
  }

  public void setCalcTokenBAmount(BigDecimal calcTokenBAmount) {
    this.calcTokenBAmount = calcTokenBAmount;
  }

  public BigDecimal getTokenABalance() {
    return tokenABalance;
  }

  public void setTokenABalance(BigDecimal tokenABalance) {
    this.tokenABalance = tokenABalance;
  }

  public BigDecimal getTokenBBalance() {
    return tokenBBalance;
  }

  public void setTokenBBalance(BigDecimal tokenBBalance) {
    this.tokenBBalance = tokenBBalance;
  }

  public BigDecimal getTokenBInA() {
    return tokenBInA;
  }

  public void setTokenBInA(BigDecimal tokenBInA) {
    this.tokenBInA = tokenBInA;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public BigDecimal getTokenAPrice() {
    return tokenAPrice;
  }

  public void setTokenAPrice(BigDecimal tokenAPrice) {
    this.tokenAPrice = tokenAPrice;
  }

  public BigDecimal getImpermanentLoss() {
    return impermanentLoss;
  }

  public void setImpermanentLoss(BigDecimal impermanentLoss) {
    this.impermanentLoss = impermanentLoss;
  }
}
