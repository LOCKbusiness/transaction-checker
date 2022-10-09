package ch.dfx.ocean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.ocean.data.ListTransactionsDTO;
import ch.dfx.ocean.data.TransactionDetailVinDTO;
import ch.dfx.ocean.data.TransactionDetailVoutDTO;
import ch.dfx.ocean.data.TransactionsDTO;
import ch.dfx.ocean.data.TransactionsDetailDTO;
import ch.dfx.ocean.data.TransactionsPageDTO;

/**
 * 
 */
public class OceanHandler {
  private static final Logger LOGGER = LogManager.getLogger(OceanHandler.class);

  // ...
  private static final String OCEAN_MAIN_URI = "https://ocean.defichain.com/v0/";
  private static final int OCEAN_FETCH_SIZE = 200;

  // ...
  private BufferedWriter writer = null;

  // ...
  private final Gson gson;

  private final HttpClient httpClient;
  private final HttpGet httpGet;

  private String network = null;
  private String address = null;

  private BigDecimal vinBalance = null;
  private BigDecimal voutBalance = null;

  /**
   * 
   */
  public OceanHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();

    this.httpClient = HttpClientBuilder.create().build();
    this.httpGet = new HttpGet();
  }

  /**
   * 
   */
  public void setup(
      @Nonnull String network,
      @Nonnull String address) {
    LOGGER.trace("setup() ...");

    this.network = network;
    this.address = address;

    this.vinBalance = BigDecimal.ZERO;
    this.voutBalance = BigDecimal.ZERO;
  }

  public BigDecimal getVinBalance() {
    return vinBalance;
  }

  public BigDecimal getVoutBalance() {
    return voutBalance;
  }

  /**
   * 
   */
  public void openWriter(@Nonnull File outputFile) throws DfxException {
    LOGGER.trace("openWriter() ...");

    try {
      if (null == writer) {
        writer = new BufferedWriter(new FileWriter(outputFile));
        writer.append("{\"datalist\": [\n");
      }
    } catch (Exception e) {
      throw new DfxException("openWriter", e);
    }
  }

  /**
   * 
   */
  public void closeWriter() {
    LOGGER.trace("closeWriter() ...");

    try {
      if (null != writer) {
        writer.append("]}");
        writer.flush();
        writer.close();
        writer = null;
      }
    } catch (Exception e) {
      LOGGER.error("closeWriter", e);
    }
  }

  /**
   * 
   */
  public String webcall(@Nullable String next) throws DfxException {
    LOGGER.trace("webcall() ...");

    try {
      StringBuilder oceanURIBuilder = new StringBuilder()
          .append(OCEAN_MAIN_URI).append(network)
          .append("/address/").append(address)
          .append("/transactions?size=").append(OCEAN_FETCH_SIZE);

      if (null != next) {
        oceanURIBuilder.append("&next=").append(next);
      }

      String oceanURI = oceanURIBuilder.toString();
      LOGGER.debug("URI: " + oceanURI);

      httpGet.setURI(new URI(oceanURI));

      HttpResponse httpResponse = httpClient.execute(httpGet);
      HttpEntity httpEntity = httpResponse.getEntity();

      String jsonResponse = EntityUtils.toString(httpEntity);

      TransactionsDTO transactionsDTO = gson.fromJson(jsonResponse, TransactionsDTO.class);

      // ...
      if (null != writer) {
        writer.append(transactionsDTO.toString());

        if (null != transactionsDTO.getPage()) {
          writer.append(",");
        }

        writer.append("\n");
      }

      return analyzeTransactions(transactionsDTO);
    } catch (Exception e) {
      throw new DfxException("error", e);
    }
  }

  /**
   * 
   */
  public ListTransactionsDTO readFromFile(@Nonnull File jsonFile) throws DfxException {
    LOGGER.trace("readFromFile() ...");

    try {
      ListTransactionsDTO transactionsDTOList = gson.fromJson(new FileReader(jsonFile), ListTransactionsDTO.class);

      for (TransactionsDTO transactionsDTO : transactionsDTOList.getDatalist()) {
        analyzeTransactions(transactionsDTO);
      }

      return transactionsDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("readFromFile", e);
    }
  }

  /**
   * 
   */
  private String analyzeTransactions(@Nonnull TransactionsDTO transactionsDTO) throws DfxException {
    LOGGER.trace("analyzeTransactions() ...");

    List<TransactionsDetailDTO> transactionsDetailDTOList = transactionsDTO.getData();

    for (TransactionsDetailDTO transactionsDetailDTO : transactionsDetailDTOList) {
      if (0 == transactionsDetailDTO.getTokenId()) {
        TransactionDetailVinDTO transactionDetailVinDTO = transactionsDetailDTO.getVin();
        TransactionDetailVoutDTO transactionDetailVoutDTO = transactionsDetailDTO.getVout();

        if (null != transactionDetailVinDTO) {
          vinBalance = vinBalance.add(transactionsDetailDTO.getValue());
        } else if (null != transactionDetailVoutDTO) {
          voutBalance = voutBalance.add(transactionsDetailDTO.getValue());
        } else {
          throw new DfxException("no vin and vout found...");
        }
      } else {
        throw new DfxException("unknown token ...");
      }
    }

    String next = null;

    TransactionsPageDTO transactionPageDTO = transactionsDTO.getPage();

    if (null != transactionPageDTO) {
      next = transactionPageDTO.getNext();
    }

    return next;
  }
}
