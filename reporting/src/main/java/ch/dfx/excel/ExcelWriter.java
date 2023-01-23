package ch.dfx.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.util.ZipSecureFile;
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
  private CellStyle dateCellStyle = null;
  private CellStyle timestampCellStyle = null;

  /**
   * 
   */
  public ExcelWriter() {
    ZipSecureFile.setMinInflateRatio(0);
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

    dateCellStyle = workbook.createCellStyle();
    dateCellStyle.setDataFormat(dataFormat.getFormat("YYYY-MM-DD"));

    timestampCellStyle = workbook.createCellStyle();
    timestampCellStyle.setDataFormat(dataFormat.getFormat("YYYY-MM-DD HH:MM:SS"));
  }

  /**
   * 
   */
  public void cleanSheet(@Nonnull CellDataList cellDataList) throws DfxException {
    LOGGER.trace("cleanSheet()");

    insertCellData(cellDataList);
  }

  /**
   * 
   */
  public void cleanSheet(int afterRowNumber) {
    LOGGER.trace("cleanSheet()");

    for (int rowNum = sheet.getPhysicalNumberOfRows(); rowNum >= afterRowNumber; rowNum--) {
      Row row = sheet.getRow(rowNum);

      if (null != row) {
        sheet.removeRow(row);
      }
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

    int rowOffset = rowDataList.getRowOffset();

    for (int rowIndex = 0; rowIndex < rowDataList.size(); rowIndex++) {
      Row row = sheet.createRow(rowIndex + rowOffset);
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

    if (!cellData.isKeepStyle()
        && cellData.isBold()) {
      cell.setCellStyle(boldCellStyle);
    }

    if (null == value) {
      cell.setCellValue((String) null);
    } else {
      Class<? extends Object> valueClass = value.getClass();

      if (String.class == valueClass) {
        cell.setCellValue((String) value);
      } else if (BigDecimal.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(decimalNumberCellStyle);
        }

        cell.setCellValue(((BigDecimal) value).doubleValue());
      } else if (Integer.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(numberCellStyle);
        }

        cell.setCellValue(((Integer) value).doubleValue());
      } else if (Long.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(numberCellStyle);
        }

        cell.setCellValue(((Long) value).doubleValue());
      } else if (java.util.Date.class == valueClass
          || java.sql.Date.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(dateCellStyle);
        }

        cell.setCellValue((java.util.Date) value);
      } else if (LocalDate.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(dateCellStyle);
        }

        cell.setCellValue((LocalDate) value);
      } else if (java.sql.Timestamp.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(timestampCellStyle);
        }

        cell.setCellValue((java.util.Date) value);

      } else if (LocalDateTime.class == valueClass) {
        if (!cellData.isKeepStyle()) {
          cell.setCellStyle(timestampCellStyle);
        }

        cell.setCellValue((LocalDateTime) value);
      } else {
        throw new DfxException("unknown value class " + valueClass.getSimpleName());
      }
    }
  }
}
