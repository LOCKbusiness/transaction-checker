package ch.dfx.reporting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class BalanceReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(BalanceReporting.class);

  // ...
  public static final String TOTAL_BALANCE_PROPERTY = "TOTAL_BALANCE";

  // ...
  public enum BalanceReportingTypeEnum {
    STAKING,
    YIELD_MACHINE
  }

  // ...
  private final List<String> logInfoList;
  private final BalanceReportingTypeEnum reportingType;

  /**
   * 
   */
  public BalanceReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull List<String> logInfoList,
      @Nonnull BalanceReportingTypeEnum reportingType) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.logInfoList = logInfoList;
    this.reportingType = reportingType;
  }

  /**
   * 
   */
  public void report(
      @Nonnull Connection connection,
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    LOGGER.debug("report(): reportingType=" + reportingType);

    long startTime = System.currentTimeMillis();

    try {
      if (BalanceReportingTypeEnum.STAKING == reportingType) {
        RowDataList stakingCustomerRowDataList = createStakingCustomerRowDataList();
        logInfoList.add("Addresses: " + stakingCustomerRowDataList.size() + " (" + reportingType + ")");

        stakingBalanceReport(reportingTimestamp, rootPath, fileName, sheet, stakingCustomerRowDataList);
      } else {
        RowDataList yieldmachineCustomerRowDataList = createYieldmachineRowDataList();
        logInfoList.add("Addresses: " + yieldmachineCustomerRowDataList.size() + " (" + reportingType + ")");

        yieldmachineBalanceReport(reportingTimestamp, rootPath, fileName, sheet, yieldmachineCustomerRowDataList);
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  public RowDataList createStakingCustomerRowDataList() throws DfxException {
    LOGGER.trace("createStakingCustomerRowDataList()");

    Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = getDepositAddressToStakingDTOMap();
    return createStakingCustomerRowDataList(depositAddressToStakingDTOMap);
  }

  /**
   * 
   */
  public RowDataList createStakingCustomerRowDataList(@Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createStakingCustomerRowDataList()");

    RowDataList rowDataList = new RowDataList(2);

    BigDecimal totalBalance = BigDecimal.ZERO;

    for (Integer depositAddressNumber : depositAddressToStakingDTOMap.keySet()) {
      List<StakingDTO> stakingDTOList = (List<StakingDTO>) depositAddressToStakingDTOMap.get(depositAddressNumber);

      StakingDTO firstStakingDTO = stakingDTOList.get(0);

      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getDepositAddress()));

      // ...
      Map<TokenEnum, BigDecimal> tokenToBalanceMap = new EnumMap<>(TokenEnum.class);

      for (StakingDTO stakingDTO : stakingDTOList) {
        TokenEnum token = TokenEnum.createWithNumber(stakingDTO.getTokenNumber());
        BigDecimal balance = stakingDTO.getVin().subtract(stakingDTO.getVout());

        tokenToBalanceMap.merge(token, balance, (currVal, newVal) -> currVal.add(newVal));

        totalBalance = totalBalance.add(balance);
      }

      // ...
      BigDecimal balance = tokenToBalanceMap.getOrDefault(TokenEnum.DFI, BigDecimal.ZERO);
      rowData.addCellData(new CellData().setValue(balance));

      rowDataList.add(rowData);
    }

    rowDataList.addProperty(TOTAL_BALANCE_PROPERTY, totalBalance);

    return rowDataList;
  }

  /**
   * 
   */
  public RowDataList createYieldmachineRowDataList() throws DfxException {
    LOGGER.trace("createYieldmachineRowDataList()");

    Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = getDepositAddressToStakingDTOMap();
    return createYieldmachineRowDataList(depositAddressToStakingDTOMap);
  }

  /**
   * 
   */
  public RowDataList createYieldmachineRowDataList(@Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createYieldmachineRowDataList()");

    RowDataList rowDataList = new RowDataList(2);

    Map<TokenEnum, BigDecimal> tokenToTotalBalanceMap = new EnumMap<>(TokenEnum.class);

    for (Integer depositAddressNumber : depositAddressToStakingDTOMap.keySet()) {
      List<StakingDTO> stakingDTOList = (List<StakingDTO>) depositAddressToStakingDTOMap.get(depositAddressNumber);

      StakingDTO firstStakingDTO = stakingDTOList.get(0);

      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getDepositAddress()));

      // ...
      Map<TokenEnum, BigDecimal> tokenToBalanceMap = new EnumMap<>(TokenEnum.class);

      for (StakingDTO stakingDTO : stakingDTOList) {
        TokenEnum token = TokenEnum.createWithNumber(stakingDTO.getTokenNumber());
        BigDecimal balance = stakingDTO.getVin().subtract(stakingDTO.getVout());

        tokenToBalanceMap.merge(token, balance, (currVal, newVal) -> currVal.add(newVal));

        tokenToTotalBalanceMap.merge(token, balance, (currVal, newVal) -> currVal.add(newVal));
      }

      // ...
      for (TokenEnum token : TokenEnum.values()) {
        // TODO: currently without SPY, coming later ...
        if (TokenEnum.SPY != token) {
          BigDecimal balance = tokenToBalanceMap.getOrDefault(token, BigDecimal.ZERO);
          rowData.addCellData(new CellData().setValue(balance));
        }
      }

      rowDataList.add(rowData);
    }

    rowDataList.addProperty(TOTAL_BALANCE_PROPERTY, tokenToTotalBalanceMap);

    return rowDataList;
  }

  /**
   * 
   */
  public Multimap<Integer, StakingDTO> getDepositAddressToStakingDTOMap() throws DfxException {
    LOGGER.debug("getDepositAddressToStakingDTOMap()");

    // ...
    List<StakingDTO> stakingDTOList = new ArrayList<>();

    for (TokenEnum token : TokenEnum.values()) {
      stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(token));
    }

    // ...
    stakingDTOList.sort(new Comparator<StakingDTO>() {
      @Override
      public int compare(StakingDTO dto1, StakingDTO dto2) {
        int dto1BlockNumber = Math.max(dto1.getLastInBlockNumber(), dto1.getLastOutBlockNumber());
        int dto2BlockNumber = Math.max(dto2.getLastInBlockNumber(), dto2.getLastOutBlockNumber());

        int compare = dto2BlockNumber - dto1BlockNumber;

        if (0 == compare) {
          compare = ObjectUtils.compare(dto1.getDepositAddress(), dto2.getDepositAddress());
        }

        return compare;
      }
    });

    Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = ArrayListMultimap.create();

    for (StakingDTO stakingDTO : stakingDTOList) {
      depositAddressToStakingDTOMap.put(stakingDTO.getDepositAddressNumber(), stakingDTO);
    }

    return depositAddressToStakingDTOMap;
  }

  /**
   * 
   */
  private void stakingBalanceReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull RowDataList customerRowDataList) throws DfxException {
    LOGGER.trace("stakingBalanceReport()");

    BigDecimal dfiTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_BALANCE_PROPERTY);

    // ...
    openExcel(rootPath, fileName, sheet);

    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingTimestamp));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(dfiTotalBalance));

    cleanExcel(2);
    cleanExcel(cleanCellDataList);

    writeExcel(customerRowDataList, new CellDataList());
    closeExcel();
  }

  /**
   * 
   */
  private void yieldmachineBalanceReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull RowDataList customerRowDataList) throws DfxException {
    LOGGER.trace("yieldmachineBalanceReport()");

    // ...
    openExcel(rootPath, fileName, sheet);

    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingTimestamp));

    @SuppressWarnings("unchecked")
    Map<TokenEnum, BigDecimal> tokenToTotalBalanceMap = (EnumMap<TokenEnum, BigDecimal>) customerRowDataList.getProperty(TOTAL_BALANCE_PROPERTY);

    int cellIndex = 2;

    for (TokenEnum token : TokenEnum.values()) {
      // TODO: currently without SPY, coming later ...
      if (TokenEnum.SPY != token) {
        cleanCellDataList.add(
            new CellData().setRowIndex(0).setCellIndex(cellIndex++).setKeepStyle(true).setValue(tokenToTotalBalanceMap.getOrDefault(token, BigDecimal.ZERO)));
      }
    }

    cleanExcel(2);
    cleanExcel(cleanCellDataList);

    writeExcel(customerRowDataList, new CellDataList());
    closeExcel();
  }
}
