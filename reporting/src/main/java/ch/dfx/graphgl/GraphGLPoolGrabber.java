package ch.dfx.graphgl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.graphgl.data.FarmingHistoryDataDTO;
import ch.dfx.graphgl.data.FarmingHistoryGetDTO;
import ch.dfx.graphgl.data.FarmingHistoryGetDTOList;
import ch.dfx.graphgl.data.FarmingHistoryPoolDTO;
import ch.dfx.graphgl.data.FarmingHistoryPoolDTOList;

/**
 * 
 */
public class GraphGLPoolGrabber {
  private static final Logger LOGGER = LogManager.getLogger(GraphGLPoolGrabber.class);

  // ...
  private final File rootPath;
  private final Gson gson;

  /**
   * 
   */
  public GraphGLPoolGrabber(@Nonnull File rootPath) {
    this.rootPath = rootPath;

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void grabPoolTransaction() throws DfxException {

    String farmingHistoryPoolFileName = "farming-history-pools.json";

    FarmingHistoryPoolDTOList farmingHistoryPoolDTOList = readTotalFarmingHistoryPoolDTOList(farmingHistoryPoolFileName);

    FarmingHistoryPoolDTOList btcFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList ethFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList dusdFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList usdtFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList usdcFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList eurocFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();
    FarmingHistoryPoolDTOList spyFarmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();

    for (FarmingHistoryPoolDTO farmingHistoryPoolDTO : farmingHistoryPoolDTOList) {
      String pair = farmingHistoryPoolDTO.getPair();

      if ("BTC-DFI".equals(pair)) {
        btcFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("ETH-DFI".equals(pair)) {
        ethFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("DUSD-DFI".equals(pair)) {
        dusdFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("USDT-DUSD".equals(pair)) {
        usdtFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("USDC-DUSD".equals(pair)) {
        usdcFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("EUROC-DUSD".equals(pair)) {
        eurocFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      } else if ("SPY-DUSD".equals(pair)) {
        spyFarmingHistoryPoolDTOList.add(farmingHistoryPoolDTO);
      }
    }

    // ...
    String btcFarmingHistoryPoolFileName = "farming-history-pool-btc.json";
    writeFarmingHistoryPoolDTOList(btcFarmingHistoryPoolDTOList, btcFarmingHistoryPoolFileName);

    String ethFarmingHistoryPoolFileName = "farming-history-pool-eth.json";
    writeFarmingHistoryPoolDTOList(ethFarmingHistoryPoolDTOList, ethFarmingHistoryPoolFileName);

    String dusdFarmingHistoryPoolFileName = "farming-history-pool-dusd.json";
    writeFarmingHistoryPoolDTOList(dusdFarmingHistoryPoolDTOList, dusdFarmingHistoryPoolFileName);

    String usdtFarmingHistoryPoolFileName = "farming-history-pool-usdt.json";
    writeFarmingHistoryPoolDTOList(usdtFarmingHistoryPoolDTOList, usdtFarmingHistoryPoolFileName);

    String usdcFarmingHistoryPoolFileName = "farming-history-pool-usdc.json";
    writeFarmingHistoryPoolDTOList(usdcFarmingHistoryPoolDTOList, usdcFarmingHistoryPoolFileName);

    String eurocFarmingHistoryPoolFileName = "farming-history-pool-euroc.json";
    writeFarmingHistoryPoolDTOList(eurocFarmingHistoryPoolDTOList, eurocFarmingHistoryPoolFileName);

    String spyFarmingHistoryPoolFileName = "farming-history-pool-spy.json";
    writeFarmingHistoryPoolDTOList(spyFarmingHistoryPoolDTOList, spyFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readBTCFarmingHistoryPoolDTOList() throws DfxException {
    String btcFarmingHistoryPoolFileName = "farming-history-pool-btc.json";
    return readFarmingHistoryPoolDTOList(btcFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readETHFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-eth.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readDUSDFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-dusd.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readUSDTFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-usdt.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readUSDCFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-usdc.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readEUROCFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-euroc.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  public FarmingHistoryPoolDTOList readSPYFarmingHistoryPoolDTOList() throws DfxException {
    String ethFarmingHistoryPoolFileName = "farming-history-pool-spy.json";
    return readFarmingHistoryPoolDTOList(ethFarmingHistoryPoolFileName);
  }

  /**
   * 
   */
  private FarmingHistoryPoolDTOList readTotalFarmingHistoryPoolDTOList(@Nonnull String fileName) throws DfxException {
    LOGGER.trace("readTotalFarmingHistoryPoolDTOList()");

    File jsonFile = new File(rootPath, fileName);

    try (FileReader fileReader = new FileReader(jsonFile)) {
      FarmingHistoryPoolDTOList farmingHistoryPoolDTOList = new FarmingHistoryPoolDTOList();

      FarmingHistoryDataDTO farmingHistoryDataDTO = gson.fromJson(fileReader, FarmingHistoryDataDTO.class);

      Map<String, FarmingHistoryGetDTOList> dataMap = farmingHistoryDataDTO.getData();
      FarmingHistoryGetDTOList farmingHistoryGetDTOList = dataMap.values().iterator().next();

      for (FarmingHistoryGetDTO farmingHistoryGetDTO : farmingHistoryGetDTOList) {
        farmingHistoryPoolDTOList.addAll(farmingHistoryGetDTO.getPools());
      }

      return farmingHistoryPoolDTOList;
    } catch (Exception e) {
      throw new DfxException("readTotalFarmingHistoryPoolDTOList", e);
    }
  }

  /**
   * 
   */
  private FarmingHistoryPoolDTOList readFarmingHistoryPoolDTOList(@Nonnull String fileName) throws DfxException {
    LOGGER.trace("readFarmingHistoryPoolDTOList()");

    File jsonFile = new File(rootPath, fileName);

    try (FileReader fileReader = new FileReader(jsonFile)) {
      return gson.fromJson(fileReader, FarmingHistoryPoolDTOList.class);
    } catch (Exception e) {
      throw new DfxException("readFarmingHistoryPoolDTOList", e);
    }
  }

  /**
   * 
   */
  private void writeFarmingHistoryPoolDTOList(
      @Nonnull FarmingHistoryPoolDTOList farmingHistoryPoolDTOList,
      @Nonnull String fileName) throws DfxException {
    LOGGER.trace("writeFarmingHistoryPoolDTOList()");

    File jsonFile = new File(rootPath, fileName);

    try (FileWriter fileWriter = new FileWriter(jsonFile)) {
      gson.toJson(farmingHistoryPoolDTOList, fileWriter);
      fileWriter.flush();
    } catch (Exception e) {
      throw new DfxException("writeFarmingHistoryPoolDTOList", e);
    }
  }
}
