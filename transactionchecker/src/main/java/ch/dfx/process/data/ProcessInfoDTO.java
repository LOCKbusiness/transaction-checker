package ch.dfx.process.data;

import java.io.Serializable;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class ProcessInfoDTO implements Serializable {
  private static final long serialVersionUID = -7793689784079722815L;

  // Memory ...
  private long heapInitSize = -1;
  private long heapUsedSize = -1;
  private long heapMaxSize = -1;
  private long heapCommittedSize = -1;

  // Disk ...
  private long diskTotalSpace = -1;
  private long diskUsableSpace = -1;
  private long diskFreeSpace = -1;

  /**
   * 
   */
  public ProcessInfoDTO() {
  }

  public long getHeapInitSize() {
    return heapInitSize;
  }

  public void setHeapInitSize(long heapInitSize) {
    this.heapInitSize = heapInitSize;
  }

  public long getHeapUsedSize() {
    return heapUsedSize;
  }

  public void setHeapUsedSize(long heapUsedSize) {
    this.heapUsedSize = heapUsedSize;
  }

  public long getHeapMaxSize() {
    return heapMaxSize;
  }

  public void setHeapMaxSize(long heapMaxSize) {
    this.heapMaxSize = heapMaxSize;
  }

  public long getHeapCommittedSize() {
    return heapCommittedSize;
  }

  public void setHeapCommittedSize(long heapCommittedSize) {
    this.heapCommittedSize = heapCommittedSize;
  }

  public long getDiskTotalSpace() {
    return diskTotalSpace;
  }

  public void setDiskTotalSpace(long diskTotalSpace) {
    this.diskTotalSpace = diskTotalSpace;
  }

  public long getDiskUsableSpace() {
    return diskUsableSpace;
  }

  public void setDiskUsableSpace(long diskUsableSpace) {
    this.diskUsableSpace = diskUsableSpace;
  }

  public long getDiskFreeSpace() {
    return diskFreeSpace;
  }

  public void setDiskFreeSpace(long diskFreeSpace) {
    this.diskFreeSpace = diskFreeSpace;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
