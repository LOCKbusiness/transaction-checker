package ch.dfx.reporting;

import java.io.File;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.ExcelWriter;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public abstract class Reporting {
  private static final Logger LOGGER = LogManager.getLogger(Reporting.class);

  protected final H2DBManager databaseManager;
  protected final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public Reporting(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;

    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  protected void writeExcel(
      @Nonnull String reportingRootPath,
      @Nonnull String reportingFileName,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("writeExcel()");

    try {
      LOGGER.debug("Reporting: " + reportingFileName);

      // ...
      File excelStakingBalanceFile = new File(reportingRootPath, reportingFileName);

      ExcelWriter excelWriter = new ExcelWriter();
      excelWriter.openWorkbook(excelStakingBalanceFile);

      excelWriter.insertRowData(rowDataList);

      // ...
      excelWriter.writeWorkbook(excelStakingBalanceFile);
    } catch (Exception e) {
      throw new DfxException("writeExcel", e);
    }
  }
}
