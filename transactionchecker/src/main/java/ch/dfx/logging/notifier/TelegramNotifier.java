package ch.dfx.logging.notifier;

import javax.annotation.Nonnull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;

/**
 * 
 */
public class TelegramNotifier {
  private static final Logger LOGGER = LogManager.getLogger(TelegramNotifier.class);

  // ...
  private final String telegramURL;

  /**
   *
   */
  public TelegramNotifier() {
    String telegramToken = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_TOKEN);
    telegramURL = "https://api.telegram.org/bot" + telegramToken + "/sendMessage";
  }

  /**
   *
   */
  public void sendMessage(@Nonnull String message) throws DfxException {
    LOGGER.trace("sendMessage()");

    try {
      String telegramChatId = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_CHAT_ID);

      HttpClient httpClient = HttpClientBuilder.create().build();

      URIBuilder uriBuilder = new URIBuilder(telegramURL);
      uriBuilder.addParameter("chat_id", telegramChatId);
      uriBuilder.addParameter("text", message);

      HttpGet httpGet = new HttpGet(uriBuilder.build());

      HttpResponse httpResponse = httpClient.execute(httpGet);
      HttpEntity responseEntity = httpResponse.getEntity();

      String jsonResponse = EntityUtils.toString(responseEntity);
      LOGGER.trace("Response: " + jsonResponse);
    } catch (Exception e) {
      throw new DfxException("sendMessage", e);
    }
  }
}