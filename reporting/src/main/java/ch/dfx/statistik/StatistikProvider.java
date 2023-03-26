package ch.dfx.statistik;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.builder.DepositBuilder;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public abstract class StatistikProvider extends DepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StatistikProvider.class);

  // ...
  private static final String CSV_STATISTIK_FILE_NAME = "Statistik.csv";

  // ...
  protected final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StatistikProvider(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBalanceHelper);

    this.network = network;

    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void fillDepositStatistikData(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull Map<LocalDate, Integer> dateToCountMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.trace("fillDepositStatistikData()");

    try {
      fillStatistikFromCSV(dateToCountMap, dateToSumVinMap, dateToSumVoutMap);

      // ...
      openStatements(connection);

      // ...
      List<StakingAddressDTO> stakingAddressDTOList =
          databaseBalanceHelper.getStakingAddressDTOList()
              .stream().filter(dto -> -1 == dto.getRewardAddressNumber()).collect(Collectors.toList());

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByNumber(stakingAddressDTO.getStartBlockNumber());

        if (null != blockDTO) {
          int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();
          LocalDate startDate = new Date(blockDTO.getTimestamp() * 1000).toLocalDate();
          fillDateToCountMap(token, startDate, dateToCountMap);

          fillDateToVinMap(token, liquidityAddressNumber, startDate, dateToSumVinMap);
          fillDateToVoutMap(token, liquidityAddressNumber, startDate, dateToSumVoutMap);
        }
      }

      // ...
      closeStatements();

      // ...
      writeStatistikToCSV(dateToCountMap, dateToSumVinMap, dateToSumVoutMap);
    } catch (Exception e) {
      throw new DfxException("fillDepositStatistikData", e);
    }
  }

  public abstract void openStatements(@Nonnull Connection connection) throws DfxException;

  public abstract void closeStatements() throws DfxException;

  public abstract File getDataPath();

  /**
   * 
   */
  private File getStatistikFile() {
    return new File(getDataPath(), CSV_STATISTIK_FILE_NAME);
  }

  /**
   * 
   */
  private void fillStatistikFromCSV(
      @Nonnull Map<LocalDate, Integer> dateToCountMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.debug("fillStatistikFromCSV");

    File statistikFile = getStatistikFile();

    try (BufferedReader reader = new BufferedReader(new FileReader(statistikFile))) {
      // skip header ...
      reader.readLine();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        LocalDate day = LocalDate.parse(entryArray[0]);
        dateToCountMap.put(day, Integer.valueOf(entryArray[1]));
        dateToSumVinMap.put(day, new BigDecimal(entryArray[2]));
        dateToSumVoutMap.put(day, new BigDecimal(entryArray[3]));
      }
    } catch (Exception e) {
      throw new DfxException("fillStatistikFromCSV", e);
    }
  }

  /**
   * 
   */
  private void writeStatistikToCSV(
      @Nonnull Map<LocalDate, Integer> dateToCountMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.debug("writeStatistikToCSV");

    File statistikFile = getStatistikFile();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(statistikFile))) {
      writer.append("\"DAY\";\"COUNT\";\"SUM_VIN\";\"SUM_VOUT\"");
      writer.append("\n");

      List<LocalDate> allDayList = new ArrayList<>(dateToSumVinMap.keySet());
      allDayList.sort((d1, d2) -> d1.compareTo(d2));

      for (LocalDate day : allDayList) {
        Integer count = dateToCountMap.get(day);
        BigDecimal sumVin = dateToSumVinMap.get(day);
        BigDecimal sumVout = dateToSumVoutMap.get(day);

        writer.append(day.toString());
        writer.append(";").append(count.toString());
        writer.append(";").append(sumVin.toString());
        writer.append(";").append(sumVout.toString());
        writer.append("\n");
      }
    } catch (Exception e) {
      throw new DfxException("writeStatistikToCSV", e);
    }
  }

  /**
   * 
   */
  private void fillDateToCountMap(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, Integer> dateToCountMap) throws DfxException {
    LOGGER.trace("fillDateToCountMap()");

    // ...
    LocalDate workDate;

    if (dateToCountMap.isEmpty()) {
      workDate = startDate;
    } else {
      List<LocalDate> dateList = new ArrayList<>(dateToCountMap.keySet());
      dateList.sort((d1, d2) -> d2.compareTo(d1));
      workDate = dateList.get(0);
    }

    // ...
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      int count = getCount(token, workDate);

      dateToCountMap.put(workDate, count);

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  public abstract int getCount(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate workDate) throws DfxException;

  /**
   * 
   */
  private void fillDateToVinMap(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap) throws DfxException {
    LOGGER.trace("fillDateToVinMap()");

    // ...
    LocalDate workDate;

    if (dateToSumVinMap.isEmpty()) {
      workDate = startDate;
    } else {
      List<LocalDate> dateList = new ArrayList<>(dateToSumVinMap.keySet());
      dateList.sort((d1, d2) -> d2.compareTo(d1));
      workDate = dateList.get(0);
    }

    // ...
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      BigDecimal sumVin = getSumVin(token, liquidityAddressNumber, workDate);

      dateToSumVinMap.put(workDate, sumVin);

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  public abstract BigDecimal getSumVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException;

  /**
   * 
   */
  private void fillDateToVoutMap(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.trace("fillDateToVoutMap()");

    // ...
    LocalDate workDate;

    if (dateToSumVoutMap.isEmpty()) {
      workDate = startDate;
    } else {
      List<LocalDate> dateList = new ArrayList<>(dateToSumVoutMap.keySet());
      dateList.sort((d1, d2) -> d2.compareTo(d1));
      workDate = dateList.get(0);
    }

    // ...
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      BigDecimal sumVout = getSumVout(token, liquidityAddressNumber, workDate);

      dateToSumVoutMap.put(workDate, sumVout);

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  public abstract BigDecimal getSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException;

  /**
   * 
   */
  public long[] getTimestampOfDay(@Nonnull LocalDate localDate) {
    long startTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MIN)).getTime() / 1000;
    long endTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MAX)).getTime() / 1000;

    return new long[] {
        startTimeOfDay, endTimeOfDay
    };
  }
}
