package ch.dfx.transactionserver.data;

/**
 * 
 */
public class StakingAddressDTO extends DatabaseDTO {
  private final int tokenNumber;

  private int liquidityAddressNumber = -1;
  private int rewardAddressNumber = -1;
  private int startBlockNumber = -1;
  private int startTransactionNumber = -1;

  private String liquidityAddress = null;
  private String rewardAddress = null;

  /**
   * 
   */
  public StakingAddressDTO(int tokenNumber) {
    this.tokenNumber = tokenNumber;
  }

  public int getTokenNumber() {
    return tokenNumber;
  }

  public int getLiquidityAddressNumber() {
    return liquidityAddressNumber;
  }

  public void setLiquidityAddressNumber(int liquidityAddressNumber) {
    this.liquidityAddressNumber = liquidityAddressNumber;
  }

  public int getRewardAddressNumber() {
    return rewardAddressNumber;
  }

  public void setRewardAddressNumber(int rewardAddressNumber) {
    this.rewardAddressNumber = rewardAddressNumber;
  }

  public int getStartBlockNumber() {
    return startBlockNumber;
  }

  public void setStartBlockNumber(int startBlockNumber) {
    this.startBlockNumber = startBlockNumber;
  }

  public int getStartTransactionNumber() {
    return startTransactionNumber;
  }

  public void setStartTransactionNumber(int startTransactionNumber) {
    this.startTransactionNumber = startTransactionNumber;
  }

  public String getLiquidityAddress() {
    return liquidityAddress;
  }

  public void setLiquidityAddress(String liquidityAddress) {
    this.liquidityAddress = liquidityAddress;
  }

  public String getRewardAddress() {
    return rewardAddress;
  }

  public void setRewardAddress(String rewardAddress) {
    this.rewardAddress = rewardAddress;
  }
}
