package ch.dfx.reporting.compare.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class APIStakingBalanceDTO {

  private String asset = null;
  private BigDecimal balance = null;
  private String blockchain = null;
  private String stakingStrategy = null;

  /**
   * 
   */
  public APIStakingBalanceDTO() {
  }

  public String getAsset() {
    return asset;
  }

  public void setAsset(String asset) {
    this.asset = asset;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public String getBlockchain() {
    return blockchain;
  }

  public void setBlockchain(String blockchain) {
    this.blockchain = blockchain;
  }

  public String getStakingStrategy() {
    return stakingStrategy;
  }

  public void setStakingStrategy(String stakingStrategy) {
    this.stakingStrategy = stakingStrategy;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
