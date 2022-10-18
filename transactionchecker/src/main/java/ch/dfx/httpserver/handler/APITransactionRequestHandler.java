package ch.dfx.httpserver.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.transaction.OpenTransactionDTOList;

/**
 * 
 */
public class APITransactionRequestHandler extends APIRequestHandler implements HttpRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(APITransactionRequestHandler.class);

  /**
   * 
   */
  public APITransactionRequestHandler() {
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
      } else if ("put".equals(method)) {
        doPut(request, response);
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

    OpenTransactionDTOList openTransactionDTOList = createOpenTransactionDTOList();

    StringEntity entity = new StringEntity(openTransactionDTOList.toString(), ContentType.APPLICATION_JSON);
    response.setEntity(entity);
    response.setStatusCode(HttpStatus.SC_OK);
  }

  /**
   * 
   */
  private OpenTransactionDTOList createOpenTransactionDTOList() throws IOException {
    LOGGER.debug("createOpenTransactionDTOList() ...");

    OpenTransactionDTOList openTransactionDTOList = new OpenTransactionDTOList();

    File[] jsonFileArray = readJSONFiles("transaction");

    for (File jsonFile : jsonFileArray) {
      openTransactionDTOList.addAll(gson.fromJson(Files.readString(jsonFile.toPath()), OpenTransactionDTOList.class));
    }

    return openTransactionDTOList;
  }

  /**
   * 
   */
  private void doPut(
      HttpRequest request,
      HttpResponse response) throws IOException {
    LOGGER.debug("doPut() ...");

    BasicHttpEntityEnclosingRequest entityRequest = (BasicHttpEntityEnclosingRequest) request;
    HttpEntity requestEntity = entityRequest.getEntity();

    String jsonRequest = EntityUtils.toString(requestEntity);
    writeJSONFile(jsonRequest);
  }
}
