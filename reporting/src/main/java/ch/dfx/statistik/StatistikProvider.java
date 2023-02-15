package ch.dfx.statistik;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    LOGGER.trace("fillDfiDepositStatistikData()");

    try {
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
    } catch (Exception e) {
      throw new DfxException("fillDepositStatistikData", e);
    }
  }

  public abstract void openStatements(@Nonnull Connection connection) throws DfxException;

  public abstract void closeStatements() throws DfxException;

  /**
   * 
   */
  private void fillDateToCountMap(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, Integer> dateToCountMap) throws DfxException {
    LOGGER.trace("fillDateToCountMap()");

    // ...
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      int count = getCount(token, workDate);

      dateToCountMap.merge(workDate, count, (currVal, newVal) -> currVal += newVal);

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
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      BigDecimal sumVin = getSumVin(token, liquidityAddressNumber, workDate);

      dateToSumVinMap.merge(workDate, sumVin, (currVal, newVal) -> currVal.add(newVal));

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
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      BigDecimal sumVout = getSumVout(token, liquidityAddressNumber, workDate);

      dateToSumVoutMap.merge(workDate, sumVout, (currVal, newVal) -> currVal.add(newVal));

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
