package ch.dfx.defichain.data.price;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiFixedIntervalPriceData extends ResultDataA {

  private String fixedIntervalPriceId = null;

  private BigDecimal activePrice = null;
  private BigDecimal nextPrice = null;

  private Long activePriceBlock = null;
  private Long nextPriceBlock = null;

  /**
   * 
   */
  public DefiFixedIntervalPriceData() {
  }

  public String getFixedIntervalPriceId() {
    return fixedIntervalPriceId;
  }

  public void setFixedIntervalPriceId(String fixedIntervalPriceId) {
    this.fixedIntervalPriceId = fixedIntervalPriceId;
  }

  public BigDecimal getActivePrice() {
    return activePrice;
  }

  public void setActivePrice(BigDecimal activePrice) {
    this.activePrice = activePrice;
  }

  public BigDecimal getNextPrice() {
    return nextPrice;
  }

  public void setNextPrice(BigDecimal nextPrice) {
    this.nextPrice = nextPrice;
  }

  public Long getActivePriceBlock() {
    return activePriceBlock;
  }

  public void setActivePriceBlock(Long activePriceBlock) {
    this.activePriceBlock = activePriceBlock;
  }

  public Long getNextPriceBlock() {
    return nextPriceBlock;
  }

  public void setNextPriceBlock(Long nextPriceBlock) {
    this.nextPriceBlock = nextPriceBlock;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
