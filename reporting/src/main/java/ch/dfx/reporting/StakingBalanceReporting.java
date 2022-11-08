package ch.dfx.reporting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingBalanceReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(StakingBalanceReporting.class);

  /**
   * 
   */
  public StakingBalanceReporting(@Nonnull H2DBManager databaseManager) {
    super(databaseManager);
  }

  /**
   * 
   */
  public void report() throws DfxException {
    LOGGER.debug("report()");

    String googleRootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
    String googleFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_STAKING_BALANCE_SHEET);

    if (!StringUtils.isEmpty(googleFileName)) {
      Connection connection = databaseManager.openConnection();
      databaseHelper.openStatements(connection);

      List<StakingAddressDTO> stakingAddressDTOList = databaseHelper.getStakingAddressDTOList();

      RowDataList rowDataList = createRowDataList(stakingAddressDTOList);
      writeExcel(googleRootPath, googleFileName, rowDataList);

      databaseHelper.closeStatements();
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private RowDataList createRowDataList(@Nonnull List<StakingAddressDTO> stakingAddressDTOList) throws DfxException {
    LOGGER.trace("createRowDataList()");

    RowDataList rowDataList = new RowDataList();

    // ...
    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        LOGGER.debug("Liquidity Address: " + stakingAddressDTO.getLiquidityAddress());

        addDeposit(stakingAddressDTO, rowDataList);
      }
    }

    return rowDataList;
  }

  /**
   * 
   */
  private void addDeposit(
      @Nonnull StakingAddressDTO stakingAddressDTO,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("addDeposit()");

    List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOListByLiquidityAddressNumber(stakingAddressDTO.getLiquidityAddressNumber());
    LOGGER.debug("Number of Deposit Addresses: " + depositDTOList.size());

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
      rowData.addCellData(new CellData().setValue(depositDTO.getDepositAddress()));
      rowData.addCellData(new CellData().setValue(depositDTO.getCustomerAddress()));

      addBalance(depositDTO, rowData);
      addStaking(stakingAddressDTO, depositDTO, rowData);

      rowDataList.add(rowData);
    }
  }

  /**
   * 
   */
  private void addBalance(
      @Nonnull DepositDTO depositDTO,
      @Nonnull RowData rowData) throws DfxException {
    LOGGER.trace("addBalance()");

    BalanceDTO balanceDTO = databaseHelper.getBalanceDTOByAddressNumber(depositDTO.getDepositAddressNumber());

    BigDecimal vout = BigDecimal.ZERO;
    BigDecimal vin = BigDecimal.ZERO;

    if (null != balanceDTO) {
      vin = vin.add(balanceDTO.getVin());
      vout = vout.add(balanceDTO.getVout());
    }

    BigDecimal balance = vout.subtract(vin);

    rowData.addCellData(new CellData().setValue(balance));
    rowData.addCellData(new CellData().setValue(vout));
    rowData.addCellData(new CellData().setValue(vin));
  }

  /**
   * 
   */
  private void addStaking(
      @Nonnull StakingAddressDTO stakingAddressDTO,
      @Nonnull DepositDTO depositDTO,
      @Nonnull RowData rowData) throws DfxException {
    LOGGER.trace("addStaking()");

    BigDecimal totalVin = BigDecimal.ZERO;
    BigDecimal totalVout = BigDecimal.ZERO;

    List<StakingDTO> stakingDTOList =
        databaseHelper.getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber(
            stakingAddressDTO.getLiquidityAddressNumber(), depositDTO.getDepositAddressNumber());

    for (StakingDTO stakingDTO : stakingDTOList) {
      totalVin = totalVin.add(stakingDTO.getVin());
      totalVout = totalVout.add(stakingDTO.getVout());
    }

    BigDecimal balance = totalVin.subtract(totalVout);

    rowData.addCellData(new CellData().setValue(balance));
    rowData.addCellData(new CellData().setValue(totalVin));
    rowData.addCellData(new CellData().setValue(totalVout));
  }
}
