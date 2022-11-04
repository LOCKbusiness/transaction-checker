package ch.dfx.process;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final long MEMORY_WATERMARK_WARNING = 80;
  private static final long MEMORY_WATERMARK_CRITICAL = 90;

  private static final long DISK_WATERMARK_WARNING = 80;
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

    LOGGER.debug("Memory Max Size:  " + (heapMaxSize / MB));
    LOGGER.debug("Memory Used Size: " + (heapUsedSize / MB));
    LOGGER.debug("Memory Capacity:  " + heapCapacity + "%");

    if (heapCapacity >= MEMORY_WATERMARK_CRITICAL) {
      // SEND MESSAGE: ...
      LOGGER.error("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
    } else if (heapCapacity >= MEMORY_WATERMARK_WARNING) {
      // SEND MESSAGE: ...
      LOGGER.warn("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
    }

    // ...
    long diskTotalSpace = processInfoDTO.getDiskTotalSpace();
    long diskFreeSpace = processInfoDTO.getDiskFreeSpace();
    long diskUsedSpace = diskTotalSpace - diskFreeSpace;
    long diskCapacity = (diskUsedSpace * 100 / diskTotalSpace);

    LOGGER.debug("Disk Max Space:  " + (diskTotalSpace / GB));
    LOGGER.debug("Disk Used Space: " + (diskUsedSpace / GB));
    LOGGER.debug("Disk Capacity:   " + diskCapacity + "%");

    if (diskCapacity >= DISK_WATERMARK_CRITICAL) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
      LOGGER.error("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
    } else if (diskCapacity >= DISK_WATERMARK_WARNING) {
      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
      LOGGER.warn("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
    }
  }
}
