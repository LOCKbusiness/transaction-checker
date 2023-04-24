package ch.dfx.ocean;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.ocean.data.HistoryDetailDTO;
import ch.dfx.ocean.data.HistoryDetailDTOList;
import ch.dfx.ocean.data.HistoryPoolDTO;
import ch.dfx.ocean.data.HistoryPoolDTOList;

/**
 * 
 */
public class OceanHistoryPoolTransactionGrabber {
  private static final Logger LOGGER = LogManager.getLogger(OceanHistoryPoolTransactionGrabber.class);

  // ...
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String POOL_ADD_LIQUIDITY = "AddPoolLiquidity";
  private static final String POOL_REMOVE_LIQUIDITY = "RemovePoolLiquidity";

  // ...
  private final File rootPath;
  private final Gson gson;

  /**
   * 
   */
  public OceanHistoryPoolTransactionGrabber(@Nonnull File rootPath) {
    this.rootPath = rootPath;

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void grabPoolTransaction(
      @Nonnull HistoryDetailDTOList liquidityHistoryDetailDTOList,
      @Nonnull HistoryDetailDTOList poolHistoryDetailDTOList,
      @Nonnull String poolAddress,
      @Nonnull TokenEnum tokenA,
      @Nonnull TokenEnum tokenB) throws DfxException {
    LOGGER.debug("grabPoolTransaction(): tokenA/tokenB=" + tokenA + "/" + tokenB);

    // ...
    Map<String, HistoryDetailDTO> liquidityTransactionToHistoryDetailDTOMap = new LinkedHashMap<>();
    liquidityHistoryDetailDTOList.stream()
        .filter(dto -> POOL_ADD_LIQUIDITY.equals(dto.getType()) || POOL_REMOVE_LIQUIDITY.equals(dto.getType()))
        .forEach(dto -> liquidityTransactionToHistoryDetailDTOMap.put(dto.getTxid(), dto));

    Map<String, HistoryDetailDTO> poolTransactionToHistoryDetailDTOMap = new LinkedHashMap<>();
    poolHistoryDetailDTOList.stream()
        .filter(dto -> POOL_ADD_LIQUIDITY.equals(dto.getType()) || POOL_REMOVE_LIQUIDITY.equals(dto.getType()))
        .forEach(dto -> poolTransactionToHistoryDetailDTOMap.put(dto.getTxid(), dto));

    // ...
    HistoryPoolDTOList historyPoolDTOList =
        createHistoryPoolDTOList(
            liquidityTransactionToHistoryDetailDTOMap,
            poolTransactionToHistoryDetailDTOMap,
            tokenA, tokenB);

    HistoryPoolDTOList historyPoolDTOPerHourList =
        createHistoryPoolDTOPerHourList(poolAddress, tokenA, tokenB, historyPoolDTOList);

    // ...
    String fileName =
        new StringBuilder()
            .append(poolAddress)
            .append("-").append(tokenA.toString())
            .append("-").append(tokenB.toString())
            .append("-").append("pool-total-history.json")
            .toString();

    writeHistoryPoolDTOList(historyPoolDTOList, fileName);

    // ...
    String fileNamePerHour =
        new StringBuilder()
            .append(poolAddress)
            .append("-").append(tokenA.toString())
            .append("-").append(tokenB.toString())
            .append("-").append("pool-per-hour-history.json")
            .toString();

    writeHistoryPoolDTOList(historyPoolDTOPerHourList, fileNamePerHour);
  }

  /**
   * 
   */
  public HistoryPoolDTOList readTotalHistoryPoolDTOList(
      @Nonnull String poolAddress,
      @Nonnull TokenEnum tokenA,
      @Nonnull TokenEnum tokenB) throws DfxException {

    String fileName =
        new StringBuilder()
            .append(poolAddress)
            .append("-").append(tokenA.toString())
            .append("-").append(tokenB.toString())
            .append("-").append("pool-total-history.json")
            .toString();

    return readHistoryPoolDTOList(fileName);
  }

  /**
   * 
   */
  private HistoryPoolDTOList createHistoryPoolDTOList(
      @Nonnull Map<String, HistoryDetailDTO> liquidityTransactionToHistoryDetailDTOMap,
      @Nonnull Map<String, HistoryDetailDTO> poolTransactionToHistoryDetailDTOMap,
      @Nonnull TokenEnum tokenA,
      @Nonnull TokenEnum tokenB) {
    LOGGER.trace("createHistoryPoolDTOList()");

    HistoryPoolDTOList historyPoolDTOList = new HistoryPoolDTOList();

    for (String transactionId : poolTransactionToHistoryDetailDTOMap.keySet()) {
      HistoryDetailDTO poolHistoryDetailDTO = poolTransactionToHistoryDetailDTOMap.get(transactionId);
      HistoryDetailDTO liquidityHistoryDetailDTO = liquidityTransactionToHistoryDetailDTOMap.get(transactionId);

      Timestamp timestamp = new Timestamp(poolHistoryDetailDTO.getBlock().getTime() * 1000);

      Map<String, BigDecimal> tokenToAmountMap = createTokenToAmountMap(poolHistoryDetailDTO.getAmounts());

      if (null != liquidityHistoryDetailDTO) {
        tokenToAmountMap.putAll(createTokenToAmountMap(liquidityHistoryDetailDTO.getAmounts()));
      }

      HistoryPoolDTO historyPoolDTO = new HistoryPoolDTO();

      historyPoolDTO.setTimestamp(DATE_FORMAT.format(timestamp));
      historyPoolDTO.setTxId(poolHistoryDetailDTO.getTxid());
      historyPoolDTO.setType(poolHistoryDetailDTO.getType());

      String tokenAName = tokenA.toString();
      String tokenBName = tokenB.toString();
      String poolName = tokenAName + "-" + tokenBName;

      historyPoolDTO.setPoolTokenA(tokenToAmountMap.get(tokenAName));
      historyPoolDTO.setPoolTokenB(tokenToAmountMap.get(tokenBName));
      historyPoolDTO.setPoolToken(tokenToAmountMap.get(poolName));

      historyPoolDTOList.add(historyPoolDTO);
    }

    return historyPoolDTOList;
  }

  /**
   * 
   */
  private Map<String, BigDecimal> createTokenToAmountMap(@Nonnull List<String> amountWithTokenList) {
    LOGGER.trace("createTokenToAmountMap()");

    Map<String, BigDecimal> tokenToAmountMap = new HashMap<>();

    for (String amountWithToken : amountWithTokenList) {
      String[] amountWithTokenArray = amountWithToken.split("\\@");
      tokenToAmountMap.put(amountWithTokenArray[1], new BigDecimal(amountWithTokenArray[0]));
    }

    return tokenToAmountMap;
  }

  /**
   * 
   */
  private HistoryPoolDTOList createHistoryPoolDTOPerHourList(
      @Nonnull String address,
      @Nonnull TokenEnum tokenA,
      @Nonnull TokenEnum tokenB,
      @Nonnull HistoryPoolDTOList historyPoolDTOList) throws DfxException {
    LOGGER.trace("createHistoryPoolDTOListPerHour()");

    Map<String, HistoryPoolDTO> hourToHistoryPoolDTOAddMap = new HashMap<>();
    Map<String, HistoryPoolDTO> hourToHistoryPoolDTORemoveMap = new HashMap<>();

    for (HistoryPoolDTO historyPoolDTO : historyPoolDTOList) {
      String type = historyPoolDTO.getType();

      if (POOL_ADD_LIQUIDITY.equals(type)) {
        fillHourToHistoryPoolDTOMap(historyPoolDTO, hourToHistoryPoolDTOAddMap);
      } else if (POOL_REMOVE_LIQUIDITY.equals(type)) {
        fillHourToHistoryPoolDTOMap(historyPoolDTO, hourToHistoryPoolDTORemoveMap);
      }
    }

    // ...
    HistoryPoolDTOList historyPoolDTOPerHourList = new HistoryPoolDTOList();
    historyPoolDTOPerHourList.addAll(hourToHistoryPoolDTOAddMap.values());
    historyPoolDTOPerHourList.addAll(hourToHistoryPoolDTORemoveMap.values());

    historyPoolDTOPerHourList.sort(new Comparator<HistoryPoolDTO>() {
      @Override
      public int compare(HistoryPoolDTO dto1, HistoryPoolDTO dto2) {
        int compare = dto2.getTimestamp().compareTo(dto1.getTimestamp());

        if (0 == compare) {
          compare = dto1.getType().compareTo(dto2.getType());
        }

        return compare;
      }
    });

    return historyPoolDTOPerHourList;
  }

  /**
   * 
   */
  private void fillHourToHistoryPoolDTOMap(
      @Nonnull HistoryPoolDTO historyPoolDTO,
      @Nonnull Map<String, HistoryPoolDTO> hourToHistoryPoolDTOMap) {
    String dateWithHour = historyPoolDTO.getTimestamp().substring(0, 13);

    HistoryPoolDTO hourHistoryPoolDTO = hourToHistoryPoolDTOMap.get(dateWithHour);

    if (null == hourHistoryPoolDTO) {
      hourHistoryPoolDTO = new HistoryPoolDTO();
      hourHistoryPoolDTO.setTimestamp(dateWithHour + ":00:00");
      hourHistoryPoolDTO.setType(historyPoolDTO.getType());
    }

    hourHistoryPoolDTO.addPoolTokenA(historyPoolDTO.getPoolTokenA());
    hourHistoryPoolDTO.addPoolTokenB(historyPoolDTO.getPoolTokenB());
    hourHistoryPoolDTO.addPoolToken(historyPoolDTO.getPoolToken());

    hourToHistoryPoolDTOMap.put(dateWithHour, hourHistoryPoolDTO);
  }

  /**
   * 
   */
  private HistoryPoolDTOList readHistoryPoolDTOList(@Nonnull String fileName) throws DfxException {
    LOGGER.trace("readHistoryPoolDTOList()");

    File jsonFile = new File(rootPath, fileName);

    try (FileReader fileReader = new FileReader(jsonFile)) {
      return gson.fromJson(fileReader, HistoryPoolDTOList.class);
    } catch (Exception e) {
      throw new DfxException("readHistoryPoolDTOList", e);
    }
  }

  /**
   * 
   */
  private void writeHistoryPoolDTOList(
      @Nonnull HistoryPoolDTOList historyPoolDTOList,
      @Nonnull String fileName) throws DfxException {
    LOGGER.trace("writeHistoryPoolDTOList()");

    File jsonFile = new File(rootPath, fileName);

    try (FileWriter fileWriter = new FileWriter(jsonFile)) {
      gson.toJson(historyPoolDTOList, fileWriter);
      fileWriter.flush();
    } catch (Exception e) {
      throw new DfxException("writeHistoryPoolDTOList", e);
    }
  }
}
