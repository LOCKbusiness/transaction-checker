package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionSheetDTO {
  private BigDecimal difference = BigDecimal.ZERO;

  private BigDecimal totalLiquidityPoolToken = BigDecimal.ZERO;
  private BigDecimal totalPoolTokenA = BigDecimal.ZERO;
  private BigDecimal totalPoolTokenB = BigDecimal.ZERO;
  private BigDecimal poolTokenRatio = BigDecimal.ZERO;

  private BigDecimal swapFee = BigDecimal.ZERO;
  private BigDecimal dexFee = BigDecimal.ZERO;

  private BigDecimal input = BigDecimal.ZERO;
  private BigDecimal output = BigDecimal.ZERO;

  /**
   * 
   */
  public TransactionSheetDTO() {
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public void setDifference(BigDecimal difference) {
    this.difference = difference;
  }

  public BigDecimal getTotalLiquidityPoolToken() {
    return totalLiquidityPoolToken;
  }

  public void setTotalLiquidityPoolToken(BigDecimal totalLiquidityPoolToken) {
    this.totalLiquidityPoolToken = totalLiquidityPoolToken;
  }

  public BigDecimal getTotalPoolTokenA() {
    return totalPoolTokenA;
  }

  public void setTotalPoolTokenA(BigDecimal totalPoolTokenA) {
    this.totalPoolTokenA = totalPoolTokenA;
  }

  public BigDecimal getTotalPoolTokenB() {
    return totalPoolTokenB;
  }

  public void setTotalPoolTokenB(BigDecimal totalPoolTokenB) {
    this.totalPoolTokenB = totalPoolTokenB;
  }

  public BigDecimal getPoolTokenRatio() {
    return poolTokenRatio;
  }

  public void setPoolTokenRatio(BigDecimal poolTokenRatio) {
    this.poolTokenRatio = poolTokenRatio;
  }

  public BigDecimal getSwapFee() {
    return swapFee;
  }

  public void setSwapFee(BigDecimal swapFee) {
    this.swapFee = swapFee;
  }

  public BigDecimal getDexFee() {
    return dexFee;
  }

  public void setDexFee(BigDecimal dexFee) {
    this.dexFee = dexFee;
  }

  public BigDecimal getInput() {
    return input;
  }

  public void setInput(BigDecimal input) {
    this.input = input;
  }

  public BigDecimal getOutput() {
    return output;
  }

  public void setOutput(BigDecimal output) {
    this.output = output;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
