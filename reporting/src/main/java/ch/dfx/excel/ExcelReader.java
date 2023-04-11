package ch.dfx.excel;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellValueMap;

/**
 * 
 */
public class ExcelReader implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger(ExcelReader.class);

  // ...
  private static int SCALE = 8;

  // ...
  private Workbook workbook = null;

  /**
   * 
   */
  public ExcelReader(@Nonnull File excelFile) throws DfxException {
    open(excelFile);
  }

  /**
   * 
   */
  private void open(@Nonnull File excelFile) throws DfxException {
    LOGGER.trace("open()");

    try (FileInputStream inputStream = new FileInputStream(excelFile)) {
      workbook = WorkbookFactory.create(inputStream);
    } catch (Exception e) {
      throw new DfxException("open", e);
    }
  }

  @Override
  public void close() throws Exception {
    LOGGER.trace("close()");

    try {
      workbook.close();
    } catch (Exception e) {
      throw new DfxException("close", e);
    }
  }

  /**
   * 
   */
  public List<CellValueMap> read(
      @Nonnull String sheetName,
      int firstRowNumber,
      @Nonnull List<Integer> cellNumberList) throws DfxException {
    LOGGER.trace("read()");

    try {
      Sheet sheet = workbook.getSheet(sheetName);

      List<CellValueMap> excelValueList = new ArrayList<>();

      int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();

      for (int rowNum = firstRowNumber; rowNum < physicalNumberOfRows; rowNum++) {
        Row row = sheet.getRow(rowNum);

        CellValueMap valueMap = new CellValueMap();

        for (int cellNum : cellNumberList) {
          Cell cell = row.getCell(cellNum);
          fillValueMap(cellNum, cell, valueMap);
        }

        excelValueList.add(valueMap);
      }

      return excelValueList;
    } catch (Exception e) {
      throw new DfxException("read", e);
    }
  }

  /**
   * 
   */
  public void fillValueMap(
      @Nonnull Integer cellNum,
      @Nonnull Cell cell,
      @Nonnull CellValueMap valueMap) throws DfxException {
    LOGGER.trace("fillValueMap()");

    CellType cellType = cell.getCellType();

    if (CellType.STRING == cellType) {
      valueMap.put(cellNum, cell.getStringCellValue());
    } else if (CellType.NUMERIC == cellType) {
      if (DateUtil.isCellDateFormatted(cell)) {
        valueMap.put(cellNum, cell.getDateCellValue());
      } else {
        valueMap.put(cellNum, new BigDecimal(cell.getNumericCellValue()).setScale(SCALE, RoundingMode.HALF_UP));
      }
    } else {
      throw new DfxException("unknown cell type: " + cellType);
    }
  }
}
