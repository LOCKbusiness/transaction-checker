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

  private BigDecimal lockLMBalance1 = BigDecimal.ZERO;
  private BigDecimal lockLMPool1 = BigDecimal.ZERO;
  private BigDecimal lockLMBalance2 = BigDecimal.ZERO;
  private BigDecimal lockLMPool2 = BigDecimal.ZERO;
  private BigDecimal lockLMBalance3 = BigDecimal.ZERO;
  private BigDecimal lockLMPool3 = BigDecimal.ZERO;

  private BigDecimal lockLMBalance4 = BigDecimal.ZERO;
  private BigDecimal lockLMBalance5 = BigDecimal.ZERO;
  private BigDecimal lockLMBalance6 = BigDecimal.ZERO;

  private BigDecimal customerInterimBalance = BigDecimal.ZERO;
  private BigDecimal lockInterimBalance = BigDecimal.ZERO;

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

  public BigDecimal getLockLMBalance1() {
    return lockLMBalance1;
  }

  public void setLockLMBalance1(BigDecimal lockLMBalance1) {
    this.lockLMBalance1 = lockLMBalance1;
  }

  public BigDecimal getLockLMPool1() {
    return lockLMPool1;
  }

  public void setLockLMPool1(BigDecimal lockLMPool1) {
    this.lockLMPool1 = lockLMPool1;
  }

  public BigDecimal getLockLMBalance2() {
    return lockLMBalance2;
  }

  public void setLockLMBalance2(BigDecimal lockLMBalance2) {
    this.lockLMBalance2 = lockLMBalance2;
  }

  public BigDecimal getLockLMPool2() {
    return lockLMPool2;
  }

  public void setLockLMPool2(BigDecimal lockLMPool2) {
    this.lockLMPool2 = lockLMPool2;
  }

  public BigDecimal getLockLMBalance3() {
    return lockLMBalance3;
  }

  public void setLockLMBalance3(BigDecimal lockLMBalance3) {
    this.lockLMBalance3 = lockLMBalance3;
  }

  public BigDecimal getLockLMPool3() {
    return lockLMPool3;
  }

  public void setLockLMPool3(BigDecimal lockLMPool3) {
    this.lockLMPool3 = lockLMPool3;
  }

  public BigDecimal getLockLMBalance4() {
    return lockLMBalance4;
  }

  public void setLockLMBalance4(BigDecimal lockLMBalance4) {
    this.lockLMBalance4 = lockLMBalance4;
  }

  public BigDecimal getLockLMBalance5() {
    return lockLMBalance5;
  }

  public void setLockLMBalance5(BigDecimal lockLMBalance5) {
    this.lockLMBalance5 = lockLMBalance5;
  }

  public BigDecimal getLockLMBalance6() {
    return lockLMBalance6;
  }

  public void setLockLMBalance6(BigDecimal lockLMBalance6) {
    this.lockLMBalance6 = lockLMBalance6;
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
