package ch.dfx.manager.checker.withdrawal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.manager.ManagerUtils;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingBalanceChecker {
  private static final Logger LOGGER = LogManager.getLogger(StakingBalanceChecker.class);

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public StakingBalanceChecker(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkStakingBalance(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkStakingBalance() ...");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    if (!transactionWithdrawalDTOList.isEmpty()) {
      Connection connection = null;

      try {
        connection = databaseManager.openConnection();

        databaseHelper.openStatements(connection);

        for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
          if (checkStakingBalance(transactionWithdrawalDTO)) {
            checkedTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
          }
        }

        databaseHelper.closeStatements();
      } catch (Exception e) {
        LOGGER.error("checkStakingBalance", e);
      } finally {
        databaseManager.closeConnection(connection);
      }
    }

    return checkedTransactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private boolean checkStakingBalance(@Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) {
    LOGGER.trace("checkStakingBalance() ...");

    try {
      String customerAddress = transactionWithdrawalDTO.getCustomerAddress();

      PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();
      BigDecimal withdrawalAmount = pendingWithdrawalDTO.getAmount();

      AddressDTO addressDTO = databaseHelper.getAddressDTOByAddress(customerAddress);

      if (null == addressDTO) {
        return false;
      }

      int customerAddressNumber = addressDTO.getNumber();

      // ...
      BigDecimal stakingBalance = BigDecimal.ZERO;

      List<StakingDTO> stakingDTOList = databaseHelper.getStakingDTOListByCustomerAddressNumber(customerAddressNumber);

      for (StakingDTO stakingDTO : stakingDTOList) {
        stakingBalance = stakingBalance.add(stakingDTO.getVin().subtract(stakingDTO.getVout()));
      }

      if (-1 == stakingBalance.compareTo(withdrawalAmount)) {
        ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, "invalid balance");
        return false;
      }

      transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.BALANCE_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkStakingBalance", e);
      ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, e.getMessage());
      return false;
    }
  }
}
