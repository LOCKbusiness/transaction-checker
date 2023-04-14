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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.reporting.data.YieldPoolSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAssetPriceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryInterimDifferenceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;

/**
 * 
 */
public class TransparencyReportCsvHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportCsvHelper.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final BigDecimal MINUTE_IN_MILLIES = new BigDecimal(60 * 1000);
  private static final BigDecimal DAY_IN_MILLIES = new BigDecimal(24 * 60 * 60 * 1000);
  private static final BigDecimal MINUTES_PER_DAY = new BigDecimal(1440);
  private static final BigDecimal YEAR_IN_DAYS = new BigDecimal(365);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final DecimalFormat US_DECIMAL_FORMAT = new DecimalFormat("##,##0.00000000");

  private static final String HISTORY_AMOUNT_CSV_FILENAME = "TransparencyReportHistoryAmount.csv";
  private static final String HISTORY_INTERIM_DIFFERENCE_CSV_FILENAME = "TransparencyReportHistoryInterimDifference.csv";
  private static final String HISTORY_ASSET_PRICE_CSV_FILENAME = "TransparencyReportHistoryAssetPrice.csv";
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
      @Nonnull HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO,
      @Nonnull HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO,
      @Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) throws DfxException {
    LOGGER.debug("writeToCSV()");

    writeHistoryAmountSheetDTOToCSV(historyAmountSheetDTO);
    writeHistoryInterimDifferenceSheetDTOToCSV(historyInterimDifferenceSheetDTO);
    writeHistoryAssetPriceSheetDTOToCSV(historyAssetPriceSheetDTO);
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
  private void writeHistoryInterimDifferenceSheetDTOToCSV(@Nonnull HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO) throws DfxException {
    LOGGER.debug("writeHistoryInterimDifferenceSheetDTOToCSV()");

    File historyInterimDifferenceFile = new File(DATA_PATH, HISTORY_INTERIM_DIFFERENCE_CSV_FILENAME);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyInterimDifferenceFile, true))) {
      writer.append(DATE_FORMAT.format(historyInterimDifferenceSheetDTO.getTimestamp()));

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: currently without EUROC, coming later ...
        if (TokenEnum.EUROC != token) {
          writer.append(";").append(US_DECIMAL_FORMAT.format(historyInterimDifferenceSheetDTO.getInterimDifference(token)));
        }
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeHistoryInterimDifferenceSheetDTOToCSV", e);
    }
  }

  /**
   * 
   */
  private void writeHistoryAssetPriceSheetDTOToCSV(@Nonnull HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO) throws DfxException {
    LOGGER.debug("writeHistoryAssetPriceSheetDTOToCSV()");

    File historyAssetPriceFile = new File(DATA_PATH, HISTORY_ASSET_PRICE_CSV_FILENAME);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyAssetPriceFile, true))) {
      writer.append(DATE_FORMAT.format(historyAssetPriceSheetDTO.getTimestamp()));
      writer.append(";").append(Long.toString(historyAssetPriceSheetDTO.getBlockNumber()));

      for (TokenEnum token : TokenEnum.values()) {
        // TODO: currently without EUROC, coming later ...
        if (TokenEnum.EUROC != token) {
          writer.append(";").append(US_DECIMAL_FORMAT.format(historyAssetPriceSheetDTO.getPrice(token)));
        }
      }

      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("writeHistoryAssetPriceSheetDTOToCSV", e);
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
  public List<HistoryAmountSheetDTO> readHistoryAmountSheetDTOFromCSV(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryAmountSheetDTOFromCSV()");

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyAmountFile))) {
      List<HistoryAmountSheetDTO> historyAmountSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        Timestamp timestamp = Timestamp.valueOf(entryArray[0]);

        if (historyHourSet.isEmpty()
            || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
          HistoryAmountSheetDTO historyAmountSheetDTO = new HistoryAmountSheetDTO(timestamp);

          int i = 1;

          for (TokenEnum token : TokenEnum.values()) {
            // TODO: currently without EUROC, coming later ...
            if (TokenEnum.EUROC != token) {
              historyAmountSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
            }
          }

          historyAmountSheetDTOList.add(historyAmountSheetDTO);
        }
      }

      return historyAmountSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryAmountSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryInterimDifferenceSheetDTO> readHistoryInterimDifferenceSheetDTOFromCSV(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryInterimDifferenceSheetDTOFromCSV()");

    File historyInterimDifferenceFile = new File(DATA_PATH, HISTORY_INTERIM_DIFFERENCE_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyInterimDifferenceFile))) {
      List<HistoryInterimDifferenceSheetDTO> historyInterimDifferenceSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        Timestamp timestamp = Timestamp.valueOf(entryArray[0]);

        if (historyHourSet.isEmpty()
            || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
          HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO = new HistoryInterimDifferenceSheetDTO(timestamp);

          int i = 1;

          for (TokenEnum token : TokenEnum.values()) {
            // TODO: currently without EUROC, coming later ...
            if (TokenEnum.EUROC != token) {
              historyInterimDifferenceSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
            }
          }

          historyInterimDifferenceSheetDTOList.add(historyInterimDifferenceSheetDTO);
        }
      }

      return historyInterimDifferenceSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryInterimDifferenceSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryAssetPriceSheetDTO> readHistoryAssetPriceSheetDTOFromCSV(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryAssetPriceSheetDTOFromCSV()");

    File historyAssetPriceFile = new File(DATA_PATH, HISTORY_ASSET_PRICE_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyAssetPriceFile))) {
      List<HistoryAssetPriceSheetDTO> historyAssetPriceSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        Timestamp timestamp = Timestamp.valueOf(entryArray[0]);

        if (historyHourSet.isEmpty()
            || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
          HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO = new HistoryAssetPriceSheetDTO(timestamp);
          historyAssetPriceSheetDTO.setBlockNumber(Long.parseLong(entryArray[1]));

          int i = 2;

          for (TokenEnum token : TokenEnum.values()) {
            // TODO: currently without EUROC, coming later ...
            if (TokenEnum.EUROC != token) {
              historyAssetPriceSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
            }
          }

          historyAssetPriceSheetDTOList.add(historyAssetPriceSheetDTO);
        }
      }

      return historyAssetPriceSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryAssetPriceSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<HistoryPriceSheetDTO> readHistoryPriceSheetDTOFromCSV(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryPriceSheetDTOFromCSV()");

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_CSV_FILENAME);

    try (BufferedReader reader = new BufferedReader(new FileReader(historyPriceFile))) {
      List<HistoryPriceSheetDTO> historyPriceSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        Timestamp timestamp = Timestamp.valueOf(entryArray[0]);

        if (historyHourSet.isEmpty()
            || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
          HistoryPriceSheetDTO historyPriceSheetDTO = new HistoryPriceSheetDTO(timestamp);

          int i = 1;

          for (TokenEnum token : TokenEnum.values()) {
            // TODO: currently without EUROC, coming later ...
            if (TokenEnum.EUROC != token) {
              historyPriceSheetDTO.put(token, BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
            }
          }

          historyPriceSheetDTOList.add(historyPriceSheetDTO);
        }
      }

      return historyPriceSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readHistoryPriceSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  public List<YieldPoolSheetDTO> readYieldPoolSheetDTOFromCSV(@Nonnull String poolTokenId) throws DfxException {
    LOGGER.debug("readYieldPoolSheetDTOFromCSV(): poolTokenId=" + poolTokenId);

    String csvFileName = new StringBuilder().append("Yield-").append(poolTokenId).append("-Pool.csv").toString();
    File yieldFile = new File(DATA_PATH, csvFileName);

    try (BufferedReader reader = new BufferedReader(new FileReader(yieldFile))) {
      List<YieldPoolSheetDTO> yieldPoolSheetDTOList = new ArrayList<>();

      // Skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");
        int i = 1;

        YieldPoolSheetDTO yieldPoolSheetDTO = new YieldPoolSheetDTO(poolTokenId, Timestamp.valueOf(entryArray[0]));

        yieldPoolSheetDTO.setTokenAAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenAPrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenBAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setTokenBPrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setBalanceAmount(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));
        yieldPoolSheetDTO.setBalancePrice(BigDecimal.valueOf(US_DECIMAL_FORMAT.parse(entryArray[i++]).doubleValue()));

        yieldPoolSheetDTOList.add(yieldPoolSheetDTO);
      }

      // ...
      fillHourInterval(yieldPoolSheetDTOList);
      fillDayInterval(yieldPoolSheetDTOList);

      return yieldPoolSheetDTOList;
    } catch (Exception e) {
      throw new DfxException("readYieldPoolSheetDTOFromCSV", e);
    }
  }

  /**
   * 
   */
  private void fillHourInterval(@Nonnull List<YieldPoolSheetDTO> yieldPoolSheetDTOList) {
    LOGGER.debug("fillHourInterval()");

    YieldPoolSheetDTO previousYieldPoolSheetDTO = yieldPoolSheetDTOList.get(0);

    for (int i = 1; i < yieldPoolSheetDTOList.size(); i++) {
      Timestamp previousTimestamp = previousYieldPoolSheetDTO.getTimestamp();
      BigDecimal previousBalancePrice = previousYieldPoolSheetDTO.getBalancePrice();

      YieldPoolSheetDTO currentYieldPoolSheetDTO = yieldPoolSheetDTOList.get(i);
      Timestamp currentTimestamp = currentYieldPoolSheetDTO.getTimestamp();
      BigDecimal currentBalancePrice = currentYieldPoolSheetDTO.getBalancePrice();

      BigDecimal timeDiff = new BigDecimal(currentTimestamp.getTime() - previousTimestamp.getTime());
      BigDecimal intervalInMinutes = timeDiff.divide(MINUTE_IN_MILLIES, MATH_CONTEXT);

      BigDecimal difference = currentBalancePrice.subtract(previousBalancePrice);
      BigDecimal yieldPerInterval = difference.divide(previousBalancePrice, MATH_CONTEXT);

      BigDecimal interval = MINUTES_PER_DAY.divide(intervalInMinutes, MATH_CONTEXT);
      BigDecimal yield =
          yieldPerInterval
              .multiply(YEAR_IN_DAYS, MATH_CONTEXT)
              .multiply(interval, MATH_CONTEXT);

      currentYieldPoolSheetDTO.setHourDifference(difference.setScale(SCALE, RoundingMode.HALF_UP));
      currentYieldPoolSheetDTO.setHourInterval(interval.setScale(SCALE, RoundingMode.HALF_UP));
      currentYieldPoolSheetDTO.setHourYield(yield.setScale(SCALE, RoundingMode.HALF_UP));

      previousYieldPoolSheetDTO = currentYieldPoolSheetDTO;
    }
  }

  /**
   * 
   */
  private void fillDayInterval(@Nonnull List<YieldPoolSheetDTO> yieldPoolSheetDTOList) {
    LOGGER.debug("fillDayInterval()");

    YieldPoolSheetDTO previousYieldPoolSheetDTO = yieldPoolSheetDTOList.get(0);

    for (int i = 1; i < yieldPoolSheetDTOList.size(); i++) {
      Timestamp previousTimestamp = previousYieldPoolSheetDTO.getTimestamp();
      LocalDate previousDay = previousTimestamp.toLocalDateTime().toLocalDate();
      BigDecimal previousBalancePrice = previousYieldPoolSheetDTO.getBalancePrice();

      YieldPoolSheetDTO currentYieldPoolSheetDTO = yieldPoolSheetDTOList.get(i);
      Timestamp currentTimestamp = currentYieldPoolSheetDTO.getTimestamp();
      LocalDate currentDay = currentTimestamp.toLocalDateTime().toLocalDate();

      if (0 == currentDay.compareTo(previousDay.plusDays(1))) {
        BigDecimal currentBalancePrice = currentYieldPoolSheetDTO.getBalancePrice();

        BigDecimal timeDiff = new BigDecimal(currentTimestamp.getTime() - previousTimestamp.getTime());
        BigDecimal intervalInDays = timeDiff.divide(DAY_IN_MILLIES, MATH_CONTEXT);

        BigDecimal difference = currentBalancePrice.subtract(previousBalancePrice);
        BigDecimal yieldPerInterval = difference.divide(previousBalancePrice, MATH_CONTEXT);

        BigDecimal interval = YEAR_IN_DAYS.divide(intervalInDays, MATH_CONTEXT);
        BigDecimal yield =
            yieldPerInterval
                .multiply(interval, MATH_CONTEXT);

        currentYieldPoolSheetDTO.setDayDifference(difference.setScale(SCALE, RoundingMode.HALF_UP));
        currentYieldPoolSheetDTO.setDayInterval(interval.setScale(SCALE, RoundingMode.HALF_UP));
        currentYieldPoolSheetDTO.setDayYield(yield.setScale(SCALE, RoundingMode.HALF_UP));

        previousYieldPoolSheetDTO = currentYieldPoolSheetDTO;
      }
    }
  }
}
