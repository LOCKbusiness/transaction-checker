package ch.dfx.manager.checker.withdrawal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.manager.ManagerUtils;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.data.StakingWithdrawalReservedDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingBalanceChecker {
  private static final Logger LOGGER = LogManager.getLogger(StakingBalanceChecker.class);

  // ...
  private PreparedStatement stakingWithdrawalReservedSelectByCustomerAddressStatement = null;
  private PreparedStatement stakingWithdrawalReservedInsertStatement = null;

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
    LOGGER.trace("checkStakingBalance()");

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
    LOGGER.trace("checkStakingBalance()");

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
        ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, "invalid balance");
        return false;
      }

      transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.BALANCE_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkStakingBalance", e);
      ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, e.getMessage());
      return false;
    }
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkStakingBalanceNew(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkStakingBalance()");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    if (!transactionWithdrawalDTOList.isEmpty()) {
      Connection connection = null;

      try {
        connection = databaseManager.openConnection();

        databaseHelper.openStatements(connection);
        openStatementsNew(connection);

        for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
          if (checkStakingBalanceNew(connection, transactionWithdrawalDTO)) {
            checkedTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
          }
        }

        closeStatementsNew();
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
  private void openStatementsNew(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingWithdrawalReservedSelectByCustomerAddressSql = "SELECT * FROM public.staking_withdrawal_reserved WHERE customer_address=?";
      stakingWithdrawalReservedSelectByCustomerAddressStatement = connection.prepareStatement(stakingWithdrawalReservedSelectByCustomerAddressSql);

      String stakingWithdrawalReservedInsertSql =
          "INSERT INTO public.staking_withdrawal_reserved"
              + " (withdrawal_id, transaction_id, customer_address, vout)"
              + " VALUES (?, ?, ?, ?)";
      stakingWithdrawalReservedInsertStatement = connection.prepareStatement(stakingWithdrawalReservedInsertSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatementsNew() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      stakingWithdrawalReservedSelectByCustomerAddressStatement.close();
      stakingWithdrawalReservedInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private boolean checkStakingBalanceNew(
      @Nonnull Connection connection,
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) {
    LOGGER.trace("checkStakingBalance()");

    try {
      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = getStakingWithdrawalReservedDTOList(transactionWithdrawalDTO);

      BigDecimal stakingWithdrawalReservedVout = BigDecimal.ZERO;

      for (StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO : stakingWithdrawalReservedDTOList) {
        stakingWithdrawalReservedVout = stakingWithdrawalReservedVout.add(stakingWithdrawalReservedDTO.getVout());
      }

      // ...
      BigDecimal withdrawalAmount = BigDecimal.ZERO;

      String transactionId = transactionWithdrawalDTO.getOpenTransactionDTO().getId();
      boolean isTransactionIdInReservedList = stakingWithdrawalReservedDTOList.stream().anyMatch(dto -> transactionId.equals(dto.getTransactionId()));

      if (!isTransactionIdInReservedList) {
        PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();
        withdrawalAmount = withdrawalAmount.add(pendingWithdrawalDTO.getAmount());
      }

      BigDecimal totalWithdrawal = stakingWithdrawalReservedVout.add(withdrawalAmount);

      // ...
      BigDecimal stakingBalance = getStakingBalanceNew(transactionWithdrawalDTO);

      // ...
      if (-1 == stakingBalance.compareTo(totalWithdrawal)) {
        ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, "invalid balance");
        return false;
      }

      if (!isTransactionIdInReservedList) {
        insertStakingWithdrawalReservedNew(connection, transactionWithdrawalDTO);
      }

      transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.BALANCE_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkStakingBalance: " + e.getMessage(), e);

      ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, e.getMessage());
      return false;
    }
  }

  /**
   *  
   */
  private List<StakingWithdrawalReservedDTO> getStakingWithdrawalReservedDTOList(
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("getStakingWithdrawalReservedDTOList()");

    try {
      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = new ArrayList<>();

      String customerAddress = transactionWithdrawalDTO.getCustomerAddress();
      stakingWithdrawalReservedSelectByCustomerAddressStatement.setString(1, customerAddress);

      ResultSet resultSet = stakingWithdrawalReservedSelectByCustomerAddressStatement.executeQuery();

      while (resultSet.next()) {
        StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO = new StakingWithdrawalReservedDTO();

        stakingWithdrawalReservedDTO.setWithdrawalId(resultSet.getInt("withdrawal_id"));
        stakingWithdrawalReservedDTO.setTransactionId(resultSet.getString("transaction_id"));
        stakingWithdrawalReservedDTO.setCustomerAddress(resultSet.getString("customer_address"));
        stakingWithdrawalReservedDTO.setVout(resultSet.getBigDecimal("vout"));

        stakingWithdrawalReservedDTOList.add(stakingWithdrawalReservedDTO);
      }

      resultSet.close();

      return stakingWithdrawalReservedDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingWithdrawalReservedDTOList", e);
    }
  }

  /**
   * 
   */
  private BigDecimal getStakingBalanceNew(@Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("getStakingBalance()");

    BigDecimal stakingBalance = BigDecimal.ZERO;

    String customerAddress = transactionWithdrawalDTO.getCustomerAddress();
    AddressDTO addressDTO = databaseHelper.getAddressDTOByAddress(customerAddress);

    if (null != addressDTO) {
      int customerAddressNumber = addressDTO.getNumber();

      List<StakingDTO> stakingDTOList = databaseHelper.getStakingDTOListByCustomerAddressNumber(customerAddressNumber);

      for (StakingDTO stakingDTO : stakingDTOList) {
        stakingBalance = stakingBalance.add(stakingDTO.getVin().subtract(stakingDTO.getVout()));
      }
    }

    return stakingBalance;
  }

  /**
   * 
   */
  private void insertStakingWithdrawalReservedNew(
      @Nonnull Connection connection,
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("insertStakingWithdrawalReserved()");

    try {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();
      PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();

      stakingWithdrawalReservedInsertStatement.setInt(1, pendingWithdrawalDTO.getId());
      stakingWithdrawalReservedInsertStatement.setString(2, openTransactionDTO.getId());
      stakingWithdrawalReservedInsertStatement.setString(3, transactionWithdrawalDTO.getCustomerAddress());
      stakingWithdrawalReservedInsertStatement.setBigDecimal(4, pendingWithdrawalDTO.getAmount());

      stakingWithdrawalReservedInsertStatement.execute();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("insertStakingWithdrawalReserved: " + e.getMessage(), e);
    }
  }
}
