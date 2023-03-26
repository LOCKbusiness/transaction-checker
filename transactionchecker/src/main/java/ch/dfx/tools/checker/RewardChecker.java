package ch.dfx.tools.checker;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class RewardChecker {
  private static final Logger LOGGER = LogManager.getLogger(RewardChecker.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  // ...
  private final NetworkEnum network;

  // ...
  private PreparedStatement transactionOutSelectStatement = null;

  // ...
  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public RewardChecker(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;
    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseBalanceHelper = databaseBalanceHelper;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void check(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("check()");

    openStatements(connection);

    // ...
    AddressDTO rewardAddressDTO = databaseBlockHelper.getAddressDTOByAddress("df1qy2c4v4sjwtwcvxw24j99jxt0nfu98hc8hjwhxs");
    Integer rewardAddressNumber = (null == rewardAddressDTO ? 0 : rewardAddressDTO.getNumber());

    BigDecimal totalRewards = BigDecimal.ZERO;
    Map<LocalDate, BigDecimal> dayToRewardMap = new HashMap<>();

    List<String> masternodeRewardBlockHashList = createMasternodeRewardBlockHashList();
    LOGGER.debug("Number of Reward Blocks: " + masternodeRewardBlockHashList.size());

    for (String masternodeRewardBlockHash : masternodeRewardBlockHashList) {
      BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByHash(masternodeRewardBlockHash);

      if (null == blockDTO) {
        throw new DfxException("Unknown Block Hash '" + masternodeRewardBlockHash + "' found");
      }

      RewardCheckData rewardCheckData = getRewardCheckData(blockDTO.getNumber());

      Timestamp blockTimestamp = new Timestamp(blockDTO.getTimestamp() * 1000);
      LocalDate rewardDay = blockTimestamp.toLocalDateTime().toLocalDate();

      dayToRewardMap.merge(rewardDay, rewardCheckData.reward, (currVal, nextVal) -> currVal.add(nextVal));

      if (rewardCheckData.addressNumber != rewardAddressNumber.intValue()) {
        LOGGER.error("Wrong Reward Address: " + rewardCheckData);
      }

      totalRewards = totalRewards.add(rewardCheckData.reward);
    }

    // ...
    LOGGER.debug("Total Rewards: " + totalRewards);

    dumpAllDays(dayToRewardMap);
    dumpLastDay(dayToRewardMap);

    closeStatements();
  }

  /**
   * 
   */
  private List<String> createMasternodeRewardBlockHashList() throws DfxException {
    LOGGER.trace("createMasternodeRewardBlockHashList()");

    List<String> masternodeRewardBlockHashList = new ArrayList<>();

    List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseBlockHelper.getMasternodeWhitelistDTOList();
    List<MasternodeWhitelistDTO> filteredMasternodeWhitelistDTOList =
        masternodeWhitelistDTOList.stream().filter(dto -> null != dto.getState()).collect(Collectors.toList());

    for (MasternodeWhitelistDTO masternodeWhitelistDTO : filteredMasternodeWhitelistDTOList) {
      String masternodeId = masternodeWhitelistDTO.getTransactionId();
      Map<String, String> masternodeBlockMap = dataProvider.getMasternodeBlocks(masternodeId);
      masternodeRewardBlockHashList.addAll(masternodeBlockMap.values());
    }

    return masternodeRewardBlockHashList;
  }

  /**
   * 
   */
  private void dumpAllDays(@Nonnull Map<LocalDate, BigDecimal> dayToRewardMap) throws DfxException {
    LOGGER.trace("dumpAllDays()");

    File dumpFile = new File("data", "rewardcheck.csv");

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile))) {
      writer.append("\"DAY\";\"REWARD\";\"TOTAL BALANCE\";\"YIELD\"");
      writer.append("\n");

      Map<LocalDate, StatistikData> dayToStatistikDataMap = readStatistikData();

      // ...
      List<LocalDate> statistikDayList = new ArrayList<>(dayToStatistikDataMap.keySet());
      statistikDayList.sort((d1, d2) -> d1.compareTo(d2));

      // ...
      BigDecimal totalBalance = BigDecimal.ZERO;

      for (LocalDate statistikDay : statistikDayList) {
        StatistikData statistikData = dayToStatistikDataMap.get(statistikDay);
        BigDecimal reward = dayToRewardMap.getOrDefault(statistikDay, BigDecimal.ZERO);

        BigDecimal balance = statistikData.sumVin.subtract(statistikData.sumVout);
        totalBalance = totalBalance.add(balance);

        BigDecimal yield = reward.divide(totalBalance, MATH_CONTEXT);
        yield = yield.multiply(new BigDecimal(365));

        writer.append(statistikDay.toString());
        writer.append(";").append(reward.toString());
        writer.append(";").append(totalBalance.toString());
        writer.append(";").append(yield.toString());
        writer.append("\n");

        LOGGER.debug(statistikDay + ": " + reward + " / " + totalBalance + " / " + yield);
      }
    } catch (Exception e) {
      throw new DfxException("dumpAllDays", e);
    }
  }

  /**
   * 
   */
  private void dumpLastDay(@Nonnull Map<LocalDate, BigDecimal> dayToRewardMap) throws DfxException {
    LOGGER.trace("dumpLastDay()");

    List<LocalDate> rewardDayList = new ArrayList<>(dayToRewardMap.keySet());
    rewardDayList.sort((d1, d2) -> d1.compareTo(d2));

    BigDecimal lastDayReward = dayToRewardMap.get(rewardDayList.get(rewardDayList.size() - 2));
    BigDecimal totalStakingBalance = getTotalStakingBalance();

    BigDecimal yield = lastDayReward.divide(totalStakingBalance, MATH_CONTEXT);
    yield = yield.multiply(new BigDecimal(365));

    LOGGER.debug("");
    LOGGER.debug("Last Day Reward:       " + lastDayReward);
    LOGGER.debug("Total Staking Balance: " + totalStakingBalance);
    LOGGER.debug("Yield:                 " + yield);
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String transactionOutSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number=? AND transaction_number=0";
      transactionOutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionOutSelectSql));
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      transactionOutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private RewardCheckData getRewardCheckData(int blockNumber) throws DfxException {
    LOGGER.trace("getRewardCheckData()");

    try {
      RewardCheckData rewardCheckData = null;

      transactionOutSelectStatement.setInt(1, blockNumber);

      ResultSet resultSet = transactionOutSelectStatement.executeQuery();

      if (resultSet.next()) {
        rewardCheckData = new RewardCheckData();

        rewardCheckData.addressNumber = resultSet.getInt("address_number");
        rewardCheckData.reward = resultSet.getBigDecimal("vout");
      }

      resultSet.close();

      return rewardCheckData;
    } catch (Exception e) {
      throw new DfxException("getRewardCheckData", e);
    }
  }

  /**
   * 
   */
  private BigDecimal getTotalStakingBalance() throws DfxException {
    LOGGER.trace("getTotalStakingBalance()");

    List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOList(TokenEnum.DFI);

    BigDecimal totalStakingBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : stakingDTOList) {
      totalStakingBalance = totalStakingBalance.add(stakingDTO.getVin().subtract(stakingDTO.getVout()));
    }

    return totalStakingBalance;
  }

  /**
   * 
   */
  private Map<LocalDate, StatistikData> readStatistikData() throws DfxException {
    LOGGER.trace("readStatistikData()");

    File statistikFile = new File("data/statistik", "Statistik.csv");

    try (BufferedReader reader = new BufferedReader(new FileReader(statistikFile))) {
      // skip header ...
      reader.readLine();

      Map<LocalDate, StatistikData> dayToStatistikDataMap = new HashMap<>();

      String line = null;

      while (null != (line = reader.readLine())) {
        String[] entryArray = line.split(";");

        StatistikData statistikData = new StatistikData();

        statistikData.day = LocalDate.parse(entryArray[0]);
        statistikData.count = Integer.valueOf(entryArray[1]);
        statistikData.sumVin = new BigDecimal(entryArray[2]);
        statistikData.sumVout = new BigDecimal(entryArray[3]);

        dayToStatistikDataMap.put(statistikData.day, statistikData);
      }

      return dayToStatistikDataMap;
    } catch (Exception e) {
      throw new DfxException("readStatistikData", e);
    }
  }

  /**
   * 
   */
  private class RewardCheckData {
    private int addressNumber = 0;
    private BigDecimal reward = BigDecimal.ZERO;

    @Override
    public String toString() {
      return TransactionCheckerUtils.toJson(this);
    }
  }

  /**
   * 
   */
  private class StatistikData {
    private LocalDate day = null;
    private Integer count = 0;
    private BigDecimal sumVin = BigDecimal.ZERO;
    private BigDecimal sumVout = BigDecimal.ZERO;

    @Override
    public String toString() {
      return new StringBuilder()
          .append(day)
          .append(";").append(count)
          .append(";").append(sumVin)
          .append(";").append(sumVout)
          .toString();
    }
  }
}
