package ch.dfx.process;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.process.data.ProcessInfoDTO;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ProcessInfoProvider implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ProcessInfoProvider.class);

  // ...
  // private final HttpClient httpClient;
  private final MemoryMXBean memoryMXBean;

  // ...
  private static Registry registry = null;
  private static ProcessInfoService processInfoService = null;

  private boolean isProcessing = false;

  /**
   * 
   */
  public ProcessInfoProvider() {
    // this.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run()");

    long startTime = System.currentTimeMillis();

    isProcessing = true;

    try {
      if (null == processInfoService) {
        startRMI();
      }

      if (null != processInfoService) {
        ProcessInfoDTO processInfoDTO = getProcessInfoDTO();
        processInfoService.sendProcessInfo(processInfoDTO);
      }
    } catch (Throwable t) {
      processInfoService = null;
      LOGGER.error("run", t);
    } finally {
      isProcessing = false;

      LOGGER.debug("[ProcessInfoProvider] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * Maybe for future use: HTTP Server instead of RMI - we will see ...
   */
//  private void sendProcessInfo() {
//    LOGGER.debug("sendProcessInfo()");
//
//    try {
//      String url = "https://localhost:8083";
//      LOGGER.debug("URL: " + url);
//
//      HttpPut httpPut = new HttpPut(url);
//      httpPut.addHeader("Authorization", "");
//
//      ProcessInfoDTO processInfoDTO = getProcessInfoDTO();
//
//      StringEntity entity = new StringEntity(processInfoDTO.toString(), ContentType.APPLICATION_JSON);
//      httpPut.setEntity(entity);
//
//      HttpResponse httpResponse = httpClient.execute(httpPut);
//
//      int statusCode = httpResponse.getStatusLine().getStatusCode();
//
//      if (HttpStatus.SC_OK != statusCode) {
//        // Error Handling ...
//      }
//    } catch (Exception e) {
//      LOGGER.error("sendProcessInfo", e);
//    }
//  }

  /**
   * 
   */
  private synchronized void startRMI() throws DfxException {
    LOGGER.debug("startRMI()");

    try {
      String rmiHost = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.RMI_HOST);
      int rmiPort = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RMI_PORT, -1);

      registry = LocateRegistry.getRegistry(rmiHost, rmiPort);

      processInfoService =
          (ProcessInfoService) registry.lookup(ProcessInfoService.class.getSimpleName());

      LOGGER.debug("=============================");
      LOGGER.info("RMI Client started: Port " + rmiPort);
      LOGGER.debug("=============================");
    } catch (Exception e) {
      throw new DfxException("startRMI", e);
    }
  }

  /**
   * 
   */
  private synchronized ProcessInfoDTO getProcessInfoDTO() {
    LOGGER.debug("getProcessInfoDTO()");

    ProcessInfoDTO processInfoDTO = new ProcessInfoDTO();

    // Memory ...
    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

    processInfoDTO.setHeapInitSize(heapMemoryUsage.getInit());
    processInfoDTO.setHeapMaxSize(heapMemoryUsage.getMax());
    processInfoDTO.setHeapUsedSize(heapMemoryUsage.getUsed());
    processInfoDTO.setHeapCommittedSize(heapMemoryUsage.getCommitted());

    // Disk ...
    File file = new File(".");
    processInfoDTO.setDiskTotalSpace(file.getTotalSpace());
    processInfoDTO.setDiskUsableSpace(file.getUsableSpace());
    processInfoDTO.setDiskFreeSpace(file.getFreeSpace());

    return processInfoDTO;
  }
}
