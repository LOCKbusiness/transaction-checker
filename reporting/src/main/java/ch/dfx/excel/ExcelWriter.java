package ch.dfx.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.Date;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;

/**
 * 
 */
public class ExcelWriter {
  private static final Logger LOGGER = LogManager.getLogger(ExcelWriter.class);

  // ...
  private Workbook workbook = null;

  private Sheet sheet = null;

  private CellStyle boldCellStyle = null;
  private Font boldFont = null;

  private CellStyle decimalNumberCellStyle = null;
  private CellStyle numberCellStyle = null;

  /**
   * 
   */
  public ExcelWriter() {
  }

  /**
   * 
   */
  public void openWorkbook(
      @Nonnull File excelFile,
      @Nonnull String sheetName) throws DfxException {
    LOGGER.trace("openWorkbook()");

    try (FileInputStream inputStream = new FileInputStream(excelFile)) {
      workbook = WorkbookFactory.create(inputStream);
      sheet = workbook.getSheet(sheetName);

      initializeStyles();
      cleanSheet();
    } catch (Exception e) {
      throw new DfxException("openWorkbook", e);
    }
  }

  /**
   * 
   */
  private void initializeStyles() {
    LOGGER.trace("initializeStyles()");

    // ...
    boldCellStyle = workbook.createCellStyle();
    boldFont = workbook.createFont();
    boldFont.setBold(true);
    boldCellStyle.setFont(boldFont);

    // ...
    DataFormat dataFormat = workbook.createDataFormat();
    decimalNumberCellStyle = workbook.createCellStyle();
    decimalNumberCellStyle.setDataFormat(dataFormat.getFormat("#,##0.00000000"));

    numberCellStyle = workbook.createCellStyle();
    numberCellStyle.setDataFormat(dataFormat.getFormat("#0"));
  }

  /**
   * 
   */
  private void cleanSheet() {
    LOGGER.trace("cleanSheet()");

    for (int rowNum = sheet.getPhysicalNumberOfRows(); rowNum > 1; rowNum--) {
      Row row = sheet.getRow(rowNum);

      if (null != row) {
        sheet.removeRow(row);
      }
    }

    // ...
    Row firstRow = sheet.getRow(0);

    // B1: Timestamp
    Cell timestampCell = firstRow.getCell(1);

    if (null != timestampCell) {
      timestampCell.setCellValue(new Date());
    }

    // C1: Total
    Cell totalCell = firstRow.getCell(2);

    if (null != totalCell) {
      totalCell.setCellValue(0);
    }
  }

  /**
   * 
   */
  public void writeWorkbook(@Nonnull File excelFile) throws DfxException {
    LOGGER.trace("writeWorkbook()");

    try (FileOutputStream outputStream = new FileOutputStream(excelFile)) {
      workbook.write(outputStream);
      workbook.close();
    } catch (Exception e) {
      throw new DfxException("writeWorkbook", e);
    }
  }

  /**
   * 
   */
  public void insertRowData(@Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("insertRowData()");

    for (int rowIndex = 0; rowIndex < rowDataList.size(); rowIndex++) {
      Row row = sheet.createRow(rowIndex + 2);
      row.setHeightInPoints(16f);

      RowData rowData = rowDataList.get(rowIndex);
      insertCellData(row, rowData);
    }
  }

  /**
   * 
   */
  public void insertCellData(@Nonnull CellDataList cellDataList) throws DfxException {
    LOGGER.trace("insertCellData()");

    for (CellData cellData : cellDataList) {
      int rowIndex = cellData.getRowIndex();
      int cellIndex = cellData.getCellIndex();

      if (-1 != rowIndex
          && -1 != cellIndex) {
        Cell cell = getOrCreateCell(rowIndex, cellIndex);
        setCellValue(cell, cellData);
      }
    }
  }

  /**
   * 
   */
  private Row getOrCreateRow(int rowIndex) {
    LOGGER.trace("getOrCreateRow()");

    Row row = sheet.getRow(rowIndex);

    if (null == row) {
      row = sheet.createRow(rowIndex);
    }

    return row;
  }

  /**
   * 
   */
  private Cell getOrCreateCell(int rowIndex, int cellIndex) {
    LOGGER.trace("getOrCreateCell()");

    Row row = getOrCreateRow(rowIndex);

    Cell cell = row.getCell(cellIndex);

    if (null == cell) {
      cell = row.createCell(cellIndex);
    }

    return cell;
  }

  /**
   * 
   */
  private void insertCellData(@Nonnull Row row, @Nonnull RowData rowData) throws DfxException {
    LOGGER.trace("insertCellData()");

    CellDataList cellDataList = rowData.getCellDataList();

    for (int cellIndex = 0; cellIndex < cellDataList.size(); cellIndex++) {
      Cell cell = row.createCell(cellIndex);
      CellData cellData = cellDataList.get(cellIndex);

      setCellValue(cell, cellData);
    }
  }

  /**
   * 
   */
  private void setCellValue(
      @Nonnull Cell cell,
      @Nonnull CellData cellData) throws DfxException {
    LOGGER.trace("setCellValue()");

    Object value = cellData.getValue();

    if (cellData.isBold()) {
      cell.setCellStyle(boldCellStyle);
    }

    if (null == value) {
      cell.setCellValue((String) null);
    } else {
      Class<? extends Object> valueClass = value.getClass();

      if (String.class == valueClass) {
        cell.setCellValue((String) value);
      } else if (BigDecimal.class == valueClass) {
        cell.setCellStyle(decimalNumberCellStyle);
        cell.setCellValue(((BigDecimal) value).doubleValue());
      } else if (Integer.class == valueClass) {
        cell.setCellStyle(numberCellStyle);
        cell.setCellValue(((Integer) value).doubleValue());
      } else if (Long.class == valueClass) {
        cell.setCellStyle(numberCellStyle);
        cell.setCellValue(((Long) value).doubleValue());
      } else {
        throw new DfxException("unknown value class " + valueClass.getSimpleName());
      }
    }
  }
}
