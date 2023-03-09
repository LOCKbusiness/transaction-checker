package ch.dfx.defichain.data.pool;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiPoolPairData extends ResultDataA {

  private String symbol = null;
  private Boolean status = null;

  private String idTokenA = null;
  private String idTokenB = null;

  private BigDecimal reserveA = null;
  private BigDecimal reserveB = null;

  private BigDecimal totalLiquidity = null;

  private BigDecimal commission = null;

  private BigDecimal dexFeeInPctTokenA = null;
  private BigDecimal dexFeeOutPctTokenA = null;

  /**
   * 
   */
  public DefiPoolPairData() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Boolean getStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

  public String getIdTokenA() {
    return idTokenA;
  }

  public void setIdTokenA(String idTokenA) {
    this.idTokenA = idTokenA;
  }

  public String getIdTokenB() {
    return idTokenB;
  }

  public void setIdTokenB(String idTokenB) {
    this.idTokenB = idTokenB;
  }

  public BigDecimal getReserveA() {
    return reserveA;
  }

  public void setReserveA(BigDecimal reserveA) {
    this.reserveA = reserveA;
  }

  public BigDecimal getReserveB() {
    return reserveB;
  }

  public void setReserveB(BigDecimal reserveB) {
    this.reserveB = reserveB;
  }

  public BigDecimal getTotalLiquidity() {
    return totalLiquidity;
  }

  public void setTotalLiquidity(BigDecimal totalLiquidity) {
    this.totalLiquidity = totalLiquidity;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public void setCommission(BigDecimal commission) {
    this.commission = commission;
  }

  public BigDecimal getDexFeeInPctTokenA() {
    return dexFeeInPctTokenA;
  }

  public void setDexFeeInPctTokenA(BigDecimal dexFeeInPctTokenA) {
    this.dexFeeInPctTokenA = dexFeeInPctTokenA;
  }

  public BigDecimal getDexFeeOutPctTokenA() {
    return dexFeeOutPctTokenA;
  }

  public void setDexFeeOutPctTokenA(BigDecimal dexFeeOutPctTokenA) {
    this.dexFeeOutPctTokenA = dexFeeOutPctTokenA;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
