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
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public abstract class Reporting {
  private static final Logger LOGGER = LogManager.getLogger(Reporting.class);

  protected final NetworkEnum network;

  protected final H2DBManager databaseManager;

  protected final DatabaseBlockHelper databaseBlockHelper;
  protected final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public Reporting(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
  }

  /**
   * 
   */
  protected void writeExcel(
      @Nonnull String reportingRootPath,
      @Nonnull String reportingFileName,
      @Nonnull String reportingSheetName,
      @Nonnull RowDataList rowDataList,
      @Nonnull CellDataList specialCellDataList) throws DfxException {
    LOGGER.trace("writeExcel()");

    try {
      LOGGER.debug("Reporting: " + reportingFileName);

      // ...
      File reportingFile = new File(reportingRootPath, reportingFileName);

      ExcelWriter excelWriter = new ExcelWriter();
      excelWriter.openWorkbook(reportingFile, reportingSheetName);

      excelWriter.insertRowData(rowDataList);
      excelWriter.insertCellData(specialCellDataList);

      // ...
      excelWriter.writeWorkbook(reportingFile);
    } catch (Exception e) {
      throw new DfxException("writeExcel", e);
    }
  }
}
