package ch.dfx.reporting.transparency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.reporting.data.YieldPoolSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;

/**
 * 
 */
public class TransparencyReportCsvHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportCsvHelper.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static BigDecimal MINUTES_PER_DAY = new BigDecimal(1440);
  private static BigDecimal YEAR_IN_DAYS = new BigDecimal(365);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final DecimalFormat US_DECIMAL_FORMAT = new DecimalFormat("##,##0.00000000");

  private static final String HISTORY_AMOUNT_CSV_FILENAME = "TransparencyReportHistoryAmount.csv";
  private static final String HISTORY_PRICE_CSV_FILENAME = "TransparencyReportHistoryPrice.csv";

  private static final File DATA_PATH = Path.of("data", "yieldmachine").toFile();

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

    writeHistoryAmountSheetDTOToCSV(historyAmountSheetDTO);
    writeHistoryPriceSheetDTOToCSV(historyPriceSheetDTO);
  }

  /**
   * 
   */
  private void writeHistoryAmountSheetDTOToCSV(@Nonnull HistoryAmountSheetDTO historyAmountSheetDTO) throws DfxException {
    LOGGER.debug("writeHistoryAmountSheetDTOToCSV()");

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_CSV_FILENAME);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyAmountFile, true))) {
      writer.append(DATE_FORMAT.format(historyAmountSheetDTO.getTimestamp()));

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: currently without EUROC, coming later ...
        if (TokenEnum.EUROC != token) {
          writer.append(";").append(US_DECIMAL_FORMAT.format(historyAmountSheetDTO.getAmount(token)));
        }
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeHistoryAmountSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  private void writeHistoryPriceSheetDTOToCSV(@Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) throws DfxException {
    LOGGER.debug("writeHistoryPriceSheetDTOToCSV()");

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_CSV_FILENAME);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyPriceFile, true))) {
      writer.append(DATE_FORMAT.format(historyPriceSheetDTO.getTimestamp()));

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: currently without EUROC, coming later ...
        if (TokenEnum.EUROC != token) {
          writer.append(";").append(US_DECIMAL_FORMAT.format(historyPriceSheetDTO.getPrice(token)));
        }
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeHistoryPriceSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  public void writeToCSV(
      @Nonnull YieldPoolSheetDTO yieldPoolSheetDTO) throws DfxException {
    LOGGER.debug("writeToCSV()");

    writeYieldPoolSheetDTOToCSV(yieldPoolSheetDTO);
  }

  /**
   * 
   */
  private void writeYieldPoolSheetDTOToCSV(@Nonnull YieldPoolSheetDTO yieldPoolSheetDTO) throws DfxException {
    LOGGER.debug("writeYieldPoolSheetDTOToCSV()");

    String type = yieldPoolSheetDTO.getType();
    String csvFileName = new StringBuilder().append("Yield-").append(type).append("-Pool.csv").toString();
    File yieldFile = new File(DATA_PATH, csvFileName);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(yieldFile, true))) {
      writer.append(DATE_FORMAT.format(yieldPoolSheetDTO.getTimestamp()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getTokenAAmount()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getTokenAPrice()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getTokenBAmount()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getTokenBPrice()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getBalanceAmount()));
      writer.append(";").append(US_DECIMAL_FORMAT.format(yieldPoolSheetDTO.getBalancePrice()));
      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeYieldPoolSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryAmountSheetDTO> readHistoryAmountSheetDTOFromCSV() throws DfxException {
    LOGGER.debug("readHistoryAmountSheetDTOFromCSV()");

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyAmountFile))) {
      List<HistoryAmountSheetDTO> historyAmountSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        HistoryAmountSheetDTO historyAmountSheetDTO = new HistoryAmountSheetDTO(Timestamp.valueOf(entryArray[0]));

        for (TokenEnum token : TokenEnum.values()) {
          // TODO: currently without EUROC, coming later ...
          if (TokenEnum.EUROC != token) {
            historyAmountSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
          }
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

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyPriceFile))) {
      List<HistoryPriceSheetDTO> historyPriceSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        HistoryPriceSheetDTO historyPriceSheetDTO = new HistoryPriceSheetDTO(Timestamp.valueOf(entryArray[0]));

        for (TokenEnum token : TokenEnum.values()) {
          // TODO: currently without EUROC, coming later ...
          if (TokenEnum.EUROC != token) {
            historyPriceSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
          }
        }

        historyPriceSheetDTOList.add(historyPriceSheetDTO);
      }

      return historyPriceSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryPriceSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<YieldPoolSheetDTO> readYieldPoolSheetDTOFromCSV() throws DfxException {
    LOGGER.debug("readYieldPoolSheetDTOFromCSV()");

    String csvFileName = new StringBuilder().append("Yield-").append("USDT-DUSD").append("-Pool.csv").toString();
    File yieldFile = new File(DATA_PATH, csvFileName);

    try (BufferedReader reader = new BufferedReader(new FileReader(yieldFile))) {
      List<YieldPoolSheetDTO> yieldPoolSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        YieldPoolSheetDTO yieldPoolSheetDTO = new YieldPoolSheetDTO("USDT-DUSD", Timestamp.valueOf(entryArray[0]));

        yieldPoolSheetDTO.setTokenAAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenAPrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenBAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenBPrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setBalanceAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setBalancePrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));

        yieldPoolSheetDTOList.add(yieldPoolSheetDTO);
      }

      // ...
      YieldPoolSheetDTO previousYieldPoolSheetDTO = yieldPoolSheetDTOList.get(0);

      for (int i = 1; i < yieldPoolSheetDTOList.size(); i++) {
        Timestamp previusTimestamp = previousYieldPoolSheetDTO.getTimestamp();
        BigDecimal previousBalancePrice = previousYieldPoolSheetDTO.getBalancePrice();

        YieldPoolSheetDTO currentYieldPoolSheetDTO = yieldPoolSheetDTOList.get(i);
        Timestamp currentTimestamp = currentYieldPoolSheetDTO.getTimestamp();
        BigDecimal currentBalancePrice = currentYieldPoolSheetDTO.getBalancePrice();

        BigDecimal intervalInMinutes = new BigDecimal(Duration.between(previusTimestamp.toLocalDateTime(), currentTimestamp.toLocalDateTime()).toMinutes());
        BigDecimal difference = currentBalancePrice.subtract(previousBalancePrice);
        BigDecimal yieldPerInterval = difference.divide(previousBalancePrice, MATH_CONTEXT);

        BigDecimal interval = MINUTES_PER_DAY.divide(intervalInMinutes, MATH_CONTEXT);
        BigDecimal yield = yieldPerInterval
            .multiply(YEAR_IN_DAYS, MATH_CONTEXT)
            .multiply(interval, MATH_CONTEXT);

        currentYieldPoolSheetDTO.setDifference(difference.setScale(SCALE, RoundingMode.HALF_UP));
        currentYieldPoolSheetDTO.setInterval(interval.setScale(SCALE, RoundingMode.HALF_UP));
        currentYieldPoolSheetDTO.setYield(yield.setScale(SCALE, RoundingMode.HALF_UP));

        previousYieldPoolSheetDTO = currentYieldPoolSheetDTO;
      }

      return yieldPoolSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readYieldPoolSheetDTOFromCSV", e);
    }
  }
}
