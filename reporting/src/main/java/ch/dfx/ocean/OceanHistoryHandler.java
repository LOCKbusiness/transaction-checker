package ch.dfx.ocean;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.ocean.data.HistoryDetailDTO;
import ch.dfx.ocean.data.HistoryDetailDTOList;

/**
 * 
 */
public class OceanHistoryHandler {
  private static final Logger LOGGER = LogManager.getLogger(OceanHistoryHandler.class);

  // ...
  private final NetworkEnum network;
  private final File rootPath;
  private final Gson gson;

  private final OceanWebAPIHandler oceanHandler;

  /**
   * 
   */
  public OceanHistoryHandler(
      @Nonnull NetworkEnum network,
      @Nonnull File rootPath) {
    this.network = network;
    this.rootPath = rootPath;

    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.oceanHandler = new OceanWebAPIHandler();
  }

  /**
   * 
   */
  public HistoryDetailDTOList readFromJSON(@Nonnull String address) throws DfxException {
    LOGGER.debug("readFromJSON(): address=" + address);

    try {
      List<File> historyFileList = Arrays.asList(
          rootPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              return name.startsWith(address + "-history-") && name.endsWith(".json");
            }
          }));
      historyFileList.sort((f1, f2) -> f1.compareTo(f2));

      // ...
      HistoryDetailDTOList historyDetailDTOList = new HistoryDetailDTOList();

      for (File historyFile : historyFileList) {
        LOGGER.debug("Read: " + historyFile.getAbsolutePath());
        historyDetailDTOList.addAll(doRead(historyFile));
      }

      return historyDetailDTOList;
    } catch (Exception e) {
      throw new DfxException("read", e);
    }
  }

  /**
   * 
   */
  private HistoryDetailDTOList doRead(@Nonnull File historyFile) throws DfxException {
    LOGGER.trace("doRead()");

    try (FileReader fileReader = new FileReader(historyFile)) {
      return gson.fromJson(fileReader, HistoryDetailDTOList.class);
    } catch (Exception e) {
      throw new DfxException("doRead", e);
    }
  }

  /**
   * 
   */
  public HistoryDetailDTOList writeToJSON(@Nonnull String address) throws DfxException {
    LOGGER.debug("writeToJSON(): address=" + address);

    try {
      HistoryDetailDTOList storedHistoryDetailDTOList = readFromJSON(address);

      String storedTxid = null;

      if (!storedHistoryDetailDTOList.isEmpty()) {
        HistoryDetailDTO historyDetailDTO = storedHistoryDetailDTOList.get(0);
        storedTxid = historyDetailDTO.getTxid();
      }

      oceanHandler.setup(network, address);

      // ...
      String next = oceanHandler.fetchHistory(null, storedTxid);

      while (null != next) {
        next = oceanHandler.fetchHistory(next, storedTxid);
      }

      // ...
      HistoryDetailDTOList totalHistoryDetailDTOList = oceanHandler.getHistoryDetailDTOList();

      if (!totalHistoryDetailDTOList.isEmpty()) {
        totalHistoryDetailDTOList.addAll(storedHistoryDetailDTOList);

        List<List<HistoryDetailDTO>> partitionHistoryDetailDTOList = Lists.partition(totalHistoryDetailDTOList, 100000);

        for (int i = 0; i < partitionHistoryDetailDTOList.size(); i++) {
          List<HistoryDetailDTO> historyDetailDTOList = partitionHistoryDetailDTOList.get(i);
          doWrite(address, i, historyDetailDTOList);
        }
      }

      return totalHistoryDetailDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("write", e);
    }
  }

  /**
   * 
   */
  private void doWrite(
      @Nonnull String address,
      int i,
      @Nonnull List<HistoryDetailDTO> historyDetailDTOList) throws DfxException {
    LOGGER.trace("doWrite()");

    String fileNumber = StringUtils.leftPad(Integer.toString(i), 3, "0");

    try (FileWriter fileWriter = new FileWriter(new File(rootPath, address + "-history-" + fileNumber + ".json"))) {
      gson.toJson(historyDetailDTOList, fileWriter);
      fileWriter.flush();
    } catch (Exception e) {
      throw new DfxException("doWrite", e);
    }
  }
}
