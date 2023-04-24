package ch.dfx.ocean;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.ocean.data.HistoryDTO;
import ch.dfx.ocean.data.HistoryDetailDTO;
import ch.dfx.ocean.data.HistoryDetailDTOList;
import ch.dfx.ocean.data.NextPageDTO;

/**
 * 
 */
public class OceanWebAPIHandler {
  private static final Logger LOGGER = LogManager.getLogger(OceanWebAPIHandler.class);

  // ...
  private static final int HTTP_CLIENT_TIMEOUT = 10 * 1000;

  private static final String OCEAN_MAIN_URI = "https://ocean.defichain.com/v0/";
  private static final int OCEAN_FETCH_SIZE = 200;

  // ...
  private final Gson gson;

  private final HttpClient httpClient;
  private final HttpGet httpGet;

  private NetworkEnum network = null;
  private String address = null;

  private HistoryDetailDTOList historyDetailDTOList = null;

  /**
   * 
   */
  public OceanWebAPIHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
            .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
            .setSocketTimeout(HTTP_CLIENT_TIMEOUT).build();

    httpClient =
        HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .build();

    this.httpGet = new HttpGet();

  }

  /**
   * 
   */
  public void setup(
      @Nonnull NetworkEnum network,
      @Nonnull String address) {
    LOGGER.trace("setup()");

    this.network = network;
    this.address = address;

    this.historyDetailDTOList = new HistoryDetailDTOList();
  }

  /**
   * 
   */
  public HistoryDetailDTOList getHistoryDetailDTOList() {
    return historyDetailDTOList;
  }

  /**
   * 
   */
  public String fetchHistory(
      @Nullable String next,
      @Nullable String lastTxId) throws DfxException {
    LOGGER.trace("webcall()");

    try {
      StringBuilder oceanURIBuilder = new StringBuilder()
          .append(OCEAN_MAIN_URI).append(network)
          .append("/address/").append(address)
          .append("/history?size=").append(OCEAN_FETCH_SIZE);

      if (null != next) {
        oceanURIBuilder.append("&next=").append(next);
      }

      String oceanURI = oceanURIBuilder.toString();
      LOGGER.debug("URI: " + oceanURI);

      httpGet.setURI(new URI(oceanURI));

      HttpResponse httpResponse = httpClient.execute(httpGet);
      HttpEntity httpEntity = httpResponse.getEntity();

      String jsonResponse = EntityUtils.toString(httpEntity);

      HistoryDTO historyDTO = gson.fromJson(jsonResponse, HistoryDTO.class);

      return analyzeHistory(historyDTO, lastTxId);
    } catch (Exception e) {
      throw new DfxException("error", e);
    }
  }

  /**
   * 
   */
  private @Nullable String analyzeHistory(
      @Nonnull HistoryDTO historyDTO,
      @Nullable String lastTxId) throws DfxException {
    LOGGER.trace("analyzeHistory()");

    for (HistoryDetailDTO historyDetailDTO : historyDTO.getData()) {
      String txid = historyDetailDTO.getTxid();

      if (txid.equals(lastTxId)) {
        return null;
      }

      historyDetailDTOList.add(historyDetailDTO);
    }

    // ...
    String next = null;

    NextPageDTO nextPageDTO = historyDTO.getPage();

    if (null != nextPageDTO) {
      next = nextPageDTO.getNext();
    }

    return next;
  }
}
