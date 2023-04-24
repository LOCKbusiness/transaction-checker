package ch.dfx.reporting.transparency;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryAssetPriceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAssetPriceSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryImpermanentLossSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryImpermanentLossSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryInterimDifferenceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryInterimDifferenceSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTOList;

/**
 * 
 */
public class TransparencyReportFileHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportFileHelper.class);

  // ...
  private static final String HISTORY_AMOUNT_JSON_FILENAME = "TransparencyReportHistoryAmount.json";
  private static final String HISTORY_INTERIM_DIFFERENCE_JSON_FILENAME = "TransparencyReportHistoryInterimDifference.json";
  private static final String HISTORY_ASSET_PRICE_JSON_FILENAME = "TransparencyReportHistoryAssetPrice.json";
  private static final String HISTORY_PRICE_JSON_FILENAME = "TransparencyReportHistoryPrice.json";

  private static final Map<TokenEnum, String> TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP;

  static {
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP = new EnumMap<>(TokenEnum.class);

    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.BTC, "ImpermanentLossReportHistoryBTCPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.ETH, "ImpermanentLossReportHistoryETHPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.DUSD, "ImpermanentLossReportHistoryDUSDPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.USDT, "ImpermanentLossReportHistoryUSDTPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.USDC, "ImpermanentLossReportHistoryUSDCPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.EUROC, "ImpermanentLossReportHistoryEUROCPool.json");
    TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.put(TokenEnum.SPY, "ImpermanentLossReportHistorySPYPool.json");
  }

  private static final File DATA_PATH = Path.of("data", "yieldmachine").toFile();
  private static final File IMPERMANENT_LOSS_DATA_PATH = Path.of("data", "yieldmachine", "impermanentloss").toFile();

  // ...
  private final Gson gson;

  /**
   * 
   */
  public TransparencyReportFileHelper() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void writeHistoryAmountSheetDTOToJSON(@Nonnull HistoryAmountSheetDTOList historyAmountSheetDTOList) throws DfxException {
    LOGGER.debug("writeHistoryAmountSheetDTOToJSON()");

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyAmountFile)) {
      gson.toJson(historyAmountSheetDTOList, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("writeHistoryAmountSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void appendHistoryAmountSheetDTOToJSON(@Nonnull HistoryAmountSheetDTO historyAmountSheetDTO) throws DfxException {
    LOGGER.debug("appendHistorySheetDTOToJSON()");

    HistoryAmountSheetDTOList historyAmountSheetDTOListFromJSON = readHistoryAmountSheetDTOFromJSON(new HashSet<>());

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyAmountFile)) {
      historyAmountSheetDTOListFromJSON.add(historyAmountSheetDTO);

      gson.toJson(historyAmountSheetDTOListFromJSON, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("appendHistorySheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void writeHistoryInterimDifferenceSheetDTOToJSON(@Nonnull HistoryInterimDifferenceSheetDTOList historyInterimDifferenceSheetDTOList)
      throws DfxException {
    LOGGER.debug("writeHistoryInterimDifferenceSheetDTOToJSON()");

    File historyInterimDifferenceFile = new File(DATA_PATH, HISTORY_INTERIM_DIFFERENCE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyInterimDifferenceFile)) {
      gson.toJson(historyInterimDifferenceSheetDTOList, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("writeHistoryInterimDifferenceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void appendHistoryInterimDifferenceSheetDTOToJSON(@Nonnull HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO) throws DfxException {
    LOGGER.debug("appendHistoryInterimDifferenceSheetDTOToJSON()");

    HistoryInterimDifferenceSheetDTOList historyInterimDifferenceSheetDTOListFromJSON = readHistoryInterimDifferenceSheetDTOFromJSON(new HashSet<>());

    File historyInterimDifferenceFile = new File(DATA_PATH, HISTORY_INTERIM_DIFFERENCE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyInterimDifferenceFile)) {
      historyInterimDifferenceSheetDTOListFromJSON.add(historyInterimDifferenceSheetDTO);

      gson.toJson(historyInterimDifferenceSheetDTOListFromJSON, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("appendHistoryInterimDifferenceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void writeHistoryAssetPriceSheetDTOToJSON(@Nonnull HistoryAssetPriceSheetDTOList historyAssetPriceSheetDTOList) throws DfxException {
    LOGGER.debug("writeHistoryAssetPriceSheetDTOToJSON()");

    File historyAssetPriceFile = new File(DATA_PATH, HISTORY_ASSET_PRICE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyAssetPriceFile)) {
      gson.toJson(historyAssetPriceSheetDTOList, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("writeHistoryAssetPriceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void appendHistoryAssetPriceSheetDTOToJSON(@Nonnull HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO) throws DfxException {
    LOGGER.debug("appendHistoryAssetPriceSheetDTOToJSON()");

    HistoryAssetPriceSheetDTOList historyAssetPriceSheetDTOListFromJSON = readHistoryAssetPriceSheetDTOFromJSON(new HashSet<>());

    File historyAssetPriceFile = new File(DATA_PATH, HISTORY_ASSET_PRICE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyAssetPriceFile)) {
      historyAssetPriceSheetDTOListFromJSON.add(historyAssetPriceSheetDTO);

      gson.toJson(historyAssetPriceSheetDTOListFromJSON, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("appendHistoryAssetPriceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void writeHistoryPriceSheetDTOToJSON(@Nonnull HistoryPriceSheetDTOList historyPriceSheetDTOList) throws DfxException {
    LOGGER.debug("writeHistoryPriceSheetDTOToJSON()");

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyPriceFile)) {
      gson.toJson(historyPriceSheetDTOList, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("writeHistoryPriceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void appendHistoryPriceSheetDTOToJSON(@Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) throws DfxException {
    LOGGER.debug("appendHistoryPriceSheetDTOToJSON()");

    HistoryPriceSheetDTOList historyPriceSheetDTOListFromJSON = readHistoryPriceSheetDTOFromJSON(new HashSet<>());

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_JSON_FILENAME);

    try (FileWriter writer = new FileWriter(historyPriceFile)) {
      historyPriceSheetDTOListFromJSON.add(historyPriceSheetDTO);

      gson.toJson(historyPriceSheetDTOListFromJSON, writer);
      writer.flush();
    } catch (Exception e) {
      throw new DfxException("appendHistoryPriceSheetDTOToJSON", e);
    }
  }

  /**
   * 
   */
  public void writeHistoryImpermanentLossSheetDTOToJSON(
      @Nonnull TokenEnum token,
      @Nonnull HistoryImpermanentLossSheetDTOList historyImpermanentLossDTOList) throws DfxException {
    LOGGER.debug("writeHistoryImpermanentLossSheetDTOToJSON(): token=" + token);

    File historyImpermanentLossFile = null;

    String historyImpermanentLossFileName = TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.get(token);

    if (null != historyImpermanentLossFileName) {
      historyImpermanentLossFile = new File(IMPERMANENT_LOSS_DATA_PATH, historyImpermanentLossFileName);
    }

    if (null != historyImpermanentLossFile) {
      try (FileWriter writer = new FileWriter(historyImpermanentLossFile)) {
        gson.toJson(historyImpermanentLossDTOList, writer);
        writer.flush();
      } catch (Exception e) {
        throw new DfxException("writeHistoryImpermanentLossSheetDTOToJSON", e);
      }
    }
  }

  /**
   * 
   */
  public void appendHistoryImpermanentLossSheetDTOToJSON(
      @Nonnull TokenEnum token,
      @Nonnull HistoryImpermanentLossSheetDTO historyImpermanentLossDTO) throws DfxException {
    LOGGER.debug("appendHistoryImpermanentLossSheetDTOToJSON(): token=" + token);

    File historyImpermanentLossFile = null;

    String historyImpermanentLossFileName = TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.get(token);

    if (null != historyImpermanentLossFileName) {
      historyImpermanentLossFile = new File(IMPERMANENT_LOSS_DATA_PATH, historyImpermanentLossFileName);
    }

    if (null != historyImpermanentLossFile) {
      HistoryImpermanentLossSheetDTOList historyImpermanentLossSheetDTOListListFromJSON = readHistoryImpermanentLossSheetDTOFromJSON(token);

      try (FileWriter writer = new FileWriter(historyImpermanentLossFile)) {
        historyImpermanentLossSheetDTOListListFromJSON.add(historyImpermanentLossDTO);

        gson.toJson(historyImpermanentLossSheetDTOListListFromJSON, writer);
        writer.flush();
      } catch (Exception e) {
        throw new DfxException("appendHistoryImpermanentLossSheetDTOToJSON", e);
      }
    }
  }

  /**
   * 
   */
  public HistoryAmountSheetDTOList readHistoryAmountSheetDTOFromJSON(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryAmountSheetDTOFromJSON()");

    HistoryAmountSheetDTOList historyAmountSheetDTOList = new HistoryAmountSheetDTOList();

    File historyAmountFile = new File(DATA_PATH, HISTORY_AMOUNT_JSON_FILENAME);

    if (historyAmountFile.exists()) {
      try (FileReader reader = new FileReader(historyAmountFile)) {
        HistoryAmountSheetDTOList historyAmountSheetDTOListFromJSON = gson.fromJson(reader, HistoryAmountSheetDTOList.class);

        if (null != historyAmountSheetDTOListFromJSON) {
          for (HistoryAmountSheetDTO historyAmountSheetDTOFromJSON : historyAmountSheetDTOListFromJSON) {
            Timestamp timestamp = Timestamp.valueOf(historyAmountSheetDTOFromJSON.getTimestamp());

            if (historyHourSet.isEmpty()
                || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
              historyAmountSheetDTOList.add(historyAmountSheetDTOFromJSON);
            }
          }
        }
      } catch (Exception e) {
        throw new DfxException("readHistoryAmountSheetDTOFromJSON", e);
      }
    }

    return historyAmountSheetDTOList;
  }

  /**
   * 
   */
  public HistoryInterimDifferenceSheetDTOList readHistoryInterimDifferenceSheetDTOFromJSON(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryInterimDifferenceSheetDTOFromJSON()");

    HistoryInterimDifferenceSheetDTOList historyInterimDifferenceSheetDTOList = new HistoryInterimDifferenceSheetDTOList();

    File historyInterimDifferenceFile = new File(DATA_PATH, HISTORY_INTERIM_DIFFERENCE_JSON_FILENAME);

    if (historyInterimDifferenceFile.exists()) {
      try (FileReader reader = new FileReader(historyInterimDifferenceFile)) {
        HistoryInterimDifferenceSheetDTOList historyInterimDifferenceSheetDTOListFromJSON = gson.fromJson(reader, HistoryInterimDifferenceSheetDTOList.class);

        if (null != historyInterimDifferenceSheetDTOListFromJSON) {
          for (HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTOFromJSON : historyInterimDifferenceSheetDTOListFromJSON) {
            Timestamp timestamp = Timestamp.valueOf(historyInterimDifferenceSheetDTOFromJSON.getTimestamp());

            if (historyHourSet.isEmpty()
                || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
              historyInterimDifferenceSheetDTOList.add(historyInterimDifferenceSheetDTOFromJSON);
            }
          }
        }
      } catch (Exception e) {
        throw new DfxException("readHistoryInterimDifferenceSheetDTOFromJSON", e);
      }
    }

    return historyInterimDifferenceSheetDTOList;
  }

  /**
   * 
   */
  public HistoryAssetPriceSheetDTOList readHistoryAssetPriceSheetDTOFromJSON(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryAssetPriceSheetDTOFromJSON()");

    HistoryAssetPriceSheetDTOList historyAssetPriceSheetDTOList = new HistoryAssetPriceSheetDTOList();

    File historyAssetPriceFile = new File(DATA_PATH, HISTORY_ASSET_PRICE_JSON_FILENAME);

    if (historyAssetPriceFile.exists()) {
      try (FileReader reader = new FileReader(historyAssetPriceFile)) {
        HistoryAssetPriceSheetDTOList historyAssetPriceSheetDTOListFromJSON = gson.fromJson(reader, HistoryAssetPriceSheetDTOList.class);

        if (null != historyAssetPriceSheetDTOListFromJSON) {
          for (HistoryAssetPriceSheetDTO historyAssetPriceSheetDTOFromJSON : historyAssetPriceSheetDTOListFromJSON) {
            Timestamp timestamp = Timestamp.valueOf(historyAssetPriceSheetDTOFromJSON.getTimestamp());

            if (historyHourSet.isEmpty()
                || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
              historyAssetPriceSheetDTOList.add(historyAssetPriceSheetDTOFromJSON);
            }
          }
        }
      } catch (Exception e) {
        throw new DfxException("readHistoryAssetPriceSheetDTOFromJSON", e);
      }
    }

    return historyAssetPriceSheetDTOList;
  }

  /**
   * 
   */
  public HistoryPriceSheetDTOList readHistoryPriceSheetDTOFromJSON(@Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.debug("readHistoryPriceSheetDTOFromJSON()");

    HistoryPriceSheetDTOList historyPriceSheetDTOList = new HistoryPriceSheetDTOList();

    File historyPriceFile = new File(DATA_PATH, HISTORY_PRICE_JSON_FILENAME);

    if (historyPriceFile.exists()) {
      try (FileReader reader = new FileReader(historyPriceFile)) {
        HistoryPriceSheetDTOList historyPriceSheetDTOListFromJSON = gson.fromJson(reader, HistoryPriceSheetDTOList.class);

        if (null != historyPriceSheetDTOListFromJSON) {
          for (HistoryPriceSheetDTO historyPriceSheetDTOFromJSON : historyPriceSheetDTOListFromJSON) {
            Timestamp timestamp = Timestamp.valueOf(historyPriceSheetDTOFromJSON.getTimestamp());

            if (historyHourSet.isEmpty()
                || historyHourSet.contains(timestamp.toLocalDateTime().getHour())) {
              historyPriceSheetDTOList.add(historyPriceSheetDTOFromJSON);
            }
          }
        }
      } catch (Exception e) {
        throw new DfxException("readHistoryPriceSheetDTOFromJSON", e);
      }
    }

    return historyPriceSheetDTOList;
  }

  /**
   * 
   */
  public HistoryImpermanentLossSheetDTOList readHistoryImpermanentLossSheetDTOFromJSON(@Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("readHistoryImpermanentLossSheetDTOFromJSON(): token=" + token);

    HistoryImpermanentLossSheetDTOList historyImpermanentLossSheetDTOList = null;

    File historyImpermanentLossFile = null;

    String historyImpermanentLossFileName = TOKEN_TO_IMPERMANENTLOSS_JSON_FILENAME_MAP.get(token);

    if (null != historyImpermanentLossFileName) {
      historyImpermanentLossFile = new File(IMPERMANENT_LOSS_DATA_PATH, historyImpermanentLossFileName);
    }

    if (null != historyImpermanentLossFile) {
      if (historyImpermanentLossFile.exists()) {
        try (FileReader reader = new FileReader(historyImpermanentLossFile)) {
          HistoryImpermanentLossSheetDTOList historyImpermanentLossSheetDTOListFromJSON = gson.fromJson(reader, HistoryImpermanentLossSheetDTOList.class);

          if (null != historyImpermanentLossSheetDTOListFromJSON) {
            historyImpermanentLossSheetDTOList = historyImpermanentLossSheetDTOListFromJSON;
          }
        } catch (Exception e) {
          throw new DfxException("readHistoryImpermanentLossSheetDTOFromJSON", e);
        }
      }
    }

    if (null == historyImpermanentLossSheetDTOList) {
      historyImpermanentLossSheetDTOList = new HistoryImpermanentLossSheetDTOList();
    }

    return historyImpermanentLossSheetDTOList;
  }
}
