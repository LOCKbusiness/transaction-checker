package ch.dfx.process;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * Maybe for future use: HTTP Server instead of RMI - we will see ...
 */
public class ProcessInfoHandler implements HttpRequestHandler {
//  private static final Logger LOGGER = LogManager.getLogger(ProcessInfoHandler.class);
//
//  // ...
//  private static final long KB = 1024;
//  private static final long MB = KB * 1024;
//  private static final long GB = MB * 1024;
//
//  private static final long MEMORY_WATERMARK_WARNING = 80;
//  private static final long MEMORY_WATERMARK_CRITICAL = 90;
//
//  private static final long DISK_WATERMARK_WARNING = 80;
//  private static final long DISK_WATERMARK_CRITICAL = 90;
//
//  // ...
//  private final Gson gson;
//
//  /**
//   * 
//   */
//  public ProcessInfoHandler() {
//    this.gson = new GsonBuilder().setPrettyPrinting().create();
//  }
//
//  /**
//   * 
//   */
  @Override
  public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
//    RequestLine requestLine = request.getRequestLine();
//    String method = requestLine.getMethod().toLowerCase();
//
//    if ("put".equals(method)) {
//      BasicHttpEntityEnclosingRequest entityRequest = (BasicHttpEntityEnclosingRequest) request;
//      HttpEntity requestEntity = entityRequest.getEntity();
//
//      String jsonRequest = EntityUtils.toString(requestEntity);
//      ProcessInfoDTO processInfoDTO = gson.fromJson(jsonRequest, ProcessInfoDTO.class);
//      checkProcessInfo(processInfoDTO);
//
//      response.setStatusCode(HttpStatus.SC_OK);
//    } else {
//      response.setStatusCode(HttpStatus.SC_FORBIDDEN);
//    }
  }
//
//  /**
//   * 
//   */
//  private void checkProcessInfo(@Nonnull ProcessInfoDTO processInfoDTO) throws RemoteException {
//    LOGGER.debug("checkProcessInfo()");
//
//    LOGGER.debug("Process Info: " + processInfoDTO);
//    LOGGER.debug("");
//
//    // ...
//    long heapMaxSize = processInfoDTO.getHeapMaxSize();
//    long heapUsedSize = processInfoDTO.getHeapUsedSize();
//    long heapCapacity = (heapUsedSize * 100 / heapMaxSize);
//
//    LOGGER.debug("Memory Max Size:  " + (heapMaxSize / MB));
//    LOGGER.debug("Memory Used Size: " + (heapUsedSize / MB));
//    LOGGER.debug("Memory Capacity:  " + heapCapacity + "%");
//
//    if (heapCapacity >= MEMORY_WATERMARK_CRITICAL) {
//      // SEND MESSAGE: ...
//      LOGGER.error("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
//    } else if (heapCapacity >= MEMORY_WATERMARK_WARNING) {
//      // SEND MESSAGE: ...
//      LOGGER.warn("[Memory]: consumption is " + heapCapacity + "% (" + (heapUsedSize / MB) + " MB)");
//    }
//
//    // ...
//    long diskTotalSpace = processInfoDTO.getDiskTotalSpace();
//    long diskFreeSpace = processInfoDTO.getDiskFreeSpace();
//    long diskUsedSpace = diskTotalSpace - diskFreeSpace;
//    long diskCapacity = (diskUsedSpace * 100 / diskTotalSpace);
//
//    LOGGER.debug("Disk Max Space:  " + (diskTotalSpace / GB));
//    LOGGER.debug("Disk Used Space: " + (diskUsedSpace / GB));
//    LOGGER.debug("Disk Capacity:   " + diskCapacity + "%");
//
//    if (diskCapacity >= DISK_WATERMARK_CRITICAL) {
//      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
//      LOGGER.error("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
//    } else if (diskCapacity >= DISK_WATERMARK_WARNING) {
//      // TODO: SEND MESSAGE TO EXTERNAL RECEIVER ...
//      LOGGER.warn("[Disk]: consumption is " + diskCapacity + "% (" + (diskUsedSpace / GB) + " GB)");
//    }
//  }
}
