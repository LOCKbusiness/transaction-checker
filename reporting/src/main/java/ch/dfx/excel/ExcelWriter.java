package ch.dfx.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

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
  // ...
  private static final SimpleDateFormat SHEETNAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

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
  public void openWorkbook(@Nonnull File excelFile) throws DfxException {
    try (FileInputStream inputStream = new FileInputStream(excelFile)) {
      workbook = WorkbookFactory.create(inputStream);
      sheet = workbook.getSheetAt(0);

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
    if (null != sheet) {
      for (int rowNum = sheet.getPhysicalNumberOfRows(); rowNum > 0; rowNum--) {
        Row row = sheet.getRow(rowNum);

        if (null != row) {
          sheet.removeRow(row);
        }
      }
    }

    workbook.setSheetName(0, SHEETNAME_DATE_FORMAT.format(new Date()));
  }

  /**
   * 
   */
  public void writeWorkbook(@Nonnull File excelFile) throws DfxException {
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
    for (int rowNumber = 0; rowNumber < rowDataList.size(); rowNumber++) {
      Row row = sheet.createRow(rowNumber + 1);
      row.setHeightInPoints(16f);

      RowData rowData = rowDataList.get(rowNumber);
      insertCellData(row, rowData);
    }
  }

  /**
   * 
   */
  private void insertCellData(@Nonnull Row row, @Nonnull RowData rowData) throws DfxException {
    CellDataList cellDataList = rowData.getCellDataList();

    for (int cellNumber = 0; cellNumber < cellDataList.size(); cellNumber++) {
      Cell cell = row.createCell(cellNumber);
      CellData cellData = cellDataList.get(cellNumber);

      setCellValue(cell, cellData);
    }
  }

  /**
   * 
   */
  private void setCellValue(
      @Nonnull Cell cell,
      @Nonnull CellData cellData) throws DfxException {
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
