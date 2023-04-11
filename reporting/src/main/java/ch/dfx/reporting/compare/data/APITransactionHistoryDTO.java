package ch.dfx.reporting.compare.data;

import java.math.BigDecimal;
import java.util.Date;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class APITransactionHistoryDTO {
  private String type = null;

  private BigDecimal inputAmount = null;
  private String inputAsset = null;

  private BigDecimal outputAmount = null;
  private String outputAsset = null;

  private BigDecimal feeAmount = null;
  private String feeAsset = null;

  private BigDecimal amountInEur = null;
  private BigDecimal amountInChf = null;
  private BigDecimal amountInUsd = null;

  private String txId = null;
  private Date date = null;

  private String source = null;
  private String target = null;
  private String targetAddress = null;

  private String status = null;

  /**
   * 
   */
  public static APITransactionHistoryDTO createUnknownAPITransactionHistoryDTO() {
    APITransactionHistoryDTO apiTransactionHistoryDTO = new APITransactionHistoryDTO();

    apiTransactionHistoryDTO.setType("Unknown");
    apiTransactionHistoryDTO.setTxId("");

    apiTransactionHistoryDTO.setInputAsset("");
    apiTransactionHistoryDTO.setInputAmount(BigDecimal.ZERO);
    apiTransactionHistoryDTO.setOutputAsset("");
    apiTransactionHistoryDTO.setOutputAmount(BigDecimal.ZERO);

    return apiTransactionHistoryDTO;
  }

  /**
   * 
   */
  public APITransactionHistoryDTO() {
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getInputAmount() {
    return inputAmount;
  }

  public void setInputAmount(BigDecimal inputAmount) {
    this.inputAmount = inputAmount;
  }

  public String getInputAsset() {
    return inputAsset;
  }

  public void setInputAsset(String inputAsset) {
    this.inputAsset = inputAsset;
  }

  public BigDecimal getOutputAmount() {
    return outputAmount;
  }

  public void setOutputAmount(BigDecimal outputAmount) {
    this.outputAmount = outputAmount;
  }

  public String getOutputAsset() {
    return outputAsset;
  }

  public void setOutputAsset(String outputAsset) {
    this.outputAsset = outputAsset;
  }

  public BigDecimal getFeeAmount() {
    return feeAmount;
  }

  public void setFeeAmount(BigDecimal feeAmount) {
    this.feeAmount = feeAmount;
  }

  public String getFeeAsset() {
    return feeAsset;
  }

  public void setFeeAsset(String feeAsset) {
    this.feeAsset = feeAsset;
  }

  public BigDecimal getAmountInEur() {
    return amountInEur;
  }

  public void setAmountInEur(BigDecimal amountInEur) {
    this.amountInEur = amountInEur;
  }

  public BigDecimal getAmountInChf() {
    return amountInChf;
  }

  public void setAmountInChf(BigDecimal amountInChf) {
    this.amountInChf = amountInChf;
  }

  public BigDecimal getAmountInUsd() {
    return amountInUsd;
  }

  public void setAmountInUsd(BigDecimal amountInUsd) {
    this.amountInUsd = amountInUsd;
  }

  public String getTxId() {
    return txId;
  }

  public void setTxId(String txId) {
    this.txId = txId;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(String targetAddress) {
    this.targetAddress = targetAddress;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
