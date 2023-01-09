package ch.dfx.statistik;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ch.dfx.reporting.Reporting;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StatistikReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(StatistikReporting.class);

  private final StatistikProvider statistikProvider;

  /**
   * 
   */
  public StatistikReporting(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    super(network, databaseManager);

    this.statistikProvider = new StatistikProvider(network, databaseManager);
  }

//  /**
//   * 
//   */
//  public void report(
//      @Nonnull Map<LocalDate, Integer> dateToCountMap,
//      @Nonnull String rootPath,
//      @Nonnull String fileName) throws DfxException {
//    LOGGER.debug("report()");
//
//    File csvDataFile = new File(rootPath, fileName);
//
//    try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvDataFile))) {
//      writer.append("Date,per Day,Total").append("\n");
//
//      int totalCount = 0;
//
//      List<LocalDate> sortedLocalDateList = new ArrayList<>(dateToCountMap.keySet());
//      sortedLocalDateList.sort((d1, d2) -> d1.compareTo(d2));
//
//      for (LocalDate localDate : sortedLocalDateList) {
//        Integer count = dateToCountMap.get(localDate);
//        totalCount = totalCount + count;
//
//        StringBuilder csvDataBuilder =
//            new StringBuilder()
//                .append(localDate.toString())
//                .append(",").append(count)
//                .append(",").append(totalCount)
//                .append("\n");
//
//        writer.append(csvDataBuilder);
//      }
//    } catch (Exception e) {
//      throw new DfxException("report", e);
//    }
//  }

  /**
   * 
   */
  public void report(
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    LOGGER.debug("report()");

    Objects.requireNonNull(token, "null 'token' not allowed");
    Objects.requireNonNull(rootPath, "null 'rootPath' not allowed");
    Objects.requireNonNull(fileName, "null 'fileName' not allowed");
    Objects.requireNonNull(sheet, "null 'sheet' not allowed");

    long startTime = System.currentTimeMillis();

    try {
      Map<LocalDate, Integer> dateToCountMap = new HashMap<>();
      Map<LocalDate, BigDecimal> dateToSumVinMap = new HashMap<>();
      Map<LocalDate, BigDecimal> dateToSumVoutMap = new HashMap<>();

      statistikProvider.fillDepositStatistikData(token, dateToCountMap, dateToSumVinMap, dateToSumVoutMap);

      // ...
      int totalCount = 0;
      BigDecimal totalSumVin = BigDecimal.ZERO;
      BigDecimal totalSumVout = BigDecimal.ZERO;
      BigDecimal totalBalance = BigDecimal.ZERO;

      List<LocalDate> sortedLocalDateList = new ArrayList<>(dateToCountMap.keySet());
      sortedLocalDateList.sort((d1, d2) -> d1.compareTo(d2));

      RowDataList rowDataList = new RowDataList(1);
      CellDataList cellDataList = new CellDataList();

      for (LocalDate localDate : sortedLocalDateList) {
        Integer count = dateToCountMap.get(localDate);
        BigDecimal sumVin = dateToSumVinMap.get(localDate);
        BigDecimal sumVout = dateToSumVoutMap.get(localDate);
        BigDecimal balance = sumVin.subtract(sumVout);

        totalCount = totalCount + count;
        totalSumVin = totalSumVin.add(sumVin);
        totalSumVout = totalSumVout.add(sumVout);
        totalBalance = totalBalance.add(balance);

        RowData rowData = new RowData();
        rowData.addCellData(new CellData().setValue(localDate));
        rowData.addCellData(new CellData().setValue(count));
        rowData.addCellData(new CellData().setValue(totalCount));

        rowData.addCellData(new CellData().setValue(sumVin));
        rowData.addCellData(new CellData().setValue(totalSumVin));
        rowData.addCellData(new CellData().setValue(sumVout));
        rowData.addCellData(new CellData().setValue(totalSumVout));
        rowData.addCellData(new CellData().setValue(balance));
        rowData.addCellData(new CellData().setValue(totalBalance));

        rowDataList.add(rowData);
      }

      // ...
      openExcel(rootPath, fileName, sheet);

      cleanExcel(1);

      writeExcel(rowDataList, cellDataList);
      closeExcel();
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }
}
