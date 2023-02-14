package ch.dfx.process;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.process.data.ProcessInfoDTO;

/**
 * 
 */
public class ProcessInfoServiceImpl implements ProcessInfoService {
  private static final Logger LOGGER = LogManager.getLogger(ProcessInfoServiceImpl.class);

  // ...
  private static final long KB = 1024;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;

  private static final long MEMORY_WATERMARK_HIGH = 80;
  private static final long MEMORY_WATERMARK_CRITICAL = 90;

  private static final long DISK_WATERMARK_HIGH = 80;
  private static final long DISK_WATERMARK_CRITICAL = 90;

  /**
   * 
   */
  public ProcessInfoServiceImpl() {
  }

  @Override
  public void sendProcessInfo(@Nonnull ProcessInfoDTO processInfoDTO) throws RemoteException {
    LOGGER.debug("sendProcessInfo()");

    LOGGER.debug("Process Info: " + processInfoDTO);
    LOGGER.debug("");

    // ...
    long heapMaxSize = processInfoDTO.getHeapMaxSize();
    long heapUsedSize = processInfoDTO.getHeapUsedSize();
    long heapCapacity = (heapUsedSize * 100 / heapMaxSize);

    LOGGER.info("Memory Max Size:  " + (heapMaxSize / MB));
    LOGGER.info("Memory Used Size: " + (heapUsedSize / MB));
    LOGGER.info("Memory Capacity:  " + heapCapacity + "%");

    // ...
    long diskTotalSpace = processInfoDTO.getDiskTotalSpace();
    long diskFreeSpace = processInfoDTO.getDiskFreeSpace();
    long diskUsedSpace = diskTotalSpace - diskFreeSpace;
    long diskCapacity = (diskUsedSpace * 100 / diskTotalSpace);

    LOGGER.info("Disk Max Space:  " + (diskTotalSpace / GB));
    LOGGER.info("Disk Used Space: " + (diskUsedSpace / GB));
    LOGGER.info("Disk Capacity:   " + diskCapacity + "%");

    // ...
    String memoryMessage = createMemoryMessage(heapMaxSize, heapUsedSize, heapCapacity);
    String diskMessage = createDiskMessage(diskTotalSpace, diskUsedSpace, diskCapacity);

    sendTelegramMessage(memoryMessage + "\n" + diskMessage);

    // ...
    if (heapCapacity >= MEMORY_WATERMARK_CRITICAL) {
      LOGGER.error(memoryMessage);
      sendTelegramMessage("[ERROR] Critical Memory Watermark reached");
    } else if (heapCapacity >= MEMORY_WATERMARK_HIGH) {
      LOGGER.warn(memoryMessage);
      sendTelegramMessage("[WARN] High Memory Watermark reached");
    }

    if (diskCapacity >= DISK_WATERMARK_CRITICAL) {
      LOGGER.error(diskMessage);
      sendTelegramMessage("[ERROR] Critical Disk Watermark reached");
    } else if (diskCapacity >= DISK_WATERMARK_HIGH) {
      LOGGER.warn(diskMessage);
      sendTelegramMessage("[WARN] High Disk Watermark reached");
    }
  }

  /**
   * 
   */
  private String createMemoryMessage(
      long heapMaxSize,
      long heapUsedSize,
      long heapCapacity) {
    return new StringBuilder()
        .append("[Memory] ").append(heapCapacity).append("%")
        .append(" (").append((heapUsedSize / MB)).append(" MB / ").append((heapMaxSize / MB)).append(" MB)")
        .toString();
  }

  /**
   * 
   */
  private String createDiskMessage(
      long diskTotalSpace,
      long diskUsedSpace,
      long diskCapacity) {
    return new StringBuilder()
        .append("[Disk] ").append(diskCapacity).append("%")
        .append(" (").append((diskUsedSpace / GB)).append(" GB / ").append((diskTotalSpace / GB)).append(" GB)")
        .toString();
  }

  /**
   * 
   */
  private void sendTelegramMessage(@Nonnull String message) {
    MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));
  }
}
