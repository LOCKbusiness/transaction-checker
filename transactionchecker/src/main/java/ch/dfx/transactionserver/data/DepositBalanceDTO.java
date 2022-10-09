package ch.dfx.transactionserver.data;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DepositBalanceDTO {
  private final DepositDTO depositDTO;
  private final BalanceDTO balanceDTO;

  /**
   * 
   */
  public DepositBalanceDTO(
      @Nonnull DepositDTO depositDTO,
      @Nonnull BalanceDTO balanceDTO) {
    this.depositDTO = depositDTO;
    this.balanceDTO = balanceDTO;
  }

  public DepositDTO getDepositDTO() {
    return depositDTO;
  }

  public BalanceDTO getBalanceDTO() {
    return balanceDTO;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
