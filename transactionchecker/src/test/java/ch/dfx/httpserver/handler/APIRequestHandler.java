package ch.dfx.httpserver.handler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;

/**
 * 
 */
public abstract class APIRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(APIRequestHandler.class);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");

  private static int counter = 1;

  // ...
  protected final Gson gson;

  // ...
  private File[] jsonFileArray = null;
  private final List<String> jsonResponseList;

  /**
   * 
   */
  public APIRequestHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();

    this.jsonResponseList = new ArrayList<>();
  }

  /**
   * 
   */
  public void setJSONFileArray(File[] jsonFileArray) {
    this.jsonFileArray = jsonFileArray;

    this.jsonResponseList.clear();
  }

  /**
   * 
   */
  public File[] getJSONFileArray() {
    return jsonFileArray;
  }

  /**
   * 
   */
  public void addJSONResponse(String jsonResponse) {
    this.jsonResponseList.add(jsonResponse);
  }

  /**
   * 
   */
  public List<String> getJSONResponseList() {
    return jsonResponseList;
  }

  /**
   * 
   */
  protected boolean isAuthorized(HttpRequest request) {
    LOGGER.debug("isAuthorized() ...");

    Header[] headerArray = request.getHeaders("Authorization");

    boolean isAuthorized = false;

    String token = "Bearer " + ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_TEST_TOKEN);

    for (Header header : headerArray) {
      if ("Authorization".equals(header.getName())
          && token.equals(header.getValue())) {
        isAuthorized = true;
        break;
      }
    }

    return isAuthorized;
  }
}
