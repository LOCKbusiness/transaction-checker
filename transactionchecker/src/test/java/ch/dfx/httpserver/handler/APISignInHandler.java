package ch.dfx.httpserver.handler;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;

/**
 * 
 */
public class APISignInHandler implements HttpRequestHandler {
  private static final Logger LOGGER = LogManager.getLogger(APISignInHandler.class);

  @Override
  public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
    LOGGER.debug("handle() ...");

    String testToken = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_TEST_TOKEN);

    // ...
    StringEntity entity = new StringEntity(
        "{\"accessToken\":\"" + testToken + "\"}",
        ContentType.APPLICATION_JSON);
    response.setEntity(entity);

    response.setStatusCode(HttpStatus.SC_CREATED);
  }
}
