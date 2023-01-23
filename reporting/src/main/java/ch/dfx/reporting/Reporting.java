package ch.dfx.reporting;

import java.io.File;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.ExcelWriter;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public abstract class Reporting {
  private static final Logger LOGGER = LogManager.getLogger(Reporting.class);

  protected final NetworkEnum network;

  protected final DatabaseBlockHelper databaseBlockHelper;
  protected final DatabaseBalanceHelper databaseBalanceHelper;

  private File excelFile = null;
  private ExcelWriter excelWriter = null;

  /**
   * 
   */
  public Reporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  protected void openExcel(
      @Nonnull String reportingRootPath,
      @Nonnull String reportingFileName,
      @Nonnull String reportingSheetName) throws DfxException {
    LOGGER.trace("openExcel()");

    // ...
    excelFile = new File(reportingRootPath, reportingFileName);

    excelWriter = new ExcelWriter();
    excelWriter.openWorkbook(excelFile, reportingSheetName);
  }

  /**
   * 
   */
  protected void cleanExcel(@Nonnull CellDataList cellDataList) throws DfxException {
    LOGGER.trace("cleanExcel()");

    if (null != excelWriter) {
      excelWriter.cleanSheet(cellDataList);
    }
  }

  /**
   * 
   */
  protected void cleanExcel(int afterRowNumber) {
    LOGGER.trace("cleanExcel()");

    if (null != excelWriter) {
      excelWriter.cleanSheet(afterRowNumber);
    }
  }

  /**
   * 
   */
  protected void writeExcel(
      @Nonnull RowDataList rowDataList,
      @Nonnull CellDataList specialCellDataList) throws DfxException {
    LOGGER.trace("writeExcel()");

    LOGGER.debug("Reporting: " + excelFile.getName());

    if (null != excelWriter) {
      excelWriter.insertRowData(rowDataList);
      excelWriter.insertCellData(specialCellDataList);
    }
  }

  /**
   * 
   */
  protected void closeExcel() throws DfxException {
    LOGGER.trace("closeExcel()");

    if (null != excelWriter) {
      excelWriter.writeWorkbook(excelFile);
    }
  }
}
