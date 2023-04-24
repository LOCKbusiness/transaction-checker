package ch.dfx.reporting.compare;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.Reporting;
import ch.dfx.reporting.compare.data.APIStakingBalanceDTO;
import ch.dfx.reporting.compare.data.APIStakingBalanceDTOList;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class APIBalanceCompareReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(APIBalanceCompareReporting.class);

  // ...
  private static final int SCALE = 8;

  // ...
  private static final String TOTAL_API_BALANCE_PROPERTY = "TOTAL_API_BALANCE";
  private static final String TOTAL_DB_BALANCE_PROPERTY = "TOTAL_DB_BALANCE";
  private static final String TOTAL_DIFF_PROPERTY = "TOTAL_DIFF_BALANCE";

  // ...
  private final BalanceReporting.BalanceReportingTypeEnum reportingType;

  // ...
  private enum StatusEnum {
    OK,
    NOT_FOUND,
    DIFF
  }

  /**
   * 
   */
  public APIBalanceCompareReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull BalanceReporting.BalanceReportingTypeEnum reportingType) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.reportingType = reportingType;
  }

  /**
   * 
   */
  public void report(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.debug("report(): reportingType=" + reportingType);

    long startTime = System.currentTimeMillis();

    try {
      if (BalanceReporting.BalanceReportingTypeEnum.STAKING == reportingType) {
        RowDataList rowDataList = createStakingBalanceRowDataList(apiSelector);
        stakingBalanceReport(reportingTimestamp, rootPath, fileName, rowDataList);
      } else {
        Map<TokenEnum, RowDataList> tokenToRowDataListMap = createYieldmachineBalanceRowDataListMap(apiSelector);
        yieldmachineBalanceReport(reportingTimestamp, rootPath, fileName, tokenToRowDataListMap);
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private RowDataList createStakingBalanceRowDataList(@Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.trace("createStakingBalanceRowDataList()");

    Multimap<String, StakingDTO> stakingCustomerToStakingDTOMap = apiSelector.getStakingCustomerToStakingDTOMap();
    Map<String, APIStakingBalanceDTOList> customerToAPIStakingBalanceDTOListMap = apiSelector.getCustomerToAPIStakingBalanceDTOListMap();

    return createStakingBalanceRowDataList(stakingCustomerToStakingDTOMap, customerToAPIStakingBalanceDTOListMap);
  }

  /**
   * 
   */
  private RowDataList createStakingBalanceRowDataList(
      @Nonnull Multimap<String, StakingDTO> stakingCustomerToStakingDTOMap,
      @Nonnull Map<String, APIStakingBalanceDTOList> customerToAPIStakingBalanceDTOListMap) throws DfxException {
    LOGGER.trace("createStakingBalanceRowDataList()");

    try {
      RowDataList rowDataList = new RowDataList(2);

      BigDecimal totalAPIBalance = BigDecimal.ZERO;
      BigDecimal totalDBBalance = BigDecimal.ZERO;
      BigDecimal totalDifference = BigDecimal.ZERO;

      for (String customerAddress : stakingCustomerToStakingDTOMap.keySet()) {
        APIStakingBalanceDTOList apiStakingBalanceDTOList = customerToAPIStakingBalanceDTOListMap.get(customerAddress);
        List<StakingDTO> dbStakingDTOList = (List<StakingDTO>) stakingCustomerToStakingDTOMap.get(customerAddress);

        Optional<APIStakingBalanceDTO> optionalAPIStakingBalanceDTO =
            apiStakingBalanceDTOList.stream()
                .filter(dto -> "Masternode".equals(dto.getStakingStrategy())).findFirst();

        StatusEnum status = null;

        // ...
        APIStakingBalanceDTO apiStakingBalanceDTO;

        if (optionalAPIStakingBalanceDTO.isEmpty()) {
          LOGGER.info("Customer not found: " + customerAddress);
          status = StatusEnum.NOT_FOUND;
          apiStakingBalanceDTO = new APIStakingBalanceDTO();
          apiStakingBalanceDTO.setBalance(BigDecimal.ZERO);
        } else {
          apiStakingBalanceDTO = optionalAPIStakingBalanceDTO.get();
        }

        // ...
        BigDecimal apiStakingBalance = apiStakingBalanceDTO.getBalance();
        apiStakingBalance = apiStakingBalance.setScale(SCALE, RoundingMode.HALF_UP);
        totalAPIBalance = totalAPIBalance.add(apiStakingBalance);

        BigDecimal dbStakingVin = dbStakingDTOList.get(0).getVin();
        BigDecimal dbStakingVout = dbStakingDTOList.get(0).getVout();
        BigDecimal dbStakingBalance = dbStakingVin.subtract(dbStakingVout);
        dbStakingBalance = dbStakingBalance.setScale(SCALE, RoundingMode.HALF_UP);
        totalDBBalance = totalDBBalance.add(dbStakingBalance);

        BigDecimal difference = apiStakingBalance.subtract(dbStakingBalance);
        difference = difference.setScale(SCALE, RoundingMode.HALF_UP);
        totalDifference = totalDifference.add(difference);

        // ...
        if (0 != apiStakingBalance.compareTo(dbStakingBalance)) {
          LOGGER.info("Different Balance: " + customerAddress + " / " + difference);

          if (null == status) {
            status = StatusEnum.DIFF;
          }
        }

        if (null == status) {
          status = StatusEnum.OK;
        }

        // ...
        RowData rowData = new RowData();
        rowData.addCellData(new CellData().setValue(customerAddress));
        rowData.addCellData(new CellData().setValue(apiStakingBalance));
        rowData.addCellData(new CellData().setValue(dbStakingBalance));
        rowData.addCellData(new CellData().setValue(difference));
        rowData.addCellData(new CellData().setValue(status.toString()));

        rowDataList.add(rowData);
      }

      rowDataList.addProperty(TOTAL_API_BALANCE_PROPERTY, totalAPIBalance);
      rowDataList.addProperty(TOTAL_DB_BALANCE_PROPERTY, totalDBBalance);
      rowDataList.addProperty(TOTAL_DIFF_PROPERTY, totalDifference);

      return rowDataList;
    } catch (Exception e) {
      throw new DfxException("createStakingBalanceRowDataList", e);
    }
  }

  /**
   * 
   */
  private Map<TokenEnum, RowDataList> createYieldmachineBalanceRowDataListMap(@Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.trace("createYieldmachineBalanceRowDataListMap()");

    Multimap<String, StakingDTO> yieldmachineCustomerToStakingDTOMap = apiSelector.getYieldmachineCustomerToStakingDTOMap();
    Map<String, APIStakingBalanceDTOList> customerToAPIStakingBalanceDTOListMap = apiSelector.getCustomerToAPIStakingBalanceDTOListMap();

    return createYieldmachineBalanceRowDataListMap(yieldmachineCustomerToStakingDTOMap, customerToAPIStakingBalanceDTOListMap);
  }

  /**
   * 
   */
  private Map<TokenEnum, RowDataList> createYieldmachineBalanceRowDataListMap(
      @Nonnull Multimap<String, StakingDTO> yieldmachineCustomerToStakingDTOMap,
      @Nonnull Map<String, APIStakingBalanceDTOList> customerToAPIStakingBalanceDTOListMap) throws DfxException {
    LOGGER.trace("createYieldmachineBalanceRowDataListMap()");

    try {
      Map<TokenEnum, RowDataList> tokenToRowDataListMap = new EnumMap<>(TokenEnum.class);

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: activate SPY is used ...
        if (TokenEnum.SPY != token) {
          int tokenNumber = token.getNumber();

          RowDataList rowDataList = new RowDataList(2);

          BigDecimal totalAPIBalance = BigDecimal.ZERO;
          BigDecimal totalDBBalance = BigDecimal.ZERO;
          BigDecimal totalDifference = BigDecimal.ZERO;

          // ...
          for (String customerAddress : yieldmachineCustomerToStakingDTOMap.keySet()) {
            APIStakingBalanceDTOList apiStakingBalanceDTOList = customerToAPIStakingBalanceDTOListMap.get(customerAddress);

            Optional<APIStakingBalanceDTO> optionalAPIStakingBalanceDTO =
                apiStakingBalanceDTOList.stream()
                    .filter(dto -> token.toString().equals(dto.getAsset()))
                    .filter(dto -> "LiquidityMining".equals(dto.getStakingStrategy()))
                    .findFirst();

            List<StakingDTO> dbStakingDTOList = (List<StakingDTO>) yieldmachineCustomerToStakingDTOMap.get(customerAddress);

            Optional<StakingDTO> optionalStakingDTO =
                dbStakingDTOList.stream()
                    .filter(dto -> tokenNumber == dto.getTokenNumber())
                    .findFirst();

            // ...
            if (optionalAPIStakingBalanceDTO.isEmpty()
                && optionalStakingDTO.isEmpty()) {
              continue;
            }

            // ...
            StatusEnum status = null;

            APIStakingBalanceDTO apiStakingBalanceDTO;
            StakingDTO dbStakingDTO;

            if (optionalAPIStakingBalanceDTO.isEmpty()) {
              status = StatusEnum.NOT_FOUND;

              apiStakingBalanceDTO = new APIStakingBalanceDTO();
              apiStakingBalanceDTO.setBalance(BigDecimal.ZERO);
            } else {
              apiStakingBalanceDTO = optionalAPIStakingBalanceDTO.get();
            }

            if (optionalStakingDTO.isEmpty()) {
              dbStakingDTO = new StakingDTO(tokenNumber, 0, 0, 0);
              dbStakingDTO.setVin(BigDecimal.ZERO);
              dbStakingDTO.setVout(BigDecimal.ZERO);
            } else {
              dbStakingDTO = optionalStakingDTO.get();
            }

            // ...
            BigDecimal apiStakingBalance = apiStakingBalanceDTO.getBalance();
            apiStakingBalance = apiStakingBalance.setScale(SCALE, RoundingMode.HALF_UP);
            totalAPIBalance = totalAPIBalance.add(apiStakingBalance);

            BigDecimal dbStakingVin = dbStakingDTO.getVin();
            BigDecimal dbStakingVout = dbStakingDTO.getVout();
            BigDecimal dbStakingBalance = dbStakingVin.subtract(dbStakingVout);
            dbStakingBalance = dbStakingBalance.setScale(SCALE, RoundingMode.HALF_UP);
            totalDBBalance = totalDBBalance.add(dbStakingBalance);

            BigDecimal difference = apiStakingBalance.subtract(dbStakingBalance);
            difference = difference.setScale(SCALE, RoundingMode.HALF_UP);
            totalDifference = totalDifference.add(difference);

            // ...
            if (0 != apiStakingBalance.compareTo(dbStakingBalance)) {
              LOGGER.info("Different Balance: " + customerAddress + " / " + difference);

              if (null == status) {
                status = StatusEnum.DIFF;
              }
            }

            if (null == status) {
              status = StatusEnum.OK;
            }

            // ...
            RowData rowData = new RowData();
            rowData.addCellData(new CellData().setValue(customerAddress));
            rowData.addCellData(new CellData().setValue(apiStakingBalance));
            rowData.addCellData(new CellData().setValue(dbStakingBalance));
            rowData.addCellData(new CellData().setValue(difference));
            rowData.addCellData(new CellData().setValue(status.toString()));

            rowDataList.add(rowData);
          }

          rowDataList.addProperty(TOTAL_API_BALANCE_PROPERTY, totalAPIBalance);
          rowDataList.addProperty(TOTAL_DB_BALANCE_PROPERTY, totalDBBalance);
          rowDataList.addProperty(TOTAL_DIFF_PROPERTY, totalDifference);

          tokenToRowDataListMap.put(token, rowDataList);
        }
      }

      return tokenToRowDataListMap;
    } catch (Exception e) {
      throw new DfxException("createYieldmachineBalanceRowDataListMap", e);
    }
  }

  /**
   * 
   */
  private void stakingBalanceReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("stakingBalanceReport()");

    BigDecimal totalAPIBalance = (BigDecimal) rowDataList.getProperty(TOTAL_API_BALANCE_PROPERTY);
    BigDecimal totalDBBalance = (BigDecimal) rowDataList.getProperty(TOTAL_DB_BALANCE_PROPERTY);
    BigDecimal totalDifference = (BigDecimal) rowDataList.getProperty(TOTAL_DIFF_PROPERTY);

    // ...
    String sheetName = TokenEnum.DFI.toString();

    openExcel(rootPath, fileName, sheetName);

    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(totalAPIBalance));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(totalDBBalance));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(3).setKeepStyle(true).setValue(totalDifference));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(4).setKeepStyle(true).setValue(reportingTimestamp));

    cleanExcel(2);
    cleanExcel(cleanCellDataList);

    writeExcel(rowDataList, new CellDataList());
    closeExcel();
  }

  /**
   * 
   */
  private void yieldmachineBalanceReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull Map<TokenEnum, RowDataList> tokenToRowDataListMap) throws DfxException {
    LOGGER.trace("yieldmachineBalanceReport()");

    openExcel(rootPath, fileName);

    for (TokenEnum token : TokenEnum.values()) {
      // TODO: activate SPY is used ...
      if (TokenEnum.SPY != token) {
        RowDataList rowDataList = tokenToRowDataListMap.get(token);

        BigDecimal totalAPIBalance = (BigDecimal) rowDataList.getProperty(TOTAL_API_BALANCE_PROPERTY);
        BigDecimal totalDBBalance = (BigDecimal) rowDataList.getProperty(TOTAL_DB_BALANCE_PROPERTY);
        BigDecimal totalDifference = (BigDecimal) rowDataList.getProperty(TOTAL_DIFF_PROPERTY);

        String sheetName = token.toString();
        setSheet(sheetName);

        CellDataList cleanCellDataList = new CellDataList();
        cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(totalAPIBalance));
        cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(totalDBBalance));
        cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(3).setKeepStyle(true).setValue(totalDifference));
        cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(4).setKeepStyle(true).setValue(reportingTimestamp));

        cleanExcel(2);
        cleanExcel(cleanCellDataList);

        writeExcel(rowDataList, new CellDataList());
      }
    }

    closeExcel();
  }
}
