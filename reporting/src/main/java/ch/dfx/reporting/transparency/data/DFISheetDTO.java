package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DFISheetDTO {
  private String sheetName = "";

  private int numberOfCustomers = 0;
  private BigDecimal customerDeposits = BigDecimal.ZERO;

  private BigDecimal lockLiquidityAmount = BigDecimal.ZERO;
  private BigDecimal lockRewardAmount = BigDecimal.ZERO;

  private BigDecimal lockVault1Balance = BigDecimal.ZERO;
  private BigDecimal lockVault1Collateral = BigDecimal.ZERO;
  private BigDecimal lockVault2Balance = BigDecimal.ZERO;
  private BigDecimal lockVault2Collateral = BigDecimal.ZERO;
  private BigDecimal lockVault3Balance = BigDecimal.ZERO;
  private BigDecimal lockVault3Collateral = BigDecimal.ZERO;

  private BigDecimal lock_LM_BTC_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_BTC_DFI_Pool = BigDecimal.ZERO;
  private BigDecimal lock_LM_ETH_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_ETH_DFI_Pool = BigDecimal.ZERO;
  private BigDecimal lock_LM_DUSD_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_DUSD_DFI_Pool = BigDecimal.ZERO;

  private BigDecimal lock_LM_USDT_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_USDC_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_EUROC_DFI_Balance = BigDecimal.ZERO;
  private BigDecimal lock_LM_SPY_DFI_Balance = BigDecimal.ZERO;

  private BigDecimal customerInterimBalance = BigDecimal.ZERO;
  private BigDecimal lockInterimBalance = BigDecimal.ZERO;
  private BigDecimal lockInterimDifference = BigDecimal.ZERO;

  private BigDecimal lockChangeBTCTransactionBalance = BigDecimal.ZERO;
  private BigDecimal lockChangeETHTransactionBalance = BigDecimal.ZERO;

  private BigDecimal customerTotalBalance = BigDecimal.ZERO;
  private BigDecimal lockTotalBalance = BigDecimal.ZERO;
  private BigDecimal totalDifference = BigDecimal.ZERO;

  /**
   * 
   */
  public DFISheetDTO() {
  }

  public String getSheetName() {
    return sheetName;
  }

  public void setSheetName(String sheetName) {
    this.sheetName = sheetName;
  }

  public int getNumberOfCustomers() {
    return numberOfCustomers;
  }

  public void setNumberOfCustomers(int numberOfCustomers) {
    this.numberOfCustomers = numberOfCustomers;
  }

  public BigDecimal getCustomerDeposits() {
    return customerDeposits;
  }

  public void setCustomerDeposits(BigDecimal customerDeposits) {
    this.customerDeposits = customerDeposits;
  }

  public BigDecimal getLockLiquidityAmount() {
    return lockLiquidityAmount;
  }

  public void setLockLiquidityAmount(BigDecimal lockLiquidityAmount) {
    this.lockLiquidityAmount = lockLiquidityAmount;
  }

  public BigDecimal getLockRewardAmount() {
    return lockRewardAmount;
  }

  public void setLockRewardAmount(BigDecimal lockRewardAmount) {
    this.lockRewardAmount = lockRewardAmount;
  }

  public BigDecimal getLockVault1Balance() {
    return lockVault1Balance;
  }

  public void setLockVault1Balance(BigDecimal lockVault1Balance) {
    this.lockVault1Balance = lockVault1Balance;
  }

  public BigDecimal getLockVault1Collateral() {
    return lockVault1Collateral;
  }

  public void setLockVault1Collateral(BigDecimal lockVault1Collateral) {
    this.lockVault1Collateral = lockVault1Collateral;
  }

  public BigDecimal getLockVault2Balance() {
    return lockVault2Balance;
  }

  public void setLockVault2Balance(BigDecimal lockVault2Balance) {
    this.lockVault2Balance = lockVault2Balance;
  }

  public BigDecimal getLockVault2Collateral() {
    return lockVault2Collateral;
  }

  public void setLockVault2Collateral(BigDecimal lockVault2Collateral) {
    this.lockVault2Collateral = lockVault2Collateral;
  }

  public BigDecimal getLockVault3Balance() {
    return lockVault3Balance;
  }

  public void setLockVault3Balance(BigDecimal lockVault3Balance) {
    this.lockVault3Balance = lockVault3Balance;
  }

  public BigDecimal getLockVault3Collateral() {
    return lockVault3Collateral;
  }

  public void setLockVault3Collateral(BigDecimal lockVault3Collateral) {
    this.lockVault3Collateral = lockVault3Collateral;
  }

  public BigDecimal getLock_LM_BTC_DFI_Balance() {
    return lock_LM_BTC_DFI_Balance;
  }

  public void setLock_LM_BTC_DFI_Balance(BigDecimal lock_LM_BTC_DFI_Balance) {
    this.lock_LM_BTC_DFI_Balance = lock_LM_BTC_DFI_Balance;
  }

  public BigDecimal getLock_LM_BTC_DFI_Pool() {
    return lock_LM_BTC_DFI_Pool;
  }

  public void setLock_LM_BTC_DFI_Pool(BigDecimal lock_LM_BTC_DFI_Pool) {
    this.lock_LM_BTC_DFI_Pool = lock_LM_BTC_DFI_Pool;
  }

  public BigDecimal getLock_LM_ETH_DFI_Balance() {
    return lock_LM_ETH_DFI_Balance;
  }

  public void setLock_LM_ETH_DFI_Balance(BigDecimal lock_LM_ETH_DFI_Balance) {
    this.lock_LM_ETH_DFI_Balance = lock_LM_ETH_DFI_Balance;
  }

  public BigDecimal getLock_LM_ETH_DFI_Pool() {
    return lock_LM_ETH_DFI_Pool;
  }

  public void setLock_LM_ETH_DFI_Pool(BigDecimal lock_LM_ETH_DFI_Pool) {
    this.lock_LM_ETH_DFI_Pool = lock_LM_ETH_DFI_Pool;
  }

  public BigDecimal getLock_LM_DUSD_DFI_Balance() {
    return lock_LM_DUSD_DFI_Balance;
  }

  public void setLock_LM_DUSD_DFI_Balance(BigDecimal lock_LM_DUSD_DFI_Balance) {
    this.lock_LM_DUSD_DFI_Balance = lock_LM_DUSD_DFI_Balance;
  }

  public BigDecimal getLock_LM_DUSD_DFI_Pool() {
    return lock_LM_DUSD_DFI_Pool;
  }

  public void setLock_LM_DUSD_DFI_Pool(BigDecimal lock_LM_DUSD_DFI_Pool) {
    this.lock_LM_DUSD_DFI_Pool = lock_LM_DUSD_DFI_Pool;
  }

  public BigDecimal getLock_LM_USDT_DFI_Balance() {
    return lock_LM_USDT_DFI_Balance;
  }

  public void setLock_LM_USDT_DFI_Balance(BigDecimal lock_LM_USDT_DFI_Balance) {
    this.lock_LM_USDT_DFI_Balance = lock_LM_USDT_DFI_Balance;
  }

  public BigDecimal getLock_LM_USDC_DFI_Balance() {
    return lock_LM_USDC_DFI_Balance;
  }

  public void setLock_LM_USDC_DFI_Balance(BigDecimal lock_LM_USDC_DFI_Balance) {
    this.lock_LM_USDC_DFI_Balance = lock_LM_USDC_DFI_Balance;
  }

  public BigDecimal getLock_LM_EUROC_DFI_Balance() {
    return lock_LM_EUROC_DFI_Balance;
  }

  public void setLock_LM_EUROC_DFI_Balance(BigDecimal lock_LM_EUROC_DFI_Balance) {
    this.lock_LM_EUROC_DFI_Balance = lock_LM_EUROC_DFI_Balance;
  }

  public BigDecimal getLock_LM_SPY_DFI_Balance() {
    return lock_LM_SPY_DFI_Balance;
  }

  public void setLock_LM_SPY_DFI_Balance(BigDecimal lock_LM_SPY_DFI_Balance) {
    this.lock_LM_SPY_DFI_Balance = lock_LM_SPY_DFI_Balance;
  }

  public BigDecimal getCustomerInterimBalance() {
    return customerInterimBalance;
  }

  public void setCustomerInterimBalance(BigDecimal customerInterimBalance) {
    this.customerInterimBalance = customerInterimBalance;
  }

  public BigDecimal getLockInterimBalance() {
    return lockInterimBalance;
  }

  public void setLockInterimBalance(BigDecimal lockInterimBalance) {
    this.lockInterimBalance = lockInterimBalance;
  }

  public BigDecimal getLockInterimDifference() {
    return lockInterimDifference;
  }

  public void setLockInterimDifference(BigDecimal lockInterimDifference) {
    this.lockInterimDifference = lockInterimDifference;
  }

  public BigDecimal getLockChangeBTCTransactionBalance() {
    return lockChangeBTCTransactionBalance;
  }

  public void setLockChangeBTCTransactionBalance(BigDecimal lockChangeBTCTransactionBalance) {
    this.lockChangeBTCTransactionBalance = lockChangeBTCTransactionBalance;
  }

  public BigDecimal getLockChangeETHTransactionBalance() {
    return lockChangeETHTransactionBalance;
  }

  public void setLockChangeETHTransactionBalance(BigDecimal lockChangeETHTransactionBalance) {
    this.lockChangeETHTransactionBalance = lockChangeETHTransactionBalance;
  }

  public BigDecimal getCustomerTotalBalance() {
    return customerTotalBalance;
  }

  public void setCustomerTotalBalance(BigDecimal customerTotalBalance) {
    this.customerTotalBalance = customerTotalBalance;
  }

  public BigDecimal getLockTotalBalance() {
    return lockTotalBalance;
  }

  public void setLockTotalBalance(BigDecimal lockTotalBalance) {
    this.lockTotalBalance = lockTotalBalance;
  }

  public BigDecimal getTotalDifference() {
    return totalDifference;
  }

  public void setTotalDifference(BigDecimal totalDifference) {
    this.totalDifference = totalDifference;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
