package ch.dfx.transactionserver.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.annotation.Nonnull;

/**
 * 
 */
public class StatistikDTO extends DatabaseDTO {
  private final LocalDate day;
  private final int tokenNumber;

  private int depositCount = 0;
  private BigDecimal depositVin = BigDecimal.ZERO;
  private BigDecimal depositVout = BigDecimal.ZERO;

  /**
   * 
   */
  public StatistikDTO(
      @Nonnull LocalDate day,
      int tokenNumber) {
    this.day = day;
    this.tokenNumber = tokenNumber;
  }

  public LocalDate getDay() {
    return day;
  }

  public int getTokenNumber() {
    return tokenNumber;
  }

  public int getDepositCount() {
    return depositCount;
  }

  public void setDepositCount(int depositCount) {
    this.depositCount = depositCount;
  }

  public BigDecimal getDepositVin() {
    return depositVin;
  }

  public void setDepositVin(BigDecimal depositVin) {
    this.depositVin = depositVin;
  }

  public BigDecimal getDepositVout() {
    return depositVout;
  }

  public void setDepositVout(BigDecimal depositVout) {
    this.depositVout = depositVout;
  }
}
