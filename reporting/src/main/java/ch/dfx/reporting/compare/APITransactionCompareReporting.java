package ch.dfx.reporting.compare;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.ExcelReader;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.CellValueMap;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.Reporting;
import ch.dfx.reporting.compare.data.APITransactionHistoryDTO;
import ch.dfx.reporting.compare.data.CompareInfoDTO;
import ch.dfx.reporting.compare.data.DBTransactionDTO;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class APITransactionCompareReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(APITransactionCompareReporting.class);

  // ...
  private final BalanceReporting.BalanceReportingTypeEnum reportingType;

  private final APITransactionHistoryProvider apiTransactionHistoryProvider;

  /**
   * 
   */
  public APITransactionCompareReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull BalanceReporting.BalanceReportingTypeEnum reportingType) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.reportingType = reportingType;

    this.apiTransactionHistoryProvider = new APITransactionHistoryProvider(network, databaseBlockHelper);
  }

  /**
   * 
   */
  public void report(
      @Nonnull Connection connection,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheetName,
      @Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.debug("report(): reportingType=" + reportingType);

    long startTime = System.currentTimeMillis();

    try {
      if (BalanceReporting.BalanceReportingTypeEnum.STAKING == reportingType) {
        RowDataList rowDataList = createStakingTransactionRowDataList(connection, rootPath, fileName, apiSelector);
        transactionReport(rootPath, fileName, sheetName, rowDataList);
      } else {
        Map<TokenEnum, RowDataList> tokenToRowDataListMap = createYieldmachineTransactionRowDataListMap(connection, rootPath, fileName, apiSelector);
        yieldmachineReport(rootPath, fileName, sheetName, tokenToRowDataListMap);
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private RowDataList createStakingTransactionRowDataList(
      @Nonnull Connection connection,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.trace("createStakingTransactionRowDataList()");

    List<String> liquidityAddressList = createLiquidityAddressList();
    List<String> notOkCustomerAddressList = createStakingNotOkCustomerAddressList(rootPath, fileName);

    TokenEnum token = TokenEnum.DFI;

    return createRowDataList(connection, token, liquidityAddressList, apiSelector, notOkCustomerAddressList);
  }

  /**
   * 
   */
  private Map<TokenEnum, RowDataList> createYieldmachineTransactionRowDataListMap(
      @Nonnull Connection connection,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull APISelector apiSelector) throws DfxException {
    LOGGER.trace("createYieldmachineTransactionRowDataList()");

    List<String> liquidityAddressList = createLiquidityAddressList();
    Map<TokenEnum, List<String>> yieldmachineNotOkCustomerAddressListMap = createYieldmachineNotOkCustomerAddressListMap(rootPath, fileName);

    Map<TokenEnum, RowDataList> tokenToRowDataListMap = new HashMap<>();

    for (TokenEnum token : TokenEnum.values()) {
      // TODO: activate SPY is used ...
      if (TokenEnum.SPY != token) {
        List<String> notOkCustomerAddressList = yieldmachineNotOkCustomerAddressListMap.get(token);

        RowDataList rowDataList = createRowDataList(connection, token, liquidityAddressList, apiSelector, notOkCustomerAddressList);
        tokenToRowDataListMap.put(token, rowDataList);
      }
    }

    return tokenToRowDataListMap;
  }

  /**
   * 
   */
  private List<String> createLiquidityAddressList() throws DfxException {
    LOGGER.trace("createLiquidityAddressList()");

    List<String> liquidityAddressList = new ArrayList<>();

    List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        liquidityAddressList.add(stakingAddressDTO.getLiquidityAddress());
      }
    }

    return liquidityAddressList;
  }

  /**
   * 
   */
  private List<String> createStakingNotOkCustomerAddressList(
      @Nonnull String rootPath,
      @Nonnull String fileName) throws DfxException {
    LOGGER.trace("createStakingNotOkCustomerAddressList()");

    File excelFile = new File(rootPath, fileName);

    try (ExcelReader excelReader = new ExcelReader(excelFile)) {
      String sheetName = TokenEnum.DFI.toString();
      List<Integer> cellNumberList = Arrays.asList(0, 4);

      List<CellValueMap> excelValueList = excelReader.read(sheetName, 2, cellNumberList);

      return excelValueList.stream()
          .filter(v -> !"OK".equals(v.get(4)))
          .map(v -> v.get(0).toString())
          .collect(Collectors.toList());
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("createStakingNotOkCustomerAddressList", e);
    }
  }

  /**
   * 
   */
  private Map<TokenEnum, List<String>> createYieldmachineNotOkCustomerAddressListMap(
      @Nonnull String rootPath,
      @Nonnull String fileName) throws DfxException {
    LOGGER.trace("createYieldmachineNotOkCustomerAddressListMap()");

    File excelFile = new File(rootPath, fileName);

    try (ExcelReader excelReader = new ExcelReader(excelFile)) {
      Map<TokenEnum, List<String>> yieldmachineNotOkCustomerAddressListMap = new HashMap<>();

      List<Integer> cellNumberList = Arrays.asList(0, 4);

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: activate SPY is used ...
        if (TokenEnum.SPY != token) {
          String sheetName = token.toString();

          List<CellValueMap> excelValueList = excelReader.read(sheetName, 2, cellNumberList);

          List<String> notOkCustomerAddressList =
              excelValueList.stream()
                  .filter(v -> !"OK".equals(v.get(4)))
                  .map(v -> v.get(0).toString())
                  .collect(Collectors.toList());

          yieldmachineNotOkCustomerAddressListMap.put(token, notOkCustomerAddressList);
        }
      }

      return yieldmachineNotOkCustomerAddressListMap;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("createYieldmachineNotOkCustomerAddressListMap", e);
    }
  }

  /**
   * 
   */
  private RowDataList createRowDataList(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull List<String> liquidityAddressList,
      @Nonnull APISelector apiSelector,
      @Nonnull List<String> notOkCustomerAddressList) throws DfxException {
    RowDataList rowDataList = new RowDataList(1);

    for (String customerAddress : notOkCustomerAddressList) {
      for (String liquidityAddress : liquidityAddressList) {
        fillRowDataList(connection, token, liquidityAddress, customerAddress, apiSelector, rowDataList);
      }
    }

    return rowDataList;
  }

  /**
   * 
   */
  private void fillRowDataList(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress,
      @Nonnull APISelector apiSelector,
      @Nonnull RowDataList rowDataList) throws DfxException {
    String depositAddress = getDepositAddress(token, customerAddress);

    CompareInfoDTO depositCompareInfoDTO;
    CompareInfoDTO withdrawalCompareInfoDTO;

    if (BalanceReporting.BalanceReportingTypeEnum.STAKING == reportingType) {
      depositCompareInfoDTO = apiTransactionHistoryProvider.checkStakingDeposit(connection, token, liquidityAddress, depositAddress, apiSelector);
      withdrawalCompareInfoDTO = apiTransactionHistoryProvider.checkStakingWithdrawal(connection, token, liquidityAddress, customerAddress, apiSelector);
    } else {
      depositCompareInfoDTO = apiTransactionHistoryProvider.checkYieldmachineDeposit(connection, token, liquidityAddress, depositAddress, apiSelector);
      withdrawalCompareInfoDTO = apiTransactionHistoryProvider.checkYieldmachineWithdrawal(connection, token, liquidityAddress, customerAddress, apiSelector);
    }

    addRowData(token, customerAddress, depositAddress, depositCompareInfoDTO, rowDataList);
    addRowData(token, customerAddress, depositAddress, withdrawalCompareInfoDTO, rowDataList);
  }

  /**
   * 
   */
  private String getDepositAddress(
      @Nonnull TokenEnum token,
      @Nonnull String customerAddress) throws DfxException {
    String depositAddress = null;

    // ...
    AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress(customerAddress);

    if (null != customerAddressDTO) {
      List<StakingDTO> stakingDTOListByCustomerAddressNumber =
          databaseBalanceHelper.getStakingDTOListByCustomerAddressNumber(token, customerAddressDTO.getNumber());
      depositAddress = stakingDTOListByCustomerAddressNumber.get(0).getDepositAddress();
    }

    if (null == depositAddress) {
      throw new DfxException("Deposit Address not found: Customer Address=" + customerAddress);
    }

    return depositAddress;
  }

  /**
   * 
   */
  private void addRowData(
      @Nonnull TokenEnum token,
      @Nonnull String customerAddress,
      @Nonnull String depositAddress,
      @Nonnull CompareInfoDTO compareInfoDTO,
      @Nonnull RowDataList rowDataList) {
    LOGGER.trace("addRowData()");

    List<APITransactionHistoryDTO> apiTransactionHistoryDTOList = compareInfoDTO.getApiTransactionHistoryDTOList();
    List<DBTransactionDTO> dbTransactionDTOList = compareInfoDTO.getDbTransactionDTOList();

    for (int i = 0; i < apiTransactionHistoryDTOList.size(); i++) {
      APITransactionHistoryDTO apiTransactionHistoryDTO = apiTransactionHistoryDTOList.get(i);
      DBTransactionDTO dbTransactionDTO = dbTransactionDTOList.get(i);

      String type = apiTransactionHistoryDTO.getType();
      String txId = apiTransactionHistoryDTO.getTxId();

      BigDecimal apiAmount = apiTransactionHistoryDTO.getInputAmount();

      if (null == apiAmount) {
        apiAmount = apiTransactionHistoryDTO.getOutputAmount();
      }

      BigDecimal dbAmount = dbTransactionDTO.getAmount();

      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(token.toString()));
      rowData.addCellData(new CellData().setValue(type));
      rowData.addCellData(new CellData().setValue(customerAddress));
      rowData.addCellData(new CellData().setValue(depositAddress));
      rowData.addCellData(new CellData().setValue(txId));
      rowData.addCellData(new CellData().setValue(apiAmount));
      rowData.addCellData(new CellData().setValue(dbAmount));

      rowDataList.add(rowData);
    }
  }

  /**
   * 
   */
  private void transactionReport(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheetName,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("transactionReport()");

    openExcel(rootPath, fileName, sheetName);

    cleanExcel(1);
    writeExcel(rowDataList, new CellDataList());
    closeExcel();
  }

  /**
   * 
   */
  private void yieldmachineReport(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheetName,
      @Nonnull Map<TokenEnum, RowDataList> tokenToRowDataListMap) throws DfxException {
    LOGGER.trace("yieldmachineReport()");

    RowDataList rowDataList = new RowDataList(1);

    for (TokenEnum token : TokenEnum.values()) {
      // TODO: activate SPY is used ...
      if (TokenEnum.SPY != token) {
        RowDataList tokenRowDataList = tokenToRowDataListMap.get(token);
        tokenRowDataList.forEach(rowData -> rowDataList.add(rowData));
      }
    }

    openExcel(rootPath, fileName, sheetName);

    cleanExcel(1);
    writeExcel(rowDataList, new CellDataList());
    closeExcel();
  }
}
