package ch.dfx;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.excel.ExcelWriter;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class Reporting implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(Reporting.class);

  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public Reporting() {
    this.databaseManager = new H2DBManagerImpl();
    this.databaseHelper = new DatabaseHelper();
  }

  @Override
  public String getName() {
    return Reporting.class.getSimpleName();
  }

  @Override
  public boolean isProcessing() {
    return true;
  }

  @Override
  public void run() {
    LOGGER.trace("run()");

    try {
      fillExcel();
      LOGGER.debug("Reporting updated");
    } catch (Throwable t) {
      LOGGER.error("run", t);
    }
  }

  /**
   * 
   */
  private void fillExcel() throws DfxException {
    LOGGER.trace("fillExcel()");

    Connection connection = databaseManager.openConnection();
    databaseHelper.openStatements(connection);

    List<LiquidityDTO> liquidityDTOList = databaseHelper.getLiquidityDTOList();
    fillExcel(liquidityDTOList);

    databaseHelper.closeStatements();
    databaseManager.closeConnection(connection);
  }

  /**
   * 
   */
  private void fillExcel(@Nonnull List<LiquidityDTO> liquidityDTOList) throws DfxException {
    LOGGER.trace("fillExcel()");

    try {
      String googleRootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String googleStakingBalanceSheet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_STAKING_BALANCE_SHEET);

      LOGGER.debug("Staking Balance Sheet: " + googleStakingBalanceSheet);

      // ...
      File excelStakingBalanceFile = new File(googleRootPath, googleStakingBalanceSheet);

      ExcelWriter excelWriter = new ExcelWriter();
      excelWriter.openWorkbook(excelStakingBalanceFile);

      // ...
      RowDataList rowDataList = new RowDataList();

      // ...
      for (LiquidityDTO liquidityDTO : liquidityDTOList) {
        LOGGER.debug("Liquidity Address: " + liquidityDTO.getAddress());

        addDeposit(liquidityDTO, rowDataList);
      }

      excelWriter.insertRowData(rowDataList);

      // ...
      excelWriter.writeWorkbook(excelStakingBalanceFile);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("fillExcel", e);
    }
  }

  /**
   * 
   */
  private void addDeposit(
      @Nonnull LiquidityDTO liquidityDTO,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("addDeposit()");

    List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOListByLiquidityAddressNumber(liquidityDTO.getAddressNumber());
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
      addStaking(liquidityDTO, depositDTO, rowData);

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
      @Nonnull LiquidityDTO liquidityDTO,
      @Nonnull DepositDTO depositDTO,
      @Nonnull RowData rowData) throws DfxException {
    LOGGER.trace("addStaking()");

    BigDecimal totalVin = BigDecimal.ZERO;
    BigDecimal totalVout = BigDecimal.ZERO;

    List<StakingDTO> stakingDTOList =
        databaseHelper.getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber(
            liquidityDTO.getAddressNumber(), depositDTO.getDepositAddressNumber());

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
