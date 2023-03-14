package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class TotalSheetDTO {
  private BigDecimal amount = BigDecimal.ZERO;
  private BigDecimal price = BigDecimal.ZERO;
  private BigDecimal value = BigDecimal.ZERO;

  /**
   * 
   */
  public TotalSheetDTO() {
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
