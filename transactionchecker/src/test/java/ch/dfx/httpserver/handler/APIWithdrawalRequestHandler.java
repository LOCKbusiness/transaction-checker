package ch.dfx.httpserver.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;

/**
 * 
 */
public class APIWithdrawalRequestHandler extends APIRequestHandler implements HttpRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(APIWithdrawalRequestHandler.class);

  /**
   * 
   */
  public APIWithdrawalRequestHandler() {
    super();
  }

  @Override
  public void handle(
      HttpRequest request,
      HttpResponse response,
      HttpContext context) throws HttpException, IOException {
    LOGGER.debug("handle() ...");

    if (!isAuthorized(request)) {
      response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
    } else {
      RequestLine requestLine = request.getRequestLine();
      String method = requestLine.getMethod().toLowerCase();

      if ("get".equals(method)) {
        doGet(request, response);
      }
    }
  }

  /**
   * 
   */
  private void doGet(
      HttpRequest request,
      HttpResponse response) throws IOException {
    LOGGER.debug("doGet() ...");

    PendingWithdrawalDTOList pendingWithdrawalDTOList = createPendingWithdrawalDTOList();

    StringEntity entity = new StringEntity(pendingWithdrawalDTOList.toString(), ContentType.APPLICATION_JSON);
    response.setEntity(entity);
    response.setStatusCode(HttpStatus.SC_OK);
  }

  /**
   * 
   */
  private PendingWithdrawalDTOList createPendingWithdrawalDTOList() throws IOException {
    LOGGER.debug("createPendingWithdrawalDTOList() ...");

    PendingWithdrawalDTOList pendingWithdrawalDTOList = new PendingWithdrawalDTOList();

    File[] jsonFileArray = getJSONFileArray();

    if (null != jsonFileArray) {
      for (File jsonFile : jsonFileArray) {
        pendingWithdrawalDTOList.addAll(gson.fromJson(Files.readString(jsonFile.toPath()), PendingWithdrawalDTOList.class));
      }
    }

    return pendingWithdrawalDTOList;
  }
}
