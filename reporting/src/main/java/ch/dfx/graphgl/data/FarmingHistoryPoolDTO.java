package ch.dfx.graphgl.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class FarmingHistoryPoolDTO {
  private String date = null;

  private String pair = null;
  private BigDecimal priceA = null;
  private BigDecimal priceB = null;
  private BigDecimal reserveA = null;
  private BigDecimal reserveB = null;
  private BigDecimal totalLiquidity = null;

  /**
   * 
   */
  public FarmingHistoryPoolDTO() {
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getPair() {
    return pair;
  }

  public void setPair(String pair) {
    this.pair = pair;
  }

  public BigDecimal getPriceA() {
    return priceA;
  }

  public void setPriceA(BigDecimal priceA) {
    this.priceA = priceA;
  }

  public BigDecimal getPriceB() {
    return priceB;
  }

  public void setPriceB(BigDecimal priceB) {
    this.priceB = priceB;
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

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
