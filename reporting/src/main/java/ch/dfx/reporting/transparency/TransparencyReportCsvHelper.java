package ch.dfx.reporting.transparency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;

/**
 * 
 */
public class TransparencyReportCsvHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportCsvHelper.class);

  // ...
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final DecimalFormat US_DECIMAL_FORMAT = new DecimalFormat("##,##0.00000000");

  private static final String HISTORY_AMOUNT_CSV_FILENAME = "TransparencyReportHistoryAmount.csv";
  private static final String HISTORY_PRICE_CSV_FILENAME = "TransparencyReportHistoryPrice.csv";

  private static final File HISTORY_AMOUNT_FILE = Path.of("data", "yieldmachine", HISTORY_AMOUNT_CSV_FILENAME).toFile();
  private static final File HISTORY_PRICE_FILE = Path.of("data", "yieldmachine", HISTORY_PRICE_CSV_FILENAME).toFile();

  /**
   * 
   */
  public TransparencyReportCsvHelper() {
  }

  /**
   * 
   */
  public void writeToCSV(
      @Nonnull HistoryAmountSheetDTO historyAmountSheetDTO,
      @Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) throws DfxException {
    LOGGER.debug("writeToCSV()");

    writHistoryAmountSheetDTOToCSV(historyAmountSheetDTO);
    writeHistoryPriceSheetDTOToCSV(historyPriceSheetDTO);
  }

  /**
   * 
   */
  private void writHistoryAmountSheetDTOToCSV(@Nonnull HistoryAmountSheetDTO historyAmountSheetDTO) throws DfxException {
    LOGGER.debug("writHistoryAmountSheetDTOToCSV()");

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_AMOUNT_FILE, true))) {
      writer.append(DATE_FORMAT.format(historyAmountSheetDTO.getTimestamp()));

      for (TokenEnum token : TokenEnum.values()) {
        writer.append(";").append(US_DECIMAL_FORMAT.format(historyAmountSheetDTO.getAmount(token)));
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writHistoryAmountSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  private void writeHistoryPriceSheetDTOToCSV(@Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) throws DfxException {
    LOGGER.debug("writeHistoryPriceSheetDTOToCSV()");

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_PRICE_FILE, true))) {
      writer.append(DATE_FORMAT.format(historyPriceSheetDTO.getTimestamp()));

      for (TokenEnum token : TokenEnum.values()) {
        writer.append(";").append(US_DECIMAL_FORMAT.format(historyPriceSheetDTO.getPrice(token)));
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeHistoryPriceSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryAmountSheetDTO> readHistoryAmountSheetDTOFromCSV() throws DfxException {
    LOGGER.debug("readHistoryAmountSheetDTOFromCSV()");

    try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_AMOUNT_FILE))) {
      List<HistoryAmountSheetDTO> historyAmountSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        HistoryAmountSheetDTO historyAmountSheetDTO = new HistoryAmountSheetDTO(Timestamp.valueOf(entryArray[0]));

        for (TokenEnum token : TokenEnum.values()) {
          historyAmountSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        }

        historyAmountSheetDTOList.add(historyAmountSheetDTO);
      }

      return historyAmountSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryAmountSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryPriceSheetDTO> readHistoryPriceSheetDTOFromCSV() throws DfxException {
    LOGGER.debug("readHistoryPriceSheetDTOFromCSV()");

    try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_PRICE_FILE))) {
      List<HistoryPriceSheetDTO> historyPriceSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        HistoryPriceSheetDTO historyPriceSheetDTO = new HistoryPriceSheetDTO(Timestamp.valueOf(entryArray[0]));

        for (TokenEnum token : TokenEnum.values()) {
          historyPriceSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        }

        historyPriceSheetDTOList.add(historyPriceSheetDTO);
      }

      return historyPriceSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryPriceSheetDTOFromCSV", e);
    }
  }

}
