package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class TokenSheetDTO {
  private String sheetName = "";

  private int numberOfCustomers = 0;
  private BigDecimal customerDeposits = BigDecimal.ZERO;

  private BigDecimal lockLiquidityAmount = BigDecimal.ZERO;
  private BigDecimal lockRewardAmount = BigDecimal.ZERO;

  private BigDecimal lockVault1Balance = BigDecimal.ZERO;
  private BigDecimal lockVault1Collateral = BigDecimal.ZERO;
  private BigDecimal lockVault1Loan = BigDecimal.ZERO;
  private BigDecimal lockVault2Balance = BigDecimal.ZERO;
  private BigDecimal lockVault2Collateral = BigDecimal.ZERO;
  private BigDecimal lockVault2Loan = BigDecimal.ZERO;
  private BigDecimal lockVault3Balance = BigDecimal.ZERO;
  private BigDecimal lockVault3Collateral = BigDecimal.ZERO;
  private BigDecimal lockVault3Loan = BigDecimal.ZERO;

  private BigDecimal lockLMBalance = BigDecimal.ZERO;
  private BigDecimal lockLMPool = BigDecimal.ZERO;

  private BigDecimal customerInterimBalance = BigDecimal.ZERO;
  private BigDecimal lockInterimBalance = BigDecimal.ZERO;
  private BigDecimal lockInterimDifference = BigDecimal.ZERO;
  private BigDecimal lockChangeTransactionBalance = BigDecimal.ZERO;

  private BigDecimal totalDifference = BigDecimal.ZERO;

  /**
   * 
   */
  public TokenSheetDTO() {
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

  public BigDecimal getLockVault1Loan() {
    return lockVault1Loan;
  }

  public void setLockVault1Loan(BigDecimal lockVault1Loan) {
    this.lockVault1Loan = lockVault1Loan;
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

  public BigDecimal getLockVault2Loan() {
    return lockVault2Loan;
  }

  public void setLockVault2Loan(BigDecimal lockVault2Loan) {
    this.lockVault2Loan = lockVault2Loan;
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

  public BigDecimal getLockVault3Loan() {
    return lockVault3Loan;
  }

  public void setLockVault3Loan(BigDecimal lockVault3Loan) {
    this.lockVault3Loan = lockVault3Loan;
  }

  public BigDecimal getLockLMBalance() {
    return lockLMBalance;
  }

  public void setLockLMBalance(BigDecimal lockLMBalance) {
    this.lockLMBalance = lockLMBalance;
  }

  public BigDecimal getLockLMPool() {
    return lockLMPool;
  }

  public void setLockLMPool(BigDecimal lockLMPool) {
    this.lockLMPool = lockLMPool;
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

  public BigDecimal getLockChangeTransactionBalance() {
    return lockChangeTransactionBalance;
  }

  public void setLockChangeTransactionBalance(BigDecimal lockChangeTransactionBalance) {
    this.lockChangeTransactionBalance = lockChangeTransactionBalance;
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
