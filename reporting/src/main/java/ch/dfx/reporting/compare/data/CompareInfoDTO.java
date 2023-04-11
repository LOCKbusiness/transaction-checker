package ch.dfx.reporting.compare.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class CompareInfoDTO {
  private String liquidityAddress = null;
  private String depositAddress = null;
  private String customerAddress = null;

  private BigDecimal apiTotalAmount = null;
  private BigDecimal dbTotalAmount = null;

  private Set<String> txIdToAPITransactionHistoryDTOSet = null;
  private Set<String> txIdToDBTransactionDTOSet = null;

  private final List<APITransactionHistoryDTO> apiTransactionHistoryDTOList;
  private final List<DBTransactionDTO> dbTransactionDTOList;

  /**
   * 
   */
  public CompareInfoDTO() {
    this.apiTransactionHistoryDTOList = new ArrayList<>();
    this.dbTransactionDTOList = new ArrayList<>();
  }

  public String getLiquidityAddress() {
    return liquidityAddress;
  }

  public void setLiquidityAddress(String liquidityAddress) {
    this.liquidityAddress = liquidityAddress;
  }

  public String getDepositAddress() {
    return depositAddress;
  }

  public void setDepositAddress(String depositAddress) {
    this.depositAddress = depositAddress;
  }

  public String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(String customerAddress) {
    this.customerAddress = customerAddress;
  }

  public BigDecimal getApiTotalAmount() {
    return apiTotalAmount;
  }

  public void setApiTotalAmount(BigDecimal apiTotalAmount) {
    this.apiTotalAmount = apiTotalAmount;
  }

  public BigDecimal getDbTotalAmount() {
    return dbTotalAmount;
  }

  public void setDbTotalAmount(BigDecimal dbTotalAmount) {
    this.dbTotalAmount = dbTotalAmount;
  }

  public Set<String> getTxIdToAPITransactionHistoryDTOSet() {
    return txIdToAPITransactionHistoryDTOSet;
  }

  public void setTxIdToAPITransactionHistoryDTOSet(Set<String> txIdToAPITransactionHistoryDTOSet) {
    this.txIdToAPITransactionHistoryDTOSet = txIdToAPITransactionHistoryDTOSet;
  }

  public Set<String> getTxIdToDBTransactionDTOSet() {
    return txIdToDBTransactionDTOSet;
  }

  public void setTxIdToDBTransactionDTOSet(Set<String> txIdToDBTransactionDTOSet) {
    this.txIdToDBTransactionDTOSet = txIdToDBTransactionDTOSet;
  }

  public List<APITransactionHistoryDTO> getApiTransactionHistoryDTOList() {
    return apiTransactionHistoryDTOList;
  }

  public void addApiTransactionHistoryDTO(APITransactionHistoryDTO apiTransactionHistoryDTO) {
    apiTransactionHistoryDTOList.add(apiTransactionHistoryDTO);
  }

  public List<DBTransactionDTO> getDbTransactionDTOList() {
    return dbTransactionDTOList;
  }

  public void addDBTransactionDTO(DBTransactionDTO dbTransactionDTO) {
    dbTransactionDTOList.add(dbTransactionDTO);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
