package ch.dfx.httpserver.handler;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.httpserver.data.HttpTransactionDTO;
import ch.dfx.httpserver.data.HttpTransactionDTOList;

/**
 * 
 */
public class TransactionHandler implements HttpRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(TransactionHandler.class);

  private final Gson gson;

  /**
   * 
   */
  public TransactionHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  @Override
  public void handle(
      HttpRequest request,
      HttpResponse response,
      HttpContext context) throws HttpException, IOException {
    LOGGER.debug("handle() ...");

    // ...
    Header[] headerArray = request.getHeaders("Authorization");

    boolean isAuthorized = false;

    for (Header header : headerArray) {
      if ("Authorization".equals(header.getName())
          && "Bearer X Access Granted X".equals(header.getValue())) {
        isAuthorized = true;
      }
    }

    if (!isAuthorized) {
      response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
    } else {
      RequestLine requestLine = request.getRequestLine();
      String method = requestLine.getMethod().toLowerCase();

      if ("get".equals(method)) {
        doGet(request, response);
      } else if ("post".equals(method)) {
        doPost(request, response);
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

    HttpTransactionDTOList httpTransactionDTOList = readJSONFiles();

    StringEntity entity = new StringEntity(httpTransactionDTOList.toString(), ContentType.APPLICATION_JSON);
    response.setEntity(entity);
    response.setStatusCode(HttpStatus.SC_OK);
  }

  /**
   * 
   */
  private HttpTransactionDTOList readJSONFiles() throws IOException {
    LOGGER.debug("readJSONFiles() ...");

    HttpTransactionDTOList httpTransactionDTOList = new HttpTransactionDTOList();

    // ...
    File httpServerDataPath = Paths.get("data", "httpserver").toFile();
    LOGGER.debug("HTTP Server Data Path: " + httpServerDataPath.getAbsolutePath());

    // ...
    File[] jsonFileArray = httpServerDataPath.listFiles(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".json");
          }
        });

    // ...
    for (File jsonFile : jsonFileArray) {
      HttpTransactionDTO httpTransactionDTO = gson.fromJson(Files.readString(jsonFile.toPath()), HttpTransactionDTO.class);

      if ("NEW".equals(httpTransactionDTO.getState())) {
        httpTransactionDTO.setFileName(jsonFile.getAbsolutePath());

        httpTransactionDTOList.add(httpTransactionDTO);
      }
    }

    return httpTransactionDTOList;
  }

  /**
   * 
   */
  private void doPost(
      HttpRequest request,
      HttpResponse response) throws IOException {
    LOGGER.debug("doPost() ...");

    BasicHttpEntityEnclosingRequest entityRequest = (BasicHttpEntityEnclosingRequest) request;
    HttpEntity requestEntity = entityRequest.getEntity();

    String jsonRequest = EntityUtils.toString(requestEntity);
    HttpTransactionDTOList httpTransactionDTOList = gson.fromJson(jsonRequest, HttpTransactionDTOList.class);

    writeJSONFiles(httpTransactionDTOList);
  }

  /**
   * 
   */
  private void writeJSONFiles(HttpTransactionDTOList httpTransactionDTOList) throws IOException {
    LOGGER.debug("writeJSONFiles() ...");

    for (HttpTransactionDTO httpTransactionDTO : httpTransactionDTOList) {
      String fileName = httpTransactionDTO.getFileName();

      if (StringUtils.isEmpty(fileName)) {
        throw new IOException("empty filename found: " + httpTransactionDTO.toString());
      }

      httpTransactionDTO.setFileName(null);

      File jsonFile = new File(fileName);
      Files.writeString(jsonFile.toPath(), httpTransactionDTO.toString());
    }
  }
}
