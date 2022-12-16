package ch.dfx.reporting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class BalanceReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(BalanceReporting.class);

  // ...
  private final List<String> logInfoList;

  private BigDecimal totalBalance = null;

  /**
   * 
   */
  public BalanceReporting(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager,
      @Nonnull List<String> logInfoList) {
    super(network, databaseManager);

    this.logInfoList = logInfoList;
  }

  /**
   * 
   */
  public void report(
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    LOGGER.debug("report()");

    Objects.requireNonNull(rootPath, "null 'rootPath' not allowed");
    Objects.requireNonNull(fileName, "null 'fileName' not allowed");
    Objects.requireNonNull(sheet, "null 'sheet' not allowed");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBalanceHelper.openStatements(connection);

      totalBalance = BigDecimal.ZERO;

      List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList(token);

      RowDataList rowDataList = createRowDataList(token, stakingAddressDTOList);

      CellDataList cellDataList = new CellDataList();
      cellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setValue(totalBalance));

      writeExcel(rootPath, fileName, sheet, rowDataList, cellDataList);

      databaseBalanceHelper.closeStatements();
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private RowDataList createRowDataList(
      @Nonnull TokenEnum token,
      @Nonnull List<StakingAddressDTO> stakingAddressDTOList) throws DfxException {
    LOGGER.trace("createRowDataList()");

    RowDataList rowDataList = new RowDataList();

    // ...
    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        LOGGER.debug("Liquidity Address: " + stakingAddressDTO.getLiquidityAddress());

        addDeposit(token, stakingAddressDTO, rowDataList);
      }
    }

    return rowDataList;
  }

  /**
   * 
   */
  private void addDeposit(
      @Nonnull TokenEnum token,
      @Nonnull StakingAddressDTO stakingAddressDTO,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("addDeposit()");

    List<DepositDTO> depositDTOList =
        databaseBalanceHelper.getDepositDTOListByLiquidityAddressNumber(token, stakingAddressDTO.getLiquidityAddressNumber());
    LOGGER.debug("Number of Deposit Addresses: " + depositDTOList.size());

    // ...
    logInfoList.add("Deposit Addresses: " + depositDTOList.size());

    // ...
    depositDTOList.sort(new Comparator<DepositDTO>() {
      @Override
      public int compare(DepositDTO dto1, DepositDTO dto2) {
        return dto2.getStartBlockNumber() - dto1.getStartBlockNumber();
      }
    });

    // ...
    for (DepositDTO depositDTO : depositDTOList) {
      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(depositDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(depositDTO.getDepositAddress()));

      addStaking(token, stakingAddressDTO, depositDTO, rowData);

      rowDataList.add(rowData);
    }
  }

  /**
   * 
   */
  private void addStaking(
      @Nonnull TokenEnum token,
      @Nonnull StakingAddressDTO stakingAddressDTO,
      @Nonnull DepositDTO depositDTO,
      @Nonnull RowData rowData) throws DfxException {
    LOGGER.trace("addStaking()");

    BigDecimal totalVin = BigDecimal.ZERO;
    BigDecimal totalVout = BigDecimal.ZERO;

    List<StakingDTO> stakingDTOList =
        databaseBalanceHelper.getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber(
            token, stakingAddressDTO.getLiquidityAddressNumber(), depositDTO.getDepositAddressNumber());

    for (StakingDTO stakingDTO : stakingDTOList) {
      totalVin = totalVin.add(stakingDTO.getVin());
      totalVout = totalVout.add(stakingDTO.getVout());
    }

    BigDecimal balance = totalVin.subtract(totalVout);
    rowData.addCellData(new CellData().setValue(balance));

    totalBalance = totalBalance.add(balance);
  }
}
