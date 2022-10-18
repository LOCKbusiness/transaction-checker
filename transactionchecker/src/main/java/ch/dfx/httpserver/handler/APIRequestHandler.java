package ch.dfx.httpserver.handler;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.api.ApiAccessHandler;

/**
 * 
 */
public abstract class APIRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(APIRequestHandler.class);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");

  private static int counter = 1;

  // ...
  protected final Gson gson;

  /**
   * 
   */
  public APIRequestHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  protected boolean isAuthorized(HttpRequest request) {
    LOGGER.debug("isAuthorized() ...");

    Header[] headerArray = request.getHeaders("Authorization");

    boolean isAuthorized = false;

    String token = "Bearer " + ApiAccessHandler.TEST_TOKEN;

    for (Header header : headerArray) {
      if ("Authorization".equals(header.getName())
          && token.equals(header.getValue())) {
        isAuthorized = true;
        break;
      }
    }

    return isAuthorized;
  }

  /**
   * 
   */
  protected File[] readJSONFiles(String type) {
    LOGGER.debug("readJSONFiles() ...");

    // ...
    File httpServerDataPath = Paths.get("data", "httpserver", "get").toFile();
    LOGGER.debug("HTTP Server Data Path: " + httpServerDataPath.getAbsolutePath());

    // ...
    return httpServerDataPath.listFiles(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            String checkName = name.toLowerCase();

            return checkName.startsWith(type) && checkName.endsWith(".json");
          }
        });
  }

  /**
   * 
   */
  protected void writeJSONFile(String jsonString) throws IOException {
    LOGGER.debug("writeJSONFile() ...");

    File httpServerDataPath = Paths.get("data", "httpserver", "put").toFile();
    LOGGER.debug("HTTP Server Data Path: " + httpServerDataPath.getAbsolutePath());

    String fileName =
        new StringBuilder()
            .append(DATE_FORMAT.format(new Date()))
            .append("-").append(StringUtils.leftPad(Integer.toString(counter++), 5, "0"))
            .append(".json")
            .toString();

    File jsonFile = new File(httpServerDataPath, fileName);
    Files.writeString(jsonFile.toPath(), jsonString);
  }
}
