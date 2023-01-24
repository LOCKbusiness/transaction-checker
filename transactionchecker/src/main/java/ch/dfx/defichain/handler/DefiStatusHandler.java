package ch.dfx.defichain.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.network.PeerInfoData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DefiStatusHandler {
  private static final Logger LOGGER = LogManager.getLogger(DefiStatusHandler.class);

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public DefiStatusHandler() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public boolean isInSync() {
    LOGGER.trace("isInSync()");

    boolean isInSync = false;

    try {
      Long blockCount = dataProvider.getBlockCount();
      List<PeerInfoData> peerInfoDataList = dataProvider.getPeerInfo();

      LOGGER.debug("Block Count: " + blockCount);

      Map<Long, Integer> syncBlockCountMap = new HashMap<>();

      for (PeerInfoData peerInfoData : peerInfoDataList) {
        Long syncedHeaders = peerInfoData.getSynced_headers();
        Long syncedBlocks = peerInfoData.getSynced_blocks();
        List<Long> inflight = peerInfoData.getInflight();

        if (inflight.isEmpty()
            && (null != syncedHeaders && -1 != syncedHeaders.intValue())
            && (null != syncedBlocks && -1 != syncedBlocks.intValue())
            && syncedHeaders.equals(syncedBlocks)) {
          syncBlockCountMap.merge(syncedBlocks, 1, (currVal, newVal) -> currVal += newVal);
        }
      }

      Long syncBlock = -1l;

      for (Entry<Long, Integer> syncBlockCountMapEntry : syncBlockCountMap.entrySet()) {
        Integer count = syncBlockCountMapEntry.getValue();

        if (5 <= count) {
          syncBlock = syncBlockCountMapEntry.getKey();
        }
      }

      isInSync = -1 != blockCount.intValue() && syncBlock.equals(blockCount);

      LOGGER.debug("Sync Block: " + syncBlock);
      LOGGER.debug("Is in Sync: " + isInSync);
    } catch (Exception e) {
      LOGGER.error("isInSync", e);
      isInSync = false;
    }

    return isInSync;
  }
}
